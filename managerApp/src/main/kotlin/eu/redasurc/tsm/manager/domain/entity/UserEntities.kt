package eu.redasurc.tsm.manager.domain.entity

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import eu.redasurc.ts3botV2.service.JsonPersistenceConverter
import org.hibernate.envers.Audited
import org.springframework.data.jpa.domain.AbstractAuditable
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*

@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
open class User (

        @Column(unique = true)
        open var login: String,

        @Column(unique = true)
        open var email: String,
        open var pw: String,

        @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
        open var clan: _root_ide_package_.eu.redasurc.tsm.manager.domain.entity.UserClanAssignment? = null,

        @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "user")
        open var identitys: MutableCollection<TS3Identity> = mutableListOf(),

        @ElementCollection(fetch = FetchType.EAGER)
        @Convert(converter = JsonPersistenceConverter::class, attributeName = "value")
        @Lob
        open var attributes: MutableMap<String, JsonNode> = mutableMapOf(),

        @Enumerated(EnumType.STRING)
        open var permission: ServerPermissions = ServerPermissions.MEMBER,
        open var accountNonExpired: Boolean = true,
        open var accountNonLocked: Boolean = true,
        open var credentialsNonExpired: Boolean = true,
        open var enabled: Boolean = false
) : AbstractAuditable<User, Long> () {

        // Secondary Constructor for Spring Security
        constructor(user: User) : this(user.login, user.email, user.pw, user.clan, user.identitys, user.attributes,
                user.permission, user.accountNonExpired, user.accountNonLocked, user.credentialsNonExpired, user.enabled)

        @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
        constructor() : this(DUMMY_USER)


}


@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class SecurityToken(
        @Column(unique = true, nullable = false)
        var token: String,

        @ManyToOne
        @JoinColumn
        var user: User,

        @Enumerated(EnumType.STRING)
        var type: TokenType
) : AbstractAuditable<User, Long> () {
        @Deprecated("Dummy constructor for Spring Data, DO NOT USE")
        constructor() : this("", DUMMY_USER, TokenType.ACTIVATION_TOKEN)
}
enum class TokenType {
        ACTIVATION_TOKEN,
        PW_RESET_TOKEN
}


@Entity
@Audited
@EntityListeners(AuditingEntityListener::class)
class TS3Identity(
        @Column(unique = true)
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
