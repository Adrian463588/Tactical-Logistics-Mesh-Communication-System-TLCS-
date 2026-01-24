package com.example.tclszero.data.map

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineTileProvider - Manages offline map tile loading
 * 
 * Supports:
 * - .mbtiles files (SQLite-based tile archives)
 * - .zip archives containing tile directories (z/x/y.png format)
 * - Pre-cached tiles in app storage
 * 
 * Uses Scoped Storage (SAF) for Android 10+ compatibility
 */
@Singleton
class OfflineTileProvider @Inject constructor(
    private val context: Context
) {
    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private val _tileSourceState = MutableStateFlow<TileSourceState>(TileSourceState.NoTiles)
    val tileSourceState: StateFlow<TileSourceState> = _tileSourceState.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private var mbTilesDatabase: SQLiteDatabase? = null
    private var currentTileSource: ITileSource? = null
    private var tileDirectory: File? = null

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the tiles directory in app-private storage
     */
    fun getTilesDirectory(): File {
        val dir = File(context.filesDir, TILES_DIRECTORY)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Import tiles from a content URI (from SAF file picker)
     */
    suspend fun importTilesFromUri(uri: Uri): Result<TileImportResult> = withContext(Dispatchers.IO) {
        try {
            _tileSourceState.value = TileSourceState.Importing
            _importProgress.value = 0f

            val fileName = getFileNameFromUri(uri) ?: "unknown"
            Timber.d("Importing tiles from: $fileName")

            val result = when {
                fileName.endsWith(".mbtiles", ignoreCase = true) -> {
                    importMbTiles(uri)
                }
                fileName.endsWith(".zip", ignoreCase = true) -> {
                    importZipTiles(uri)
                }
                else -> {
                    Result.failure(IllegalArgumentException("Unsupported file type: $fileName"))
                }
            }

            _importProgress.value = 1f
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to import tiles")
            _tileSourceState.value = TileSourceState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Load an existing .mbtiles file from app storage
     */
    suspend fun loadMbTilesFile(file: File): Result<ITileSource> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File not found: ${file.path}"))
            }

            mbTilesDatabase?.close()
            mbTilesDatabase = SQLiteDatabase.openDatabase(
                file.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val tileSource = MbTilesTileSource(file.nameWithoutExtension, mbTilesDatabase!!)
            currentTileSource = tileSource
            _tileSourceState.value = TileSourceState.Ready(tileSource)
            
            Timber.d("Loaded MBTiles: ${file.name}")
            Result.success(tileSource)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load MBTiles")
            _tileSourceState.value = TileSourceState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Load tiles from a directory (z/x/y.png format)
     */
    fun loadTileDirectory(directory: File): Result<ITileSource> {
        return try {
            if (!directory.exists() || !directory.isDirectory) {
                return Result.failure(IllegalArgumentException("Invalid directory: ${directory.path}"))
            }

            tileDirectory = directory
            val tileSource = DirectoryTileSource(directory)
            currentTileSource = tileSource
            _tileSourceState.value = TileSourceState.Ready(tileSource)
            
            Timber.d("Loaded tile directory: ${directory.name}")
            Result.success(tileSource)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load tile directory")
            _tileSourceState.value = TileSourceState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Get the current tile source (or null if not loaded)
     */
    fun getCurrentTileSource(): ITileSource? = currentTileSource

    /**
     * Check if tiles are available
     */
    fun hasTiles(): Boolean = currentTileSource != null

    /**
     * List available tile files in app storage
     */
    fun listAvailableTileFiles(): List<File> {
        val tilesDir = getTilesDirectory()
        return tilesDir.listFiles()?.filter { 
            it.extension.equals("mbtiles", ignoreCase = true) ||
            (it.isDirectory && File(it, "metadata.json").exists())
        } ?: emptyList()
    }

    /**
     * Delete a tile file
     */
    suspend fun deleteTileFile(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (file == tileDirectory || file.name == mbTilesDatabase?.path) {
                closeCurrent()
            }
            
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete tile file")
            false
        }
    }

    /**
     * Close current tile source and release resources
     */
    fun closeCurrent() {
        mbTilesDatabase?.close()
        mbTilesDatabase = null
        currentTileSource = null
        tileDirectory = null
        _tileSourceState.value = TileSourceState.NoTiles
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - IMPORT METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    private suspend fun importMbTiles(uri: Uri): Result<TileImportResult> {
        val tilesDir = getTilesDirectory()
        val fileName = getFileNameFromUri(uri) ?: "imported_${System.currentTimeMillis()}.mbtiles"
        val destFile = File(tilesDir, fileName)

        // Copy file from content URI to app storage
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                    // Update progress (approximate, since we don't know total size from URI)
                    _importProgress.value = minOf(0.9f, _importProgress.value + 0.01f)
                }

                Timber.d("Copied $totalBytes bytes to ${destFile.path}")
            }
        } ?: return Result.failure(IllegalStateException("Could not open input stream"))

        // Load the imported file
        val loadResult = loadMbTilesFile(destFile)
        return loadResult.map { 
            TileImportResult(
                file = destFile,
                tileSource = it,
                tileCount = estimateTileCount(destFile)
            )
        }
    }

    private suspend fun importZipTiles(uri: Uri): Result<TileImportResult> {
        val tilesDir = getTilesDirectory()
        val tempZip = File(context.cacheDir, "temp_tiles.zip")
        
        // Copy ZIP to cache
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempZip).use { output ->
                input.copyTo(output)
            }
        } ?: return Result.failure(IllegalStateException("Could not open input stream"))

        // Extract ZIP
        val extractDir = File(tilesDir, "tiles_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        try {
            ZipFile(tempZip).use { zip ->
                val entries = zip.entries().toList()
                var processed = 0
                
                entries.forEach { entry ->
                    val destFile = File(extractDir, entry.name)
                    
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    
                    processed++
                    _importProgress.value = processed.toFloat() / entries.size * 0.9f
                }
            }
        } finally {
            tempZip.delete()
        }

        // Load the extracted directory
        val loadResult = loadTileDirectory(extractDir)
        return loadResult.map {
            TileImportResult(
                file = extractDir,
                tileSource = it,
                tileCount = countTilesInDirectory(extractDir)
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }

    private fun estimateTileCount(mbTilesFile: File): Int {
        return try {
            SQLiteDatabase.openDatabase(mbTilesFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT COUNT(*) FROM tiles", null).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getInt(0) else 0
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not count tiles")
            0
        }
    }

    private fun countTilesInDirectory(directory: File): Int {
        return directory.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") }
            .count()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    sealed class TileSourceState {
        object NoTiles : TileSourceState()
        object Importing : TileSourceState()
        data class Ready(val tileSource: ITileSource) : TileSourceState()
        data class Error(val message: String) : TileSourceState()
    }

    data class TileImportResult(
        val file: File,
        val tileSource: ITileSource,
        val tileCount: Int
    )

    companion object {
        private const val TILES_DIRECTORY = "offline_tiles"
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CUSTOM TILE SOURCES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tile source that reads from MBTiles SQLite database
 * Uses OnlineTileSourceBase but overrides to read from local database
 */
class MbTilesTileSource(
    name: String,
    private val database: SQLiteDatabase
) : OnlineTileSourceBase(
    name,
    0,
    18,
    256,
    ".png",
    arrayOf("")  // Empty URL array since we don't use network
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        // This won't be used since we override getDrawable, but needed for API
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val tmsY = (1 shl zoom) - 1 - y
        return "mbtiles://$zoom/$x/$tmsY"
    }

    fun getTileData(zoom: Int, x: Int, y: Int): ByteArray? {
        // MBTiles uses TMS scheme (y is flipped)
        val tmsY = (1 shl zoom) - 1 - y
        
        return try {
            database.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
                arrayOf(zoom.toString(), x.toString(), tmsY.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getBlob(0)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to get tile: $zoom/$x/$y")
            null
        }
    }
}

/**
 * Tile source that reads from a directory structure (z/x/y.png)
 */
class DirectoryTileSource(
    private val directory: File
) : XYTileSource(
    directory.name,
    0,
    18,
    256,
    ".png",
    arrayOf("file://${directory.absolutePath}/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        
        // Check for various extensions
        for (ext in listOf("png", "jpg", "jpeg", "webp")) {
            val tileFile = File(directory, "$zoom/$x/$y.$ext")
            if (tileFile.exists()) {
                return "file://${tileFile.absolutePath}"
            }
        }
        
        // Return default path even if not found
        return "file://${directory.absolutePath}/$zoom/$x/$y.png"
    }
}
