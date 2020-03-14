package eu.redasurc.ts3botV2.entity

import org.hibernate.annotations.Where
import org.hibernate.envers.Audited
import org.springframework.data.jpa.domain.AbstractAuditable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*

@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
open class User (
        open var login: String,
        open var email: String,
        open var pw: String,

        @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
        open var clan: UserClanAssignment? = null,

        @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
        open var identitys: MutableCollection<TS3Identity> = mutableListOf(),

        @Enumerated(EnumType.STRING)
        open var permission: ServerPermissions = ServerPermissions.MEMBER,
        open var accountNonExpired: Boolean = true,
        open var accountNonLocked: Boolean = true,
        open var credentialsNonExpired: Boolean = true,
        open var enabled: Boolean = false
) : AbstractAuditable<User, Long> () {

        // Secondary Constructor for Spring Security
        constructor(user: User) : this(user.login, user.email, user.pw, user.clan, user.identitys, user.permission,
                user.accountNonExpired, user.accountNonLocked, user.credentialsNonExpired, user.enabled)

        @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
        constructor() : this(DUMMY_USER)
}


@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class TS3Identity(
        val uuid: String,

        @ManyToOne
        @JoinColumn
        var user: User,
        var lastConnected: Date? = null,
        var comment: String? = null
) : AbstractAuditable<User, Long> () {
        @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
        constructor() : this("", DUMMY_USER)
}





@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class Game (
        var name: String,
        var iconId: String
) : AbstractAuditable<User, Long> () {
        @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
        constructor(): this("","")
}


enum class ServerPermissions(val level: Int) {
        SERVERADMIN(100),
        ADMIN(75),
        MOD(50),
        MEMBER(10)
}
