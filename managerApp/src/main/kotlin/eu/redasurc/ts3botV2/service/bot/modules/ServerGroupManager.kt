package eu.redasurc.ts3botV2.service.bot.modules

import eu.redasurc.tsclient.EventManager
import eu.redasurc.tsclient.TSClient
import eu.redasurc.tsclient.TSModule
import eu.redasurc.tsclient.TeamspeakClient
import org.springframework.stereotype.Service

@Service
class ServerGroupManager : TSModule {
    private lateinit var client: TeamspeakClient
    private val sgPlugins = mutableListOf<ServerGroupPlugin>()


    fun updateServerGroups(client: TSClient) {
        val serverGroups = sgPlugins.map { it.calcServerGroups(client) }.flatten()

    }


    override fun getModuleName(): String { return "ServerGroupManager" }

    override fun startModule(client: TeamspeakClient) {
        this.client = client
        client.events.clientConnected.register(this) { user, _ -> updateServerGroups(user)}
        client.events.clientModified.register(this) { user, _ -> updateServerGroups(user)}
        client.events.fullRefresh.register(this) {
            client.ts.clients.forEach { entry -> updateServerGroups(entry.value) }
        }
    }

    override fun stopModule(events: EventManager) {
        events.unregister(this)
    }


}

interface ServerGroupPlugin {
    fun calcServerGroups(client: TSClient) : List<Int>
}