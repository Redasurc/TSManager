package eu.redasurc.ts3botV2.security

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

data class LoginAttempt(val username: String, val ip: String, val timestamp: Date = Date())

@Service
class BruteForceService {
    private val log = LoggerFactory.getLogger(this::class.java)

    /** Require CAPTCHA for IP after ... failed attempts */
    private val CAPTCHA_ATTEMPT_PER_ADDRESS = 2

    /** Lock IP after ... failed attempts */

    private val MAX_ATTEMPT_PER_ADDRESS = 20

    /** Require CAPTCHA for USER after ... failed attempts */
    private val CAPTCHA_ATTEMPT_PER_USER = 20

    /** Lock USER after ... failed attempts */
    private val MAX_ATTEMPT_PER_USER = 50

    /** Counts registrations and forgot password calls per IP */
    private val MAX_REGISTRATION_PER_ADDRESS = 10

    /** Max age of cached login attempt (in ms) */
    private val MAX_AGE = 7200000 // 2 hours

    /** QUEUE with LoginAttempts */
    private val attempts: ArrayDeque<LoginAttempt> = ArrayDeque()

    /** QUEUE with RegistrationAttempts (only successful attempts counted) */
    private val registrationAttempts: ArrayDeque<LoginAttempt> = ArrayDeque()

    /**
     * On successful login remove all failed attempts for that IP and USER
     * (Don't remove events for that IP and a different user)
     */
    @Synchronized
    fun loginSucceeded(remoteAddr: String, username: String) {
        attempts
                .filter { it.ip == remoteAddr && it.username == username }
                .forEach{ attempts.remove(it) }
    }

    /**
     * Log failed login attempt
     */
    @Synchronized
    fun loginFailed(remoteAddr: String, username: String)  {
        attempts.push(LoginAttempt(username, remoteAddr))
    }

    /**
     * Remove all elements from queues older than MAX_AGE
     */
    @Synchronized
    fun clearExpiredAttempts() {
        val oldestTime = System.currentTimeMillis() - MAX_AGE

        // Remove elements from queue until timestamp is newer than oldestTime
        while(attempts.isNotEmpty()) {
            if (attempts.peek().timestamp.time > oldestTime) break
            attempts.poll()
        }
        while(registrationAttempts.isNotEmpty()) {
            if (registrationAttempts.peek().timestamp.time > oldestTime) break
            registrationAttempts.poll()
        }
    }

    /**
     * Is current IP address blocked?
     */
    @Synchronized
    fun isBlocked(remoteAddr: String): Boolean {
        clearExpiredAttempts()
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

    /**
     * Is given username blocked?
     */
    @Synchronized
    fun isUserBlocked(username: String): Boolean {
        clearExpiredAttempts()
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

    /**
     * Is the current IP or user enforcing captcha
     */
    @Synchronized
    fun isEnforcingCaptcha(remoteAddr: String, username: String? = null): Boolean {
        clearExpiredAttempts()
        val userEnforcing = username?.run {
            attempts.filter { it.username == username }.count() > CAPTCHA_ATTEMPT_PER_USER } ?: false

        return attempts.filter { it.ip == remoteAddr }.count() > CAPTCHA_ATTEMPT_PER_ADDRESS || userEnforcing
    }


}