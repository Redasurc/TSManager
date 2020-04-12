package eu.redasurc.tsm.manager.config

import eu.redasurc.tsm.tsclient.ConnectionSettings
import eu.redasurc.tsm.tsclient.TSModule
import eu.redasurc.tsm.tsclient.TeamspeakClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class TeamspeakBotConfig {

    @Bean
    fun tsClient(@Autowired monProp : MonitorProperties, @Autowired modules: List<TSModule>): TeamspeakClient {
        val conSettings = ConnectionSettings(monProp.server, monProp.channel, monProp.botGroup, monProp.login.pass)
        val init = TeamspeakClient(conSettings)
        init.registerModules(modules)
        if(monProp.autoconnect) init.start()
        return init
    }
}