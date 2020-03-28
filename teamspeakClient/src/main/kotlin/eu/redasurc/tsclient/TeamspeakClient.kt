package eu.redasurc.tsclient

import com.github.manevolent.ts3j.command.CommandException
import com.github.manevolent.ts3j.identity.LocalIdentity
import com.github.theholywaffle.teamspeak3.TS3Config
import com.github.theholywaffle.teamspeak3.TS3Query
import com.github.theholywaffle.teamspeak3.api.ChannelProperty
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress

class TeamspeakClient (private val connectionSettings: ConnectionSettings) : Closeable {
    val events: EventManager = EventManager()

    var started = false

    private val log = LoggerFactory.getLogger(this::class.java)

    // TS Clients
    private val sqConfig = TS3Config()
            .setHost(connectionSettings.serverAdress)
            .setQueryPort(connectionSettings.ports.sq)
    val sqClient = TS3Query(sqConfig)
    val udpClient = CustomLocalTeamspeakClientSocket()

    val ts = VirtualTS(udpClient, sqClient, events)



    fun start() {
        if(started) return
        started = true
        // Start client
        connectSqClient()
        connectUdpClient()
        setupClients()
        ts.updater.init()
        ts.updater.run()

    }
    fun stop() {
        if(!started) return
        started = false
        close()
    }


    fun connectSqClient() {
        if (sqClient.isConnected) {
            return
        }
        log.info("Connecting to serverquery at ${connectionSettings.serverAdress}")
        sqClient.connect()

        // Log in, select the right server and change the nickname
        sqClient.api.login(connectionSettings.login.squser, connectionSettings.login.pass)
        sqClient.api.selectVirtualServerByPort(connectionSettings.ports.udp)
        sqClient.api.setNickname(connectionSettings.nicknameSQ)
        log.info("Connection serverquery complete")
    }
    fun connectUdpClient() {
        log.info("Connecting UDP client")

        // Read Identity file or create a new one if not readable
        val identityFile = File(connectionSettings.identityFile);
        val identity = try {
            LocalIdentity.read(identityFile)
        } catch (e: IOException) {
            val tmp = LocalIdentity.generateNew(8)
            tmp.save(identityFile)
            tmp
        }

        // Set up client
        udpClient.setIdentity(identity)
        udpClient.addListener(ts.updater)
        udpClient.nickname = "TS Bot v2.0"
        udpClient.setClientVersion("Linux","5.0.0-beta.9 [Build: 1571949734]",  "b6ksNapJZndbf5qa1dcvqRgCdcgay0KQrnw8IYkPAXY/OvccuoJ/LUfg/a01nXbxbh45kp7h5gTk9l0L9NVPDQ==")

        udpClient.connect(
                InetSocketAddress(connectionSettings.serverAdress, connectionSettings.ports.udp),
                "",
                10000L
        )
        log.info("Connecting UDP client complete, clientId: ${udpClient.clientId}")
    }

    fun setupClients() {
        val udpClientInfo = sqClient.api.getClientInfo(udpClient.clientId)
        if(!udpClientInfo.serverGroups.contains(connectionSettings.botGroup)) {
            log.info("UDP Client missing servergroup, setting.")
            sqClient.api.addClientToServerGroup(connectionSettings.botGroup, udpClientInfo.databaseId)
            log.info("Setting servergroup successful.")
        }

        try {
            if (connectionSettings.channel > 0) {
                udpClient.joinChannel(connectionSettings.channel, "")
            }
        } catch (e: CommandException) {
            log.error("Switching channel failed", e)
        }
        udpClient.channelCommander = true
        udpClient.subscribeAll()
    }


    override fun close() {
        sqClient.exit()
        udpClient.disconnect("quitting")
    }
}