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
class Clan(var name: String, leader: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User) : AbstractAuditable<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User, Long>() {

    @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
    constructor() : this("", _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.DUMMY_USER)

    @OneToOne(cascade = [CascadeType.ALL])
    @Where(clause = "position = 'ADMIN'")
    var leader: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment = _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment(leader, this, _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.ADMIN)

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "clan")
    @Where(clause = "position = 'MOD'")
    var mods: MutableCollection<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment> = mutableListOf()

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "clan")
    @Where(clause = "position = 'MEMBER'")
    var member: MutableCollection<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment> = mutableListOf()

    fun addMember(user: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User, position: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition) {
        when(position) {
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.ADMIN -> {
                // ERROR cannot add a new member as leader
            }
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MOD -> mods.add(_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment(user, this, _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MOD))
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MEMBER -> member.add(_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment(user, this, _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MEMBER))
        }
    }
}

@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class UserClanAssignment (
        @OneToOne
            @JoinColumn
            var user: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User,

        @ManyToOne
            @JoinColumn
            var clan: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.Clan,

        @Enumerated(EnumType.STRING)
            var position: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition = _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MEMBER
        ) : AbstractAuditable<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User, Long>() {

    @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
    constructor() : this(_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.DUMMY_USER, _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.DUMMY_CLAN)

    /**
     * Change clan position of the User
     */
    fun changePosition(newPosition: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition) {
        if(this.position == newPosition) {
            return // No change, no need to do anything
        }

        // Remove from old position
        when(this.position) {
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.ADMIN -> {
                // Cant change clan admin without setting a new one first
            }
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MOD -> {
                this.clan.mods.remove(this)
            }
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MEMBER -> {
                this.clan.member.remove(this)
            }
        }

        // Set new position and add to the corresponding list
        this.position = newPosition
        when(newPosition) {
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.ADMIN -> {
                // Downgrade old admin to mod
                this.clan.leader.position = _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MOD
                this.clan.mods.add(this.clan.leader)
                // set new admin
                this.clan.leader = this
            }
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MOD -> {
                this.clan.mods.add(this)
            }
            _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.ClanPosition.MEMBER -> {
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
        var user: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User,

        @ManyToOne
        @JoinColumn
        var clan: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.Clan
) : AbstractAuditable<_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.User, Long>() {
    @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
    constructor() : this(_root_ide_package_.eu.redasurc.tsm.manager.domain.entity.DUMMY_USER, _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.DUMMY_CLAN)
}

enum class ClanPosition(val level: Int) {
    ADMIN(10),
    MOD(5),
    MEMBER(1)
}