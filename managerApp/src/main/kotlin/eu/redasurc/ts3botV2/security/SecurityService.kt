package eu.redasurc.ts3botV2.security

import eu.redasurc.ts3botV2.domain.BruteForceException
import eu.redasurc.ts3botV2.domain.entity.ClanPosition
import eu.redasurc.ts3botV2.domain.entity.ServerPermissions.*
import eu.redasurc.ts3botV2.domain.entity.User
import eu.redasurc.ts3botV2.domain.entity.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.stream.Collectors
import javax.servlet.http.HttpServletRequest


/**
 * Spring security user detail service
 */
@Service
class CustomUserDetailsService (private val userRepository: UserRepository,
                                private val bruteForceService: BruteForceService,
                                private val request: HttpServletRequest) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        // Before attempting to login, check if ip or username is locked due to brute-force settings
        val ip = getClientIP(request)
        if(bruteForceService.isBlocked(ip)) {
            throw BruteForceException("IP address is blocked for 2 hours. Too many invalid login attempts.")
        }
        if(bruteForceService.isUserBlocked(username)) {
            throw BruteForceException("There have been to many invalid login attempts for this user. " +
                    "User is blocked for 2 hours.")
        }

        // Search for user and return
        return CustomUserDetails(userRepository.findOneByLoginIgnoreCase(username)
                ?: throw UsernameNotFoundException("Username not found"))
    }
}


/**
 * Wrapper for User to use in Spring Security
 */
open class CustomUserDetails(user: User) : User(user), UserDetails {

    private val log = LoggerFactory.getLogger(CustomUserDetails::class.java)

    override fun getAuthorities(): Collection<GrantedAuthority> {
        val roles = mutableListOf<String>()

        // Handle global permissions
        roles.add(MEMBER.toString())
        when(permission.level) {
            in MOD.level..Int.MAX_VALUE -> roles.add(MOD.toString())
            in ADMIN.level..Int.MAX_VALUE -> roles.add(ADMIN.toString())
            in SERVERADMIN.level..Int.MAX_VALUE -> roles.add(SERVERADMIN.toString())
        }


        // Handle clan permissions if clan exists
        when(clan?.position?.level) {
            in ClanPosition.MEMBER.level..Int.MAX_VALUE -> roles.add("c_${clan!!.clan.name}_${ClanPosition.MEMBER}")
            in ClanPosition.MOD.level..Int.MAX_VALUE -> roles.add("c_${clan!!.clan.name}_${ClanPosition.MOD}")
            in ClanPosition.ADMIN.level..Int.MAX_VALUE -> roles.add("c_${clan!!.clan.name}_${ClanPosition.ADMIN}")
        }


        return roles
                .stream()
                .map { role ->
                    log.debug("Granting Authority to user with role: " + role.toString())
                    SimpleGrantedAuthority(role.toString())
                }
                .collect(Collectors.toList())
    }

    override fun getPassword(): String {
        return super.pw
    }

    override fun getUsername(): String {
        return super.login
    }

    override fun isEnabled(): Boolean {
        return super.enabled
    }

    override fun isCredentialsNonExpired(): Boolean {
        return super.credentialsNonExpired
    }

    override fun isAccountNonExpired(): Boolean {
        return super.accountNonExpired
    }

    override fun isAccountNonLocked(): Boolean {
        return super.accountNonLocked
    }
}