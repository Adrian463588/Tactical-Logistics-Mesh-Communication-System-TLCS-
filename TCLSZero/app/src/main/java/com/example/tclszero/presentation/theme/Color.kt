package com.example.tclszero.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * TLCS Zero Color System
 * 
 * Design Requirements:
 * - High-Contrast Light Mode for daytime tactical operations
 * - Primary: Black (#000000) for maximum contrast
 * - Secondary: White (#FFFFFF)
 * - Module-specific pastel palettes for quick visual distinction
 */

// ═══════════════════════════════════════════════════════════════════════════════
// CORE THEME COLORS (High-Contrast Light Mode)
// ═══════════════════════════════════════════════════════════════════════════════

object CoreColors {
    // Primary - Black for maximum contrast
    val Primary = Color(0xFF000000)
    val OnPrimary = Color(0xFFFFFFFF)
    
    // Secondary - White
    val Secondary = Color(0xFFFFFFFF)
    val OnSecondary = Color(0xFF000000)
    
    // Background & Surface - Light for readability
    val Background = Color(0xFFFAFAFA)
    val OnBackground = Color(0xFF1A1A1A)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF1A1A1A)
    val SurfaceVariant = Color(0xFFF5F5F5)
    val OnSurfaceVariant = Color(0xFF616161)
    
    // Outline & Borders
    val Outline = Color(0xFFE0E0E0)
    val OutlineVariant = Color(0xFFEEEEEE)
    
    // Status Colors
    val Error = Color(0xFFB71C1C)
    val OnError = Color(0xFFFFFFFF)
    val Success = Color(0xFF1B5E20)
    val Warning = Color(0xFFF57F17)
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOGISTICS MODULE COLORS (Pastel Blue - #E3F2FD)
// For: Map, Inventory, Supply Chain, Blue Force Tracking
// ═══════════════════════════════════════════════════════════════════════════════

object LogisticsColors {
    // Primary - Deep Blue
    val Primary = Color(0xFF1565C0)
    val PrimaryLight = Color(0xFF42A5F5)
    val PrimaryDark = Color(0xFF0D47A1)
    
    // Surface - Pastel Blue (#E3F2FD)
    val Surface = Color(0xFFE3F2FD)
    val SurfaceVariant = Color(0xFFBBDEFB)
    val OnSurface = Color(0xFF0D47A1)
    
    // Supply status colors
    val SupplyHigh = Color(0xFF2E7D32)
    val SupplyMedium = Color(0xFFF9A825)
    val SupplyLow = Color(0xFFE65100)
    val SupplyCritical = Color(0xFFC62828)
    
    // Blue Force Tracking
    val FriendlyUnit = Color(0xFF1976D2)
    val CommandPost = Color(0xFF7B1FA2)
    val MedicalUnit = Color(0xFFC2185B)
    val SupplyDepot = Color(0xFF388E3C)
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMMUNICATION MODULE COLORS (Pastel Green - #E8F5E9)
// For: PTT, Voice Comms, Mesh Networking, Status
// ═══════════════════════════════════════════════════════════════════════════════

object CommsColors {
    // Primary - Deep Green
    val Primary = Color(0xFF2E7D32)
    val PrimaryLight = Color(0xFF66BB6A)
    val PrimaryDark = Color(0xFF1B5E20)
    
    // Surface - Pastel Green (#E8F5E9)
    val Surface = Color(0xFFE8F5E9)
    val SurfaceVariant = Color(0xFFC8E6C9)
    val OnSurface = Color(0xFF1B5E20)
    
    // PTT State colors
    val Transmitting = Color(0xFFC62828)       // Red - active transmission
    val Receiving = Color(0xFF2E7D32)          // Green - receiving
    val Idle = Color(0xFF757575)               // Gray - idle
    val Connecting = Color(0xFFF9A825)         // Amber - connecting
    
    // Network State colors
    val MeshConnected = Color(0xFF2E7D32)
    val MeshSearching = Color(0xFFF9A825)
    val MeshDisconnected = Color(0xFF9E9E9E)
    val MeshError = Color(0xFFC62828)
}

// ═══════════════════════════════════════════════════════════════════════════════
// ALERT COLORS (Pastel Red - #FFEBEE)
// For: Errors, Warnings, Critical Alerts
// ═══════════════════════════════════════════════════════════════════════════════

object AlertColors {
    // Surface - Pastel Red (#FFEBEE)
    val Surface = Color(0xFFFFEBEE)
    val SurfaceVariant = Color(0xFFFFCDD2)
    val OnSurface = Color(0xFFB71C1C)
    
    // Alert levels
    val Critical = Color(0xFFB71C1C)
    val Warning = Color(0xFFE65100)
    val Info = Color(0xFF0277BD)
    val Success = Color(0xFF1B5E20)
}

// ═══════════════════════════════════════════════════════════════════════════════
// TACTICAL OVERLAY COLORS (For Map HUD elements)
// ═══════════════════════════════════════════════════════════════════════════════

object TacticalOverlayColors {
    // Grid & Reticle
    val GridLines = Color(0x40000000)
    val ReticleBlue = Color(0xFF1565C0)
    val ReticleRed = Color(0xFFC62828)
    
    // Map Markers
    val MarkerFriendly = Color(0xFF1976D2)
    val MarkerHostile = Color(0xFFC62828)
    val MarkerNeutral = Color(0xFFFBC02D)
    val MarkerUnknown = Color(0xFF757575)
    
    // Zones & Areas (semi-transparent)
    val ZoneSafe = Color(0x401B5E20)
    val ZoneDanger = Color(0x40C62828)
    val ZoneObjective = Color(0x401565C0)
}
