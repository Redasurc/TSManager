package eu.redasurc.tsclient

data class TSChannel(val id: Int, val name: String, val clients: Int, val topic: String, val parent: Int)
data class TSClient(val id: Int, val name: String, val groups: List<Int>, val channel: Int)


enum class DisconnectReason {
    USER,
    KICK,
    BAN
}
enum class MoveReason {
    USER,
    MOVED,
    KICK
}
