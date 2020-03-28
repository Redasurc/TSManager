package eu.redasurc.tsclient

import com.github.manevolent.ts3j.command.Command
import com.github.manevolent.ts3j.command.SingleCommand
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter
import com.github.manevolent.ts3j.protocol.ProtocolRole
import com.github.manevolent.ts3j.protocol.client.ClientConnectionState
import com.github.manevolent.ts3j.protocol.socket.client.LocalTeamspeakClientSocket

class CustomLocalTeamspeakClientSocket : LocalTeamspeakClientSocket() {
    var channelCommander = false
        set(value) {
            when (state) {
                ClientConnectionState.RETRIEVING_DATA, ClientConnectionState.CONNECTED -> {
                    val command: Command = SingleCommand("clientupdate", ProtocolRole.CLIENT)
                    command.add(CommandSingleParameter("client_is_channel_commander", if(value) {"1"} else {"0"}))
                    try {
                        executeCommand(command).complete()
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
                else -> {}
            }

            field = value
        }
}