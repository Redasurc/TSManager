package eu.redasurc.tsm.manager.config

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

        @DefaultValue("config/identity.bin")
        val identityFile: String,

        val ignoredServergroups: List<Int>,

        val ports: _root_ide_package_.eu.redasurc.tsm.manager.config.MonitorProperties.Ts3PortsConfig,
        val login: _root_ide_package_.eu.redasurc.tsm.manager.config.MonitorProperties.Ts3LoginConfig) {

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
        @DefaultValue("ts@bot.local")
        val from: String = "ts@bot.local",

        @DefaultValue("http://localhost:8080")
        val appUrl: String = "http://localhost:8080",

        @DefaultValue("TS Manager Registration")
        val registrationSubject: String = "TS Manager Registration",

        @DefaultValue("Hello {username},\n\nTo confirm your e-mail address, please click the link below:\n{token}\n\nYour Allround-Gaming Community")
        val registrationMessage: String = "Hello {username}\n Activation Link: {token}",

        @DefaultValue("TS Manager Password Reset")
        val pwResetSubject: String = "TS Manager Password Reset",

        @DefaultValue("Hello {username},\n\nTo reset your password, please click the link below:\n{token}\n\nYour Allround-Gaming Community")
        val pwResetMessage: String = "Hello {username}\n PW Reset Link: {token}",

        @DefaultValue("1209600") // 14 days - in seconds
        val tokenMaxAge: Long = 1209600)



@ConstructorBinding
@ConfigurationProperties("google.recaptcha.key")
data class CaptchaSettings(
        @DefaultValue("true")
        val enabled: Boolean,

        @DefaultValue("")
        val site: String,

        @DefaultValue("")
        val secret: String)

@ConstructorBinding
@ConfigurationProperties("bruteforce")
data class BruteForceSettings(
        /** Require CAPTCHA for IP after ... failed attempts */
        @DefaultValue("2")
        val captchaAttemptPerAddress: Int,

        /** Lock IP after ... failed attempts */
        @DefaultValue("20")
        val maxAttemptPerAddress: Int,

        /** Require CAPTCHA for USER after ... failed attempts */
        @DefaultValue("20")
        val captchaAttemptPerUser: Int,

        /** Lock USER after ... failed attempts */
        @DefaultValue("50")
        val maxAttemptPerUser: Int,

        /** Counts registrations and forgot password calls per IP (Everything that sends an email) */
        @DefaultValue("10")
        val maxRegistrationPerAddress: Int,

        /** Lock IP after ... failed token redemption attempts */
        @DefaultValue("50")
        val maxTokenAttempts: Int,

        /** Max age of cached login attempt (in ms) */
        @DefaultValue("7200000") // 2 hours
        val maxAge: Int)