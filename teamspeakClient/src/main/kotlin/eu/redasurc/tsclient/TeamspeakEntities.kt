package eu.redasurc.tsclient

import eu.redasurc.tsclient.util.TeamspeakValueExtractor as TSVal
import eu.redasurc.tsclient.util.NullableTeamspeakValueExtractor as TSValNull
typealias ClientId = Int
typealias ChannelId = Int

/**
 * TS Channel representation
 */
data class TSChannel(val map: Map<String, Any?>) {
    //ALL
    val id : Int                                       by TSVal(map, "cid")
    val parent : Int                                   by TSVal(map, "pid", "cpid")
    val name : String                                  by TSVal(map, "channelName")
    val order : Int                                    by TSVal(map, "channelOrder")

    // All except list
    val channelTopic : String?                         by TSValNull(map)
    val channelFlagPermanent : Boolean                 by TSVal(map)
    val channelFlagSemiPermanent : Boolean             by TSVal(map)
    val channelFlagDefault : Boolean                   by TSVal(map)
    val channelFlagPassword : Boolean                  by TSVal(map)
    val channelCodecIsUnencrypted : Boolean            by TSVal(map)

    // INITIAL & DETAILS
    val channelCodec : Int                             by TSVal(map)
    val channelCodecQuality : Int                      by TSVal(map)
    val channelMaxclients : Int                        by TSVal(map)
    val channelMaxfamilyclients : Int                  by TSVal(map)
    val channelCodecLatencyFactor : Int                by TSVal(map)
    val channelDeleteDelay : Int                       by TSVal(map)
    val channelUniqueIdentifier : String               by TSVal(map)
    val channelFlagMaxclientsUnlimited : Boolean       by TSVal(map)
    val channelFlagMaxfamilyclientsUnlimited : Boolean by TSVal(map)
    val channelFlagMaxfamilyclientsInherited : Boolean by TSVal(map)
    val channelNeededTalkPower : Int                   by TSVal(map)
    val channelForcedSilence : Boolean                 by TSVal(map)
    val channelNamePhonetic : String?                  by TSValNull(map)
    val channelIconId : Int                            by TSVal(map)
    val channelBannerGfxUrl : String?                  by TSValNull(map)
    val channelBannerMode : Int                        by TSVal(map)

    // ONLY DETAILS
    val channelDescription : String?                   by TSValNull(map)
    val channelFilepath : String?                      by TSValNull(map)
    val secondsEmpty : Long?                           by TSValNull(map)
}

/**
 * TS Client representation
 */
data class TSClient(val map: Map<String, Any?>) {
    constructor(id: Int, name: String, groups: List<Int>, channel: Int) :
            this(mapOf("clid" to id, "clientNickname" to name, "clientServergroups" to groups, "cid" to channel))
    // ALL
    val id : ClientId                                   by TSVal(map, "clid")
    val channel : Int                                   by TSVal(map, "cid")
    val clientDatabaseId : Int                          by TSVal(map)
    val clientNickname : String                         by TSVal(map)
    val clientType : Int                                by TSVal(map)
    // JOIN AND INFO
    val clientUniqueIdentifier : String                 by TSVal(map)
    val clientInputMuted : Boolean                      by TSVal(map)
    val clientOutputMuted : Boolean                     by TSVal(map)
    val clientOutputonlyMuted : Boolean                 by TSVal(map)
    val clientInputHardware : Boolean                   by TSVal(map)
    val clientOutputHardware : Boolean                  by TSVal(map)
    val clientIsRecording : Boolean                     by TSVal(map)
    val clientChannelGroupId : Int                      by TSVal(map)
    val clientServergroups : List<Int>                  by TSVal(map)
    val clientAway : Boolean                            by TSVal(map)
    val clientTalkPower : Int                           by TSVal(map)
    val clientTalkRequest : Int                         by TSVal(map)
    val clientDescription : String?                     by TSVal(map)
    val clientIsTalker : Boolean                        by TSVal(map)
    val clientIsPrioritySpeaker : Boolean               by TSVal(map)
    val clientNicknamePhonetic : String?                by TSVal(map)
    val clientNeededServerqueryViewPower : Int          by TSVal(map)
    val clientIconId : Int                              by TSVal(map)
    val clientIsChannelCommander : Boolean              by TSVal(map)
    val clientCountry : String?                         by TSVal(map)
    val clientChannelGroupInheritedChannelId : Int      by TSVal(map)
    val clientBadges : String?                          by TSVal(map)
    val clientMyteamspeakId : String?                   by TSVal(map)
    val clientIntegrations : String?                    by TSVal(map)
    val clientMyteamspeakAvatar : String?               by TSVal(map)
    val clientSignedBadges : String?                    by TSVal(map)
    val clientMetaData : String?                        by TSVal(map)
}

enum class DisconnectReason(ordinal: Int) {
    USER(8),
    CONNECTION_LOST(3),
    KICK(5),
    BAN(6);
    companion object {
        fun getReason(ordinal: Int): DisconnectReason {
            return when (ordinal) {
                8 -> USER
                3 -> CONNECTION_LOST
                5 -> KICK
                6 -> BAN
                else -> throw NoSuchElementException("DisconnectReason has no value for $ordinal")
            }
        }
    }
}
enum class JoinReason(ordinal: Int) {
    JOINED(0),
    INITIAL(2);
    companion object {
        fun getReason(ordinal: Int): JoinReason {
            return when (ordinal) {
                0 -> JOINED
                2 -> INITIAL
                else -> throw NoSuchElementException("JoinReason has no value for $ordinal")
            }
        }
    }
}

enum class MoveReason(ordinal: Int) {
    SELF(0),
    MOVED(1),
    KICKED(4);
    companion object {
        fun getReason(ordinal: Int): MoveReason {
            return when (ordinal) {
                0 -> SELF
                1 -> MOVED
                4 -> KICKED
                else -> throw NoSuchElementException("MoveReason has no value for $ordinal")
            }
        }
    }
}