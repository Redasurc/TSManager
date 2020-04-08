package eu.redasurc.tsclient

interface TSModule {
    fun getModuleName(): String
    fun startModule(client: TeamspeakClient)
    fun stopModule(events: EventManager)

    enum class ModuleStatus {
        INIT,
        STARTED,
        STOPPED
    }
}