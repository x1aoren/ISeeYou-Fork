package cn.xor7.iseeyou

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.leavesmc.leaves.entity.Photographer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class EventListener : Listener {
    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (!toml!!.data.shouldRecordPlayer(player)) {
            return
        }

        val photographer = Bukkit.getPhotographerManager().createPhotographer(player.name, player.location) ?: return
        
        val uuid = UUID.randomUUID().toString()
        photographers[uuid] = photographer

        val currentTime = LocalDateTime.now()
        val recordPath: String = toml!!.data.recordPath
            .replace("\${name}", player.name)
            .replace("\${uuid}", player.uniqueId.toString())
        File(recordPath).mkdirs()
        val recordFile = File(recordPath + "/" + currentTime.format(DATE_FORMATTER) + ".mcpr")
        if (recordFile.exists()) recordFile.delete()
        recordFile.createNewFile()
        photographer.setRecordFile(recordFile)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val photographerKey = photographers.entries.find { it.value.player == player }?.key ?: return
        
        if (toml!!.data.pauseInsteadOfStopRecordingOnPlayerQuit) {
            photographers[photographerKey]?.pauseRecording()
        } else {
            photographers[photographerKey]?.stopRecording(toml!!.data.asyncSave)
            photographers.remove(photographerKey)
        }
    }
} 