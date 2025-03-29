package cn.xor7.iseeyou.utils

import net.minecraft.client.player.LocalPlayer
import java.util.*

data class ConfigData(
    var pauseRecordingOnHighSpeed: HighSpeedPauseConfig = HighSpeedPauseConfig(),
    var deleteTmpFileOnLoad: Boolean = true,
    var pauseInsteadOfStopRecordingOnPlayerQuit: Boolean = false,
    var filter: FilterConfig = FilterConfig(),
    var recordPath: String = "replay/player/\${name}@\${uuid}",
    var clearOutdatedRecordFile: OutdatedRecordRetentionConfig = OutdatedRecordRetentionConfig(),
    var instantReplay: InstantReplayConfig = InstantReplayConfig(),
    var asyncSave: Boolean = false,
    var bStats: Boolean = true,
    var check_for_updates: Boolean = true,
) {
    fun isConfigValid(): String? {
        if ("name" != filter.checkBy && "uuid" != filter.checkBy) {
            return "invalid checkBy value!"
        }
        if ("blacklist" != filter.recordMode && "whitelist" != filter.recordMode) {
            return "invalid recordMode value!"
        }
        return null
    }

    fun setConfig() {
        if ("blacklist" == filter.recordMode) {
            if (filter.blacklist == null) {
                filter.blacklist = mutableSetOf()
            }
        } else if (filter.whitelist == null) {
            filter.whitelist = mutableSetOf()
        }
    }

    fun shouldRecordPlayer(player: LocalPlayer): Boolean {
        return if ("blacklist" == filter.recordMode) {
            !containsPlayer(player, filter.blacklist!!)
        } else {
            containsPlayer(player, filter.whitelist!!)
        }
    }

    private fun containsPlayer(player: LocalPlayer, list: Set<String>): Boolean {
        return if ("name" == filter.checkBy) {
            list.contains(player.name.string)
        } else {
            list.contains(player.uuid.toString())
        }
    }
}

data class HighSpeedPauseConfig(
    var enabled: Boolean = false,
    var threshold: Double = 20.00,
)

data class OutdatedRecordRetentionConfig(
    var enabled: Boolean = false,
    var interval: Int = 24,
    var days: Int = 7,
)

data class FilterConfig(
    var checkBy: String = "name",
    var recordMode: String = "blacklist",
    var blacklist: Set<String>? = null,
    var whitelist: Set<String>? = null,
)

data class InstantReplayConfig(
    var enabled: Boolean = false,
    var replayMinutes: Int = 5,
    var createMinutes: Int = 1,
    var recordPath: String = "replay/instant/\${name}@\${uuid}",
)
