package eu.redasurc.tsclient

import java.util.concurrent.atomic.AtomicInteger

/**
 * This class manages all event callbacks
 */
class EventManager {
    /**
     * Fires every time the Bot connects / reconnects with the teamspeak sever
     */
    val connected = EventLib<(()->Unit)>()

    /**
     * Fires every time the Bot disconnects or looses connection
     */
    val disconnected = EventLib<(()->Unit)>()

    /**
     * New channel added
     */
    val channelAdded = EventLib<((TSChannel, TSClient?)->Unit)>()

    /**
     * Channel removed from server
     */
    val channelRemoved = EventLib<((TSChannel, TSClient?)->Unit)>()

    /**
     * Channel properties modified
     */
    val channelModified = EventLib<((old: TSChannel, new: TSChannel, user: TSClient?)->Unit)>()

    val clientConnected = EventLib<((user: TSClient, channel: TSChannel)->Unit)>()
    val clientDisconnected = EventLib<((user: TSClient, fromChannel: TSChannel, DisconnectReason,
                                        reasonMsg: String?, invoker: TSClient?)->Unit)>()
    val clientMoved = EventLib<((user: TSClient, from: TSChannel, to: TSChannel, MoveReason,
                                 reasonMsg: String?, invoker: TSClient?)->Unit)>()
    val clientModified = EventLib<((old: TSClient, new: TSClient)->Unit)>()

    /**
     * Triggered after a quick server refresh has been triggered
     */
    val quickRefresh = EventLib<(()->Unit)>()

    /**
     * Triggered after a full server refresh has been triggered
     */
    val fullRefresh = EventLib<(()->Unit)>()

    class EventLib<T:Function<*>>{
        private val list = mutableMapOf<Int, T>()
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
                list[id] = callback
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
         * Trigger event
         */
        internal fun triggerEvent(funct:((T)->Unit)) {
            synchronized(list) {
                list.forEach {
                    funct(it.value)
                }
            }
        }
    }
}