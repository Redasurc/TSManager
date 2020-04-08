package eu.redasurc.ts3botV2.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue

@ConstructorBinding
@ConfigurationProperties("ts3manager.monitor")
data class MonitorProperties(
        @DefaultValue("localhost")
        val server: String,

        @DefaultValue("false")
        val autoconnect: Boolean,

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


@ConstructorBinding
@ConfigurationProperties("ts3manager.email")
data class EmailProperties(
        @DefaultValue("ts@bot.com")
        val from: String,

        @DefaultValue("http://localhost:8080")
        val appUrl: String,

        @DefaultValue("TS Manager Registration")
        val registrationSubject: String,

        @DefaultValue("To confirm your e-mail address, please click the link below:")
        val registrationMessage: String,

        @DefaultValue("1209600") // 14 days - in seconds
        val tokenMaxAge: Long)



@ConstructorBinding
@ConfigurationProperties("google.recaptcha.key")
data class CaptchaSettings(
        @DefaultValue("true")
        val enabled: Boolean,

        @DefaultValue("")
        val site: String,

        @DefaultValue("")
        val secret: String)