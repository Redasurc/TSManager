package eu.redasurc.tsm.manager.service.bot.modules

import com.github.theholywaffle.teamspeak3.TS3Api
import com.github.theholywaffle.teamspeak3.api.ChannelProperty
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException
import eu.redasurc.tsm.tsclient.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


@Service
class AutoCreateTeamChannels : TSModule {
    private val log = LoggerFactory.getLogger(this::class.java)
    private var status = TSModule.ModuleStatus.INIT
    private lateinit var api : TS3Api
    private lateinit var virtualTS : VirtualTS


    override fun startModule(client: TeamspeakClient) {
        // Register all event handlers
        registerEvents(client.events)

        this.api = client.sqClient.api
        this.virtualTS = client.ts
        status = TSModule.ModuleStatus.STARTED
        // Refresh all channels
        fullRefresh()
    }

    private fun registerEvents(events: EventManager) {
        with(events) {
            // Channel events
            channelAdded.register { channel: TSChannel, _: TSClient? -> checkTopic(channel) }
            channelModified.register { old: TSChannel, new: TSChannel, _: TSClient? ->
                run {
                    if (old.channelTopic != new.channelTopic) {
                        checkTopic(new)
                    }
                }
            }
            channelRemoved.register { channel: TSChannel, _: TSClient? -> monitoredChannels.remove(channel.id) }

            // Client Events
            clientConnected.register { _: TSClient, channel: TSChannel -> refreshSubchannels(channel) }
            clientDisconnected.register { _: TSClient, channel: TSChannel, _: DisconnectReason,
                                          _: String?, _: TSClient? ->
                refreshSubchannels(channel)
            }
            clientMoved.register { _: TSClient, old: TSChannel, new: TSChannel,
                                   _: MoveReason, _: String?, _: TSClient? ->
                refreshSubchannels(old, new)
            }

            // Full refresh after every virtualTS quick refresh
            quickRefresh.register(::fullRefresh)
        }
    }

    override fun stopModule(events: EventManager) {
        events.unregister(this)
        status = TSModule.ModuleStatus.STOPPED
    }


    private val monitoredChannels = mutableMapOf<Int, Metadata>()
    private val regex = "<<managed\\|*([^|]+)*\\|*([0-9]+)*>>".toRegex()
    private val autocreatedTopic = "<<autocreated>>"

    private val channelProps = mapOf(ChannelProperty.CHANNEL_FLAG_SEMI_PERMANENT to "1",
            ChannelProperty.CHANNEL_TOPIC to autocreatedTopic)







    private fun checkTopic(channel: TSChannel) {
        checkTopic(channel, monitoredChannels)
        refreshSubchannels(channel)
    }

    private fun checkTopic(channel: TSChannel, map: MutableMap<Int, Metadata>) {
        // Break if module not running
        if(status != TSModule.ModuleStatus.STARTED) return

        // Break if channel has no topic
        val topic = channel.channelTopic ?: return
        val matchResult = regex.find(topic)
        matchResult?.run {
            val prefix = if(matchResult.groupValues[1].isEmpty()) {
                "Team"
            } else {matchResult.groupValues[1]}
            val maxChannels = if(matchResult.groupValues[2].isEmpty()) {
                2
            } else {
                matchResult.groupValues[2].toInt()
            }
            map[channel.id] = Metadata(prefix, maxChannels, channel.id)
        }
    }

    private fun refreshSubchannels(vararg channels: TSChannel) {
        // For all given channels trigger a refresh for it and its parent
        for (channel in channels) {
            refreshSubchannels(channel.id)
            refreshSubchannels(channel.parent)
        }
    }

    /**
     * Creates and or deletes sub-channels in the given channel id to
     * match the pattern given in the metadata object for that id
     */
    private fun refreshSubchannels(channelId: ChannelId) {
        // Break if module not running
        if(status != TSModule.ModuleStatus.STARTED) return

        // Break if channel is unmonitored
        val metadata = monitoredChannels[channelId] ?: return

        // Break if channel can't be found (possible desync event)
        val channel = virtualTS.channels[channelId] ?: return

        // All child channels of the current channel
        val childChannels = virtualTS.getChildChannels(channel)

        // Map with number of sub-channel to channel name
        val nameList = (metadata.maxChannels downTo 1).map { it to "${metadata.prefix} $it" }.toMap()

        // Delete all sub-channels which are auto created but not in the list
        // of possible channel names
        childChannels.stream()
                .filter { autocreatedTopic == it.channelTopic }
                .filter { !nameList.values.contains(it.name) }
                .filter { virtualTS.getClients(it).isEmpty() }
                .forEach { try {api.deleteChannel(it.id)} catch(e: TS3CommandFailedException) {}}

        // Map with number of sub-channel to TSChannel object if exists
        val createdChannels =  nameList.map {
            it.key to childChannels.firstOrNull { c -> c.name == it.value }
        }.toMap()

        // Map with number of sub-channel to "isEmpty"
        val channelUsers = createdChannels
                .map { it.key to virtualTS.getClients(it.value).isEmpty()}
                .toMap()

        var blocked = false

        // Iterate over the list of possible and created channels
        for ((number, tsChannel) in createdChannels) {

            // If current channel doesn't exist but one of the succeeding channels is filled (blocked)
            // or if there is no empty channel in the preceding channels create the current channel
            tsChannel ?:run {
                if(blocked || channelUsers.filter { it.key < number }.filter { it.value }.isEmpty()) {
                    nameList[number]?.run {
                        try {
                            api.createChannel(this, channelProps.plus(ChannelProperty.CPID to "$channelId"))
                        } catch(e: Exception) {
                            log.warn("Channel creation failed", e)
                        }
                    }
                }
            }

            // Continue if we channel doesn't exists (handled by the run above)
            // or if we don't have a channel user count (for whatever reason)
            tsChannel ?: continue
            val channelEmpty = channelUsers[number] ?: continue

            // if channel isn't empty set blocked to true
            if(!channelEmpty) {
                blocked = true

            // If current channel is empty, is not blocked and preceding channels have
            // an empty channel as well, delete this one
            } else if(!blocked && channelUsers.filter { it.key < number }.filter { it.value }.isNotEmpty()) {
                try {
                    api.deleteChannel(tsChannel.id)
                } catch(e: TS3CommandFailedException) {}
            }

        }

    }

    private fun fullRefresh() {
        val newMap = mutableMapOf<ChannelId, Metadata>()
        for ((_, tsChannel) in virtualTS.channels) {
            checkTopic(tsChannel, newMap)
        }
        monitoredChannels.clear()
        monitoredChannels.putAll(newMap)
        for (key in monitoredChannels.keys) {
            refreshSubchannels(key)
        }
    }




    // ---------------------- //

    data class Metadata(val prefix: String, val maxChannels: Int, val channelId: Int)

    override fun getModuleName(): String {
        return "Autocreate Channels"
    }
}
