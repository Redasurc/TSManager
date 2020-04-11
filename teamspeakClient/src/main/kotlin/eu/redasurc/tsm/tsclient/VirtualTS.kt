package eu.redasurc.tsm.tsclient

import com.github.manevolent.ts3j.event.*
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket
import com.github.theholywaffle.teamspeak3.TS3Query
import eu.redasurc.tsm.tsclient.util.moveKey
import eu.redasurc.tsm.tsclient.util.onlyKeys
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.streams.toList

class VirtualTS(udpClient: LocalTeamspeakClientSocket, sqClient: TS3Query, events: EventManager) {
    var channels = mapOf<ChannelId, TSChannel>()
        internal set
    var clients = mapOf<ClientId, TSClient>()
        internal set

    internal val updater = Updater(udpClient, sqClient, events, this)

    /**
     * Get parent channel for a given channel
     *
     * @param client Client to find the channel to
     * @return clients channel
     * @throws TSChannelNotFoundException if channel the client is currently in cannot be found
     */
    @Throws(TSChannelNotFoundException::class)
    fun getChannelForClient(client: TSClient) : TSChannel {
        return channels[client.channel]?:throw TSChannelNotFoundException()
    }

    /**
     * Get parent channel for a given channel
     *
     * @param channel Child channel to find the parent to
     * @return parent channel or null if the given channel is a root channel
     * @throws TSChannelNotFoundException if channel has a parent set but parent can't be found
     */
    @Throws(TSChannelNotFoundException::class)
    fun getParentChannel(channel: TSChannel) : TSChannel? {
        if(channel.parent == 0) { // TODO CHECK IF THIS IS CORRECT
            return null
        }
        return channels[channel.parent]?:throw TSChannelNotFoundException("Parent for channel $channel.id not found")
    }

    fun getClients(channel: TSChannel?): List<TSClient> {
        return channel?.run { getClients(channel.id) }?:run { listOf<TSClient>()}
    }
    fun getClients(channel: ChannelId): List<TSClient> {
        return clients.values.stream().filter { it.channel == channel }.toList()
    }

    fun getChildChannels(channel: TSChannel): List<TSChannel> {
        return channels.values.stream().filter { it.parent == channel.id }.toList()
    }


    class Updater(private val udpClient: LocalTeamspeakClientSocket, private val sqClient: TS3Query,
                  private val events: EventManager,
                  val virtualTS: VirtualTS) : TS3Listener, Runnable {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val udpQueue = ArrayDeque<BaseEvent>()
        val running = AtomicBoolean(false)

        fun init() {
            // DO WE NEED INIT?
        }

        private fun fullUpdate() {

        }
        private fun quickUpdate() {
            // Request current teamspeak status
            val listClients = udpClient.listClients()
            val listChannels = udpClient.listChannels()

            // PROCESS CHANNELS
            val registeredChannels = mutableListOf(*virtualTS.channels.keys.toTypedArray())

            for (channel in listChannels) {
                val oldChannel = virtualTS.channels[channel.id]
                val newChannel = TSChannel(channel.map)

                // Detect DESYNC Channel create event missing
                if(oldChannel == null) {
                    log.warn("DESYNC: Channel created event missed for ${newChannel.id} (${newChannel.name}), trying to fix")
                    val channelDetails = udpClient.getChannelInfo(newChannel.id)
                    val chan = TSChannel(channelDetails.map.plus("cid" to "${newChannel.id}"))
                    addOrUpdateChannel(chan)
                    events.channelAdded.triggerEvent { it(chan, null) }
                    continue
                }

                // Detect Desync RENAME event missing

                // Detect Desync Move event missing

                registeredChannels.remove(channel.id)
            }
            // Process all id's whose removed event we've missed
            for (i in registeredChannels) {
                // Continue if for what reason the client is no longer in our storage
                val channel = virtualTS.channels[i] ?: continue

                // DESYNC CLIENT NO LONGER CONNECTED
                log.warn("DESYNC: Channel deleted event missed for " +
                        "${channel.id} (${channel.name}), trying to fix")
                removeChannel(channel)
                events.channelRemoved.triggerEvent { it(channel, null) }
            }

            // PROCESS CLIENTS

            // The id's of all currently connected clients. Each client id in the new clientList gets removed.
            // All remaining id's are clients who are no longer connected
            val currentlyConnected = mutableListOf(*virtualTS.clients.keys.toTypedArray())

            for (client in listClients) {
                val oldClient = virtualTS.clients[client.id]
                val newClient = TSClient(client.map)

                // Detect DESYNC CLIENT JOIN EVENT MISSING
                if(oldClient == null) {
                    log.warn("DESYNC: Client join event missed for ${client.id} (${client.nickname}), trying to fix")
                    val store = TSClient(udpClient.getClientInfo(client.id).map)
                    addOrUpdateClient(store)
                    events.clientConnected.triggerEvent { it(store, virtualTS.getChannelForClient(store)) }
                    continue
                }

                // Detect DESYNC CLIENT MOVED CHANNEL UNNOTICED
                if (oldClient.channel != newClient.channel) {
                    log.warn("DESYNC: Client ${client.nickname} (${client.id}) no longer in channel " +
                            "${oldClient.channel} but in channel ${newClient.channel}, trying to fix")
                    addOrUpdateClient(TSClient(oldClient.map.plus("cid" to newClient.channel)))
                    events.clientMoved.triggerEvent { it(newClient, virtualTS.getChannelForClient(oldClient),
                            virtualTS.getChannelForClient(newClient), MoveReason.UNKNWON, null, null) }
                }

                // Detect DESYNC CLIENT RENAME UNNOTICED
                if (oldClient.clientNickname != newClient.clientNickname) {
                    log.warn("DESYNC: Client ${client.nickname} (${client.id}) renamed to " +
                            "${newClient.clientNickname}, trying to fix")
                    addOrUpdateClient(TSClient(oldClient.map.plus("client_nickname" to newClient.clientNickname)))
                    events.clientModified.triggerEvent { it(oldClient, newClient) }
                }

                // Remove processed id from currently connected id list
                currentlyConnected.remove(client.id)
            }

            // Process all id's whose disconnect event we've missed
            for (i in currentlyConnected) {
                // Continue if for what reason the client is no longer in our storage
                val client = virtualTS.clients[i] ?: continue

                // DESYNC CLIENT NO LONGER CONNECTED
                log.warn("DESYNC: Client disconnected event missed for " +
                        "${client.id} (${client.clientNickname}), trying to fix")
                removeClient(client)
                events.clientDisconnected.triggerEvent { it(client, virtualTS.getChannelForClient(client),
                        DisconnectReason.UNKNOWN, null, null) }
            }
            events.quickRefresh.triggerEvent { it() }
        }

        /**
         * Main event loop
         *
         * This function handles all queued events and the triggering of the modules
         */
        override fun run() {
            if(running.get()) return  // Return if already running

            running.set(true)
            thread(isDaemon= true) {
                var counter = 0
                var connected = false

                while(running.get()) {

                    // Handle all queued events
                    while(!udpQueue.isEmpty()) {
                        handleEvent(udpQueue.pop())
                    }

                    // TODO: This should be solved a little bit more graceful than with a 1 second timeout ;)
                    if(counter == 10 && !connected) {
                        connected = true
                        events.connected.triggerEvent { it() }
                        log.info("Virtual TS init finished, triggering connected event")
                    }

                    // trigger every 60 seconds
                    // (Dont use 0 because we dont wanna trigger an update straight after connecting)
                    if(counter % 600 == 599) {
                        quickUpdate()
                    }

                    // every 5 minutes trigger full update and reset timer to 0
                    if(counter > 3000) {
                        counter = 0;
                        fullUpdate()
                    }
                    counter++
                    Thread.sleep(100L)
                }
            }
        }

        /**
         * Handle the given event. Updates the virtualTS and triggers events
         */
        private fun handleEvent(event: BaseEvent) {
            log.debug("Event {} triggered: {}", event::class.java.name, event)
            when(event) {
                // CLIENT EVENTS
                is ClientJoinEvent -> {
                    val client = TSClient(event.map.moveKey("ctid", "cid"))
                    addOrUpdateClient(client)

                    // Trigger event
                    events.clientConnected.triggerEvent { it(client, virtualTS.getChannelForClient(client)) }
                }
                is ClientLeaveEvent -> {
                    val client = handleEventHelperGetOldClient(event.clientId, event)?:return
                    removeClient(client)

                    events.clientDisconnected.triggerEvent { it(client, virtualTS.getChannelForClient(client),
                            DisconnectReason.getReason(event.reasonId),
                            event.reasonMessage, getInvoker(event.invokerId))}
                }
                is ClientUpdatedEvent -> {
                    val oldClient = handleEventHelperGetOldClient(event.clientId, event)?:return
                    val newClient = TSClient(oldClient.map.plus(event.map))
                    addOrUpdateClient(newClient)

                    // Trigger event
                    events.clientModified.triggerEvent {
                        it(oldClient, newClient)
                    }
                }
                is ClientMovedEvent -> {
                    val oldClient = handleEventHelperGetOldClient(event.clientId, event)?:return
                    val newClient = TSClient(oldClient.map.plus("cid" to event.targetChannelId))
                    addOrUpdateClient(newClient)

                    // TriggerEvent
                    events.clientMoved.triggerEvent {
                        it(newClient, virtualTS.getChannelForClient(oldClient), virtualTS.getChannelForClient(newClient),
                                MoveReason.getReason(event.reasonId), event.reasonMessage, getInvoker(event.invokerId))
                    }

                }
                is ClientChannelGroupChangedEvent -> {
                    val oldClient = handleEventHelperGetOldClient(event.clientId, event)?:return
                    val newClient = TSClient(oldClient.map.plus(mapOf("client_channel_group_id" to event.channelGroupId,
                            "client_channel_group_inherited_channel_id" to event.inheritedChannelId)))
                    addOrUpdateClient(newClient)
                    // TODO: TRIGGER EVENT
                }
                is ServerGroupClientDeletedEvent -> {}
                is ClientPermHintsEvent -> {}
                is PrivilegeKeyUsedEvent -> {}

                // CHANNEL EVENTS
                is ChannelListEvent -> {  // This only triggers on connect
                    val channel = TSChannel(event.map.moveKey("cpid", "pid"))
                    addOrUpdateChannel(channel)

                    // NO EVENT TRIGGERING FOR THIS ONE AS IT ONLY HAPPENS ON CONNECT
                }
                is ChannelCreateEvent -> {
                    val channelDetails = udpClient.getChannelInfo(event.channelId)
                    val channel = TSChannel(channelDetails.map.plus("cid" to "${event.channelId}"))
                    addOrUpdateChannel(channel)

                    events.channelAdded.triggerEvent {
                        it(channel, virtualTS.clients[event.invokerId])
                    }
                }
                is ChannelMovedEvent -> {
                    val oldChannel = handleEventHelperGetOldChannel(event.channelId, event) ?: return
                    val newChannel = TSChannel(oldChannel.map.plus(
                            event.map.onlyKeys("cpid", "order").moveKey("cpid", "pid")))
                    addOrUpdateChannel(newChannel)

                    events.channelModified.triggerEvent { it(oldChannel, newChannel, virtualTS.clients[event.invokerId]) }
                }
                is ChannelEditedEvent -> {
                    val oldChannel = handleEventHelperGetOldChannel(event.channelId, event) ?: return
                    val channelDetails = udpClient.getChannelInfo(event.channelId)
                    val newChannel = TSChannel(channelDetails.map.plus("cid" to "${event.channelId}"))
                    addOrUpdateChannel(newChannel)

                    events.channelModified.triggerEvent { it(oldChannel, newChannel, virtualTS.clients[event.invokerId]) }
                }
                is ChannelDeletedEvent -> {
                    val oldChannel = handleEventHelperGetOldChannel(event.channelId, event) ?: return
                    removeChannel(oldChannel)
                    events.channelRemoved.triggerEvent { it(oldChannel, virtualTS.clients[event.invokerId]) }
                }

                is ChannelDescriptionEditedEvent -> {} // SHOULDN'T be needed because the edited event is also triggered
                is ChannelPasswordChangedEvent -> {}   // SHOULDN'T be needed because the edited event is also triggered

                is ChannelSubscribedEvent -> {}
                is ChannelGroupListEvent -> {}
                is ChannelPermHintsEvent -> {}
                is ChannelUnsubscribedEvent -> {}

                // SERVERGROUP EVENT
                is PermissionListEvent -> {}
                is DisconnectedEvent -> {}
                is ServerGroupListEvent -> {}
                is ServerGroupClientAddedEvent -> {}

                // CHAT
                is TextMessageEvent -> {}
                is ClientChatComposingEvent -> {}
                is ClientChatClosedEvent -> {}
                is ClientPokeEvent -> {}

                // SERVER
                is ServerEditedEvent -> {}
                is ConnectedEvent -> {}
                is ClientNeededPermissionsEvent -> {}
            }
        }

        private fun getInvoker(id: Int?) : TSClient?{
            return id?.run { if(this > 0) { virtualTS.clients[this] }else {null}}
        }

        private fun handleEventHelperGetOldClient(clientId: ClientId, event: BaseEvent) : TSClient?{
            return virtualTS.clients[clientId] ?:run {
                log.warn("DESYNC: {} event for nonexistent client $clientId", event::class.java.name)
                return null
            }
        }
        private fun handleEventHelperGetOldChannel(channelId: ChannelId, event: BaseEvent) : TSChannel?{
            return virtualTS.channels[channelId] ?:run {
                log.warn("DESYNC: {} event for nonexistent channel $channelId", event::class.java.name)
                return null
            }
        }

        /**
         * Adds / Updates the given client in the virtualTS data structure
         */
        private fun addOrUpdateClient(client: TSClient) {
            virtualTS.clients = virtualTS.clients.plus(client.id to client)
        }

        /**
         * Removes a client from the virtualTS data structure
         */
        private fun removeClient(client: TSClient) {
            removeClient(client.id)
        }

        /**
         * Removes a client by id from the virtualTS data structure
         */
        private fun removeClient(cid: Int) {
            virtualTS.clients = virtualTS.clients.minus(cid)
        }

        /**
         * Adds / Updates the given channel in the virtualTS data structure
         */
        private fun addOrUpdateChannel(channel: TSChannel) {
            virtualTS.channels = virtualTS.channels.plus(channel.id to channel)
        }

        /**
         * Removes a channel from the virtualTS data structure
         */
        private fun removeChannel(channel: TSChannel) {
            removeChannel(channel.id)
        }

        /**
         * Removes a channel by id from the virtualTS data structure
         */
        private fun removeChannel(cid: Int) {
            virtualTS.channels = virtualTS.channels.minus(cid)
        }


        /*
                     =============================
                     == TS UDP CLIENT CALLBACKS ==
                     =============================

            All these callbacks simply add the given update event
            to the queue to be handled by the virtualTS Thread
         */

        override fun onClientLeave(e: ClientLeaveEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelMoved(e: ChannelMovedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelList(e: ChannelListEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onPrivilegeKeyUsed(e: PrivilegeKeyUsedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientPoke(e: ClientPokeEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelSubscribed(e: ChannelSubscribedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onServerGroupClientDeleted(e: ServerGroupClientDeletedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onPermissionList(e: PermissionListEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelEdit(e: ChannelEditedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientChanged(e: ClientUpdatedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientMoved(e: ClientMovedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientPermHints(e: ClientPermHintsEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onDisconnected(e: DisconnectedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onTextMessage(e: TextMessageEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelGroupList(e: ChannelGroupListEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelDeleted(e: ChannelDeletedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientChannelGroupChanged(e: ClientChannelGroupChangedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelPermHints(e: ChannelPermHintsEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientJoin(e: ClientJoinEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelDescriptionChanged(e: ChannelDescriptionEditedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onServerGroupList(e: ServerGroupListEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientChatClosed(e: ClientChatClosedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onServerGroupClientAdded(e: ServerGroupClientAddedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientComposing(e: ClientChatComposingEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onServerEdit(e: ServerEditedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelCreate(e: ChannelCreateEvent?) {
            e?.run {
                udpQueue.offer(this)

                // Subscribe to all channels in order to subscribe to the created channel
                // This is done here because it won't cause race conditions
                // due to the fact that all events of the new channel are added
                // to the queue themselves. But by doing this here, we can prevent
                // the possible loss of events due to the 100ms event handling delay
                udpClient.subscribeAll()
            }
        }

        override fun onChannelUnsubscribed(e: ChannelUnsubscribedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onConnected(e: ConnectedEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onClientNeededPermissions(e: ClientNeededPermissionsEvent?) {
            e?.run {udpQueue.offer(this)}
        }

        override fun onChannelPasswordChanged(e: ChannelPasswordChangedEvent?) {
            e?.run {udpQueue.offer(this)}
        }
    }
}