package eu.redasurc.ts3botV2.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue

@ConstructorBinding
@ConfigurationProperties("ts3manager.monitor")
data class MonitorProperties(
        @DefaultValue("localhost")
        val server: String,

        @DefaultValue("1")
        val channel: Int,

        @DefaultValue("2")
        val botGroup: Int,

        @DefaultValue("Teamspeak Bot")
        val nickname: String,

        @DefaultValue("TS Bot")
        val nicknameSQ: String,

        @DefaultValue("identity.bin")
        val identityFile: String,

        val ports: Ts3PortsConfig,
        val login: Ts3LoginConfig) {

    data class Ts3PortsConfig(
            @DefaultValue("9987")
            val udp: Int = 9987,
            @DefaultValue("10011")
            val sq: Int = 10011,
            @DefaultValue("30033")
            val ft: Int = 30033)

    data class Ts3LoginConfig(
            @DefaultValue("serveradmin")
            val squser: String,
            @DefaultValue("<unset>")
            val pass: String
    )
}