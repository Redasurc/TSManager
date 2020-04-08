package eu.redasurc.tsclient

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * This class manages all event callbacks
 */
class EventManager {

    private val eventLibs = mutableMapOf<String, EventLib<*>>()
    init {
        eventLibs["connected"] = EventLib<(()->Unit)>("connected")
        eventLibs["disconnected"] = EventLib<(()->Unit)>("disconnected")
        eventLibs["channelAdded"] = EventLib<((TSChannel, TSClient?)->Unit)>("channelAdded")
        eventLibs["channelRemoved"] = EventLib<((TSChannel, TSClient?)->Unit)>("channelRemoved")
        eventLibs["channelModified"] = EventLib<((old: TSChannel, new: TSChannel, user: TSClient?)->Unit)>("channelModified")
        eventLibs["clientConnected"] = EventLib<((user: TSClient, channel: TSChannel)->Unit)>("clientConnected")
        eventLibs["clientDisconnected"] = EventLib<((user: TSClient, fromChannel: TSChannel, DisconnectReason, reasonMsg: String?, invoker: TSClient?)->Unit)>("clientDisconnected")
        eventLibs["clientMoved"] = EventLib<((user: TSClient, from: TSChannel, to: TSChannel, MoveReason, reasonMsg: String?, invoker: TSClient?)->Unit)>("clientMoved")
        eventLibs["clientModified"] = EventLib<((old: TSClient, new: TSClient)->Unit)>("clientModified")
    }


    /**
     * Fires every time the Bot connects / reconnects with the teamspeak sever
     */
    val connected: EventLib<(()->Unit)> by eventLibs

    /**
     * Fires every time the Bot disconnects or looses connection
     */
    val disconnected : EventLib<(()->Unit)> by eventLibs

    /**
     * New channel added
     */
    val channelAdded : EventLib<((TSChannel, TSClient?)->Unit)> by eventLibs

    /**
     * Channel removed from server
     */
    val channelRemoved : EventLib<((TSChannel, TSClient?)->Unit)> by eventLibs

    /**
     * Channel properties modified
     */
    val channelModified : EventLib<((old: TSChannel, new: TSChannel, user: TSClient?)->Unit)> by eventLibs

    val clientConnected : EventLib<((user: TSClient, channel: TSChannel)->Unit)> by eventLibs
    val clientDisconnected : EventLib<((user: TSClient, fromChannel: TSChannel, DisconnectReason, reasonMsg: String?, invoker: TSClient?)->Unit)> by eventLibs
    val clientMoved : EventLib<((user: TSClient, from: TSChannel, to: TSChannel, MoveReason, reasonMsg: String?, invoker: TSClient?)->Unit)> by eventLibs
    val clientModified : EventLib<((old: TSClient, new: TSClient)->Unit)> by eventLibs

    /**
     * Triggered after a quick server refresh has been triggered
     */
    val quickRefresh = EventLib<(()->Unit)>("quickRefresh")

    /**
     * Triggered after a full server refresh has been triggered
     */
    val fullRefresh = EventLib<(()->Unit)>("fullRefresh")


    /**
     * Removes all callbacks for the given module
     *
     * @param module of the callback to be removed
     *
     * @return true if callback has been removed, false otherwise
     */
    fun unregister(module: TSModule) : Boolean {
        return eventLibs.values.map { it.unregister(module) }.reduce(Boolean::or)
    }

    class EventLib<T:Function<*>> (val name: String){
        private val log = LoggerFactory.getLogger(this::class.java)
        private val list = mutableMapOf<Int, EventStorage<T>>()
        private val counter = AtomicInteger(0)

        /** Add a function as callback and return the array id of that callback
         *  for future change / deletion
         *
         *  @param callback callback to be registered
         *
         *  @return index of the callback just registered
         */
        fun register(callback: T) : Int {
            synchronized(list) {
                val id = counter.incrementAndGet()
                list[id] = EventStorage(callback)
                return id
            }
        }

        /** Add a function as callback and return the array id of that callback
         *  for future change / deletion
         *
         *  @param module Module class for auto start/restart
         *  @param callback callback to be registered
         *
         *  @return index of the callback just registered
         */
        fun register(module: TSModule, callback: T) : Int {
            synchronized(list) {
                val id = counter.incrementAndGet()
                list[id] = EventStorage(callback, module)
                return id
            }
        }

        /**
         * Removes a callback by given index
         *
         * @param index of the callback to be removed
         *
         * @return true if callback has been removed, false otherwise
         */
        fun unregister(index: Int) : Boolean {
            synchronized(list) {
                if (!list.containsKey(index)) {
                    return false
                }
                list.remove(index)
                return true
            }
        }

        /**
         * Removes a callback by given index
         *
         * @param index of the callback to be removed
         *
         * @return true if a callback has been removed, false otherwise
         */
        fun unregister(module: TSModule) : Boolean {
            synchronized(list) {
                return list.entries
                        .filter { it.value.module == module }
                        .map {
                            unregister(it.key)
                        }.reduce(Boolean::or)
            }
        }

        /**
         * Trigger event
         */
        internal fun triggerEvent(funct:((T)->Unit)) {
            synchronized(list) {
                list.forEach {
                    try {
                        funct(it.value.callback)
                    } catch (e: Exception) {
                        log.warn("Event $name for module ${it.value.module} failed (Callback Nr. ${it.key})", e)
                    }
                }
            }
        }
    }
    private data class EventStorage<T:Function<*>> (val callback: T, val module: TSModule? = null)
}