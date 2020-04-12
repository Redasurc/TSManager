package eu.redasurc.tsm.manager.domain.entity

import org.hibernate.annotations.Where
import org.hibernate.envers.Audited
import org.springframework.data.jpa.domain.AbstractAuditable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*


@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class Clan(var name: String, leader: User) : AbstractAuditable<User, Long>() {

    @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
    constructor() : this("", DUMMY_USER)

    @OneToOne(cascade = [CascadeType.ALL])
    @Where(clause = "position = 'ADMIN'")
    var leader: UserClanAssignment = UserClanAssignment(leader, this, ClanPosition.ADMIN)

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "clan")
    @Where(clause = "position = 'MOD'")
    var mods: MutableCollection<UserClanAssignment> = mutableListOf()

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "clan")
    @Where(clause = "position = 'MEMBER'")
    var member: MutableCollection<UserClanAssignment> = mutableListOf()

    fun addMember(user: User, position: ClanPosition) {
        when(position) {
            ClanPosition.ADMIN -> {
                // ERROR cannot add a new member as leader
            }
            ClanPosition.MOD -> mods.add(UserClanAssignment(user, this, ClanPosition.MOD))
            ClanPosition.MEMBER -> member.add(UserClanAssignment(user, this, ClanPosition.MEMBER))
        }
    }
}

@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class UserClanAssignment (
        @OneToOne
            @JoinColumn
            var user: User,

        @ManyToOne
            @JoinColumn
            var clan: Clan,

        @Enumerated(EnumType.STRING)
            var position: ClanPosition = ClanPosition.MEMBER
        ) : AbstractAuditable<User, Long>() {

    @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
    constructor() : this(DUMMY_USER, DUMMY_CLAN)

    /**
     * Change clan position of the User
     */
    fun changePosition(newPosition: ClanPosition) {
        if(this.position == newPosition) {
            return // No change, no need to do anything
        }

        // Remove from old position
        when(this.position) {
            ClanPosition.ADMIN -> {
                // Cant change clan admin without setting a new one first
            }
            ClanPosition.MOD -> {
                this.clan.mods.remove(this)
            }
            ClanPosition.MEMBER -> {
                this.clan.member.remove(this)
            }
        }

        // Set new position and add to the corresponding list
        this.position = newPosition
        when(newPosition) {
            ClanPosition.ADMIN -> {
                // Downgrade old admin to mod
                this.clan.leader.position = ClanPosition.MOD
                this.clan.mods.add(this.clan.leader)
                // set new admin
                this.clan.leader = this
            }
            ClanPosition.MOD -> {
                this.clan.mods.add(this)
            }
            ClanPosition.MEMBER -> {
                this.clan.member.add(this)
            }
        }
    }
}


@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class ClanInvite (
        @OneToOne
        @JoinColumn
        var user: User,

        @ManyToOne
        @JoinColumn
        var clan: Clan
) : AbstractAuditable<User, Long>() {
    @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
    constructor() : this(DUMMY_USER, DUMMY_CLAN)
}

enum class ClanPosition(val level: Int) {
    ADMIN(10),
    MOD(5),
    MEMBER(1)
}