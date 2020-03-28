package eu.redasurc.ts3botV2.config

import eu.redasurc.tsclient.ConnectionSettings
import eu.redasurc.tsclient.TeamspeakClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct


@Configuration
class TeamspeakBotConfig {
    var bot : TeamspeakClient? = null

    @Bean
    fun tsClient(@Autowired monProp : MonitorProperties): TeamspeakClient {
        println("Creating TSCLIENT BEAN")
        val conSettings = ConnectionSettings(monProp.server, monProp.channel, monProp.botGroup, monProp.login.pass)
        val init = TeamspeakClient(conSettings)
        bot = init
        return init
    }

    @Bean
    fun initClient(@Autowired tsClient: TeamspeakClient) : String{
        tsClient.start()
        return "started"
    }
}