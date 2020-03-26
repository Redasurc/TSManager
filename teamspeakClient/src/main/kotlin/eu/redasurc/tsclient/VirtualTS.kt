package eu.redasurc.tsclient

import com.github.manevolent.ts3j.event.*
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket
import com.github.theholywaffle.teamspeak3.TS3Query
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

class VirtualTS(udpClient: LocalTeamspeakClientSocket, sqClient: TS3Query, events: EventManager) {
    var channels = mapOf<Int, TSChannel>()
        internal set
    var clients = mapOf<Int, TSClient>()
        internal set

    internal val updater = Updater(udpClient, sqClient, events)

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


    class Updater (private val udpClient: LocalTeamspeakClientSocket, private val sqClient: TS3Query,
                   private val events: EventManager) : TS3Listener, Runnable {
        private val log = LoggerFactory.getLogger(this::class.java)
        private val udpQueue = ArrayDeque<BaseEvent>()
        val running = AtomicBoolean(false)

        fun init() {
            val channels = udpClient.listChannels()
            val clients = udpClient.listClients()

        }

        override fun run() {
            if(running.get()) return
            running.set(true)
            var counter = 0
            while(running.get()) {
                while(!udpQueue.isEmpty()) {
                    handleEvent(udpQueue.pop())
                }
                if(counter > 600) { // every 60 seconds
                    counter = 0;
                    // update clients and channels and check for possible inconsistency's
                    val channels = udpClient.listChannels()
                    val clients = udpClient.listClients()
                }
                counter++
                Thread.sleep(100L)
            }
        }

        private fun handleEvent(event: BaseEvent) {
            when(event) {
                is ClientLeaveEvent -> {log.info("Event ClientLeaveEvent triggered: {}", event)}
                is ChannelMovedEvent -> {log.info("Event ChannelMovedEvent triggered: {}", event)}
                is ChannelListEvent -> {log.info("Event ChannelListEvent triggered: {}", event)}
                is PrivilegeKeyUsedEvent -> {log.info("Event PrivilegeKeyUsedEvent triggered: {}", event)}
                is ClientPokeEvent -> {log.info("Event ClientPokeEvent triggered: {}", event)}
                is ChannelSubscribedEvent -> {log.info("Event ChannelSubscribedEvent triggered: {}", event)}
                is ServerGroupClientDeletedEvent -> {log.info("Event ServerGroupClientDeletedEvent triggered: {}", event)}
                is PermissionListEvent -> {log.info("Event PermissionListEvent triggered: {}", event)}
                is ChannelEditedEvent -> {log.info("Event ChannelEditedEvent triggered: {}", event)}
                is ClientUpdatedEvent -> {log.info("Event ClientUpdatedEvent triggered: {}", event)}
                is ClientMovedEvent -> {log.info("Event ClientMovedEvent triggered: {}", event)}
                is ClientPermHintsEvent -> {log.info("Event ClientPermHintsEvent triggered: {}", event)}
                is DisconnectedEvent -> {log.info("Event DisconnectedEvent triggered: {}", event)}
                is TextMessageEvent -> {log.info("Event TextMessageEvent triggered: {}", event)}
                is ChannelGroupListEvent -> {log.info("Event ChannelGroupListEvent triggered: {}", event)}
                is ChannelDeletedEvent -> {log.info("Event ChannelDeletedEvent triggered: {}", event)}
                is ClientChannelGroupChangedEvent -> {log.info("Event ClientChannelGroupChangedEvent triggered: {}", event)}
                is ChannelPermHintsEvent -> {log.info("Event ChannelPermHintsEvent triggered: {}", event)}
                is ClientJoinEvent -> {log.info("Event ClientJoinEvent triggered: {}", event)}
                is ChannelDescriptionEditedEvent -> {log.info("Event ChannelDescriptionEditedEvent triggered: {}", event)}
                is ServerGroupListEvent -> {log.info("Event ServerGroupListEvent triggered: {}", event)}
                is ClientChatClosedEvent -> {log.info("Event ClientChatClosedEvent triggered: {}", event)}
                is ServerGroupClientAddedEvent -> {log.info("Event ServerGroupClientAddedEvent triggered: {}", event)}
                is ClientChatComposingEvent -> {log.info("Event ClientChatComposingEvent triggered: {}", event)}
                is ServerEditedEvent -> {log.info("Event ServerEditedEvent triggered: {}", event)}
                is ChannelCreateEvent -> {log.info("Event ChannelCreateEvent triggered: {}", event)}
                is ChannelUnsubscribedEvent -> {log.info("Event ChannelUnsubscribedEvent triggered: {}", event)}
                is ConnectedEvent -> {log.info("Event ConnectedEvent triggered: {}", event)}
                is ClientNeededPermissionsEvent -> {log.info("Event ClientNeededPermissionsEvent triggered: {}", event)}
                is ChannelPasswordChangedEvent -> {log.info("Event ChannelPasswordChangedEvent triggered: {}", event)}
            }
        }

        override fun onClientLeave(e: ClientLeaveEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelMoved(e: ChannelMovedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelList(e: ChannelListEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onPrivilegeKeyUsed(e: PrivilegeKeyUsedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientPoke(e: ClientPokeEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelSubscribed(e: ChannelSubscribedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onServerGroupClientDeleted(e: ServerGroupClientDeletedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onPermissionList(e: PermissionListEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelEdit(e: ChannelEditedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientChanged(e: ClientUpdatedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientMoved(e: ClientMovedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientPermHints(e: ClientPermHintsEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onDisconnected(e: DisconnectedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onTextMessage(e: TextMessageEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelGroupList(e: ChannelGroupListEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelDeleted(e: ChannelDeletedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientChannelGroupChanged(e: ClientChannelGroupChangedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelPermHints(e: ChannelPermHintsEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientJoin(e: ClientJoinEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelDescriptionChanged(e: ChannelDescriptionEditedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onServerGroupList(e: ServerGroupListEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientChatClosed(e: ClientChatClosedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onServerGroupClientAdded(e: ServerGroupClientAddedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientComposing(e: ClientChatComposingEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onServerEdit(e: ServerEditedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelCreate(e: ChannelCreateEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelUnsubscribed(e: ChannelUnsubscribedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onConnected(e: ConnectedEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onClientNeededPermissions(e: ClientNeededPermissionsEvent?) {
            e?.run {udpQueue.push(this)}
        }

        override fun onChannelPasswordChanged(e: ChannelPasswordChangedEvent?) {
            e?.run {udpQueue.push(this)}
        }
    }
}