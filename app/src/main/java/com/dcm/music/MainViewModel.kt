package com.dcm.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dcm.music.data.MusicRepository
import com.dcm.music.data.Song
import com.dcm.music.playback.PlayerConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MusicRepository(app)
    private val player = PlayerConnection(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val playerState = player.state

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                player.refreshPosition()
            }
        }
    }

    fun refresh() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _songs.value = repo.loadSongs()
            _loading.value = false
        }
    }

    fun play(list: List<Song>, index: Int) = player.play(list, index)
    fun playShuffled(list: List<Song>) = player.play(list.shuffled(), 0)
    fun togglePlay() = player.togglePlay()
    fun next() = player.next()
    fun previous() = player.previous()
    fun seekTo(ms: Long) = player.seekTo(ms)
    fun toggleShuffle() = player.toggleShuffle()
    fun cycleRepeat() = player.cycleRepeat()

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
