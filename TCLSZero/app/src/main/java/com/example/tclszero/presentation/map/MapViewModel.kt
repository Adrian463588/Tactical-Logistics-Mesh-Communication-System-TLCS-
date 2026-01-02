package com.example.tclszero.presentation.map



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tclszero.data.local.CommandPostDao
import com.example.tclszero.data.local.SoldierNodeDao
import com.example.tclszero.domain.model.CommandPost
import com.example.tclszero.domain.model.SoldierNode

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val soldierNodeDao: SoldierNodeDao,
    private val commandPostDao: CommandPostDao
) : ViewModel() {

    val allNodes: Flow<List<SoldierNode>> = soldierNodeDao.getAllNodes()
    val allPosts: Flow<List<CommandPost>> = commandPostDao.getAllPosts()

    fun updateNodePosition(nodeId: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            soldierNodeDao.updatePosition(nodeId, lat, lon)
        }
    }

    fun addOrUpdateNode(node: SoldierNode) {
        viewModelScope.launch {
            soldierNodeDao.insertOrUpdate(node)
        }
    }

    fun addOrUpdatePost(post: CommandPost) {
        viewModelScope.launch {
            commandPostDao.insertOrUpdate(post)
        }
    }
}
