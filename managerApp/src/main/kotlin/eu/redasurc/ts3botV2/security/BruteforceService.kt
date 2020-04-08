package eu.redasurc.ts3botV2.security

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

data class LoginAttempt(val username: String, val ip: String, val timestamp: Date = Date())

@Service
class BruteForceService {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val MAX_ATTEMPT_PER_ADDRESS = 4
    private val CHAPTA_ATTEMPT_PER_ADDRESS = 2
    private val MAX_ATTEMPT_PER_USER = 50

    // Counts registrations and forgot password calls
    private val MAX_REGISTRATION_PER_ADDRESS = 10

    // Max age of cached login attempt (in ms)
    private val MAX_AGE = 7200000 // 2 hours
    private val attempts: ArrayDeque<LoginAttempt> = ArrayDeque()
    private val registrationAttempts: ArrayDeque<LoginAttempt> = ArrayDeque()

    @Synchronized
    fun loginSucceeded(remoteAddr: String, username: String) {
        attempts
                .filter { it.ip == remoteAddr && it.username == username }
                .forEach{ attempts.remove(it) }
    }

    @Synchronized
    fun loginFailed(remoteAddr: String, username: String)  {
        attempts.push(LoginAttempt(username, remoteAddr))
    }

    @Synchronized
    fun cleanCache() {
        val oldestTime = System.currentTimeMillis() - MAX_AGE

        // Remove elements from queue until timestamp is newer than oldestTime
        while(attempts.isNotEmpty()) {
            if (attempts.peek().timestamp.time > oldestTime) break
            attempts.poll()
        }
    }

    @Synchronized
    fun isBlocked(remoteAddr: String): Boolean {
        cleanCache()
        val count = attempts.filter { it.ip == remoteAddr }.count()
        if(count >= MAX_ATTEMPT_PER_ADDRESS) {
            log.warn("$count recent login attempts from $remoteAddr, IP address is locked")
            return true
        }
        if(count > MAX_ATTEMPT_PER_ADDRESS / 2) {
            log.warn("$count recent login attempts from $remoteAddr, locking IP at $MAX_ATTEMPT_PER_ADDRESS attempts")
        }
        return false
    }

    @Synchronized
    fun isUserBlocked(username: String): Boolean {
        cleanCache()
        val count = attempts.filter { it.username == username }.count()
        if(count >= MAX_ATTEMPT_PER_USER) {
            log.warn("$count recent login attempts for user $username, User is locked")
            return true
        }
        if(count > MAX_ATTEMPT_PER_USER / 2) {
            log.warn("$count recent login attempts for user $username, locking User at $MAX_ATTEMPT_PER_ADDRESS attempts")
        }
        return false
    }

    @Synchronized
    fun isEnforcingChapta(remoteAddr: String): Boolean {
        cleanCache()
        return attempts.filter { it.ip == remoteAddr }.count() > CHAPTA_ATTEMPT_PER_ADDRESS
    }


}