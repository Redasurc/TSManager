package eu.redasurc.tsm.tsclient

data class ConnectionSettings(
        val serverAdress: String,
        val channel: Int,
        val botGroup: Int,
        val nickname: String,
        val nicknameSQ: String,
        val identityFile: String,
        val ports: Ts3PortsConfig,
        val login: Ts3LoginConfig) {

    constructor(
            serverAdress: String,
            channel: Int,
            botGroup: Int,
            pass: String,
            nickname: String = "TS Bot",
            nicknameSQ: String = "TS Bot Worker",
            identityFile: String = "identity.bin",
            udp: Int = 9987,
            sq: Int = 10011,
            ft: Int = 30033,
            squser: String = "serveradmin") : this(serverAdress, channel, botGroup, nickname,
            nicknameSQ, identityFile, Ts3PortsConfig(udp, sq, ft), Ts3LoginConfig(squser, pass))

    data class Ts3PortsConfig(
            val udp: Int = 9987,
            val sq: Int = 10011,
            val ft: Int = 30033)

    data class Ts3LoginConfig(
            val squser: String,
            val pass: String
    )

}