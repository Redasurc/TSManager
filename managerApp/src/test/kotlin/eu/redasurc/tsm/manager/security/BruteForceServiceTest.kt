package eu.redasurc.tsm.manager.security

import eu.redasurc.tsm.manager.config.BruteForceSettings
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class BruteForceServiceTest {

    private val settings = BruteForceSettings(2, 4, 5,
            9, 2, 2, 1000)


    val ip1 = "0.0.0.1"
    val ip2 = "0.0.0.2"
    val ip3 = "0.0.0.3"
    val user1 = "test"
    val user2 = "admin"

    @Test
    fun loginFailed() {
        val service = BruteForceService(settings)

        // Test failed attempts for IP1 till lock

        service.loginFailed(ip1, user1)
        assertFalse(service.isBlocked(ip1))
        assertFalse(service.isUserBlocked(user1))
        assertFalse(service.isEnforcingCaptcha(ip1, user1))
        assertFalse(service.isEnforcingCaptcha(ip2, user1))

        service.loginFailed(ip1, user1)
        assertFalse(service.isBlocked(ip1))
        assertFalse(service.isUserBlocked(user1))
        assertFalse(service.isEnforcingCaptcha(ip1, user1))
        assertFalse(service.isEnforcingCaptcha(ip2, user1))

        service.loginFailed(ip1, user1)
        assertFalse(service.isBlocked(ip1))
        assertFalse(service.isUserBlocked(user1))
        assertTrue(service.isEnforcingCaptcha(ip1, user1))
        assertFalse(service.isEnforcingCaptcha(ip2, user1))

        service.loginFailed(ip1, user1)
        assertTrue(service.isBlocked(ip1))
        assertFalse(service.isUserBlocked(user1))
        assertTrue(service.isEnforcingCaptcha(ip1, user1))
        assertFalse(service.isEnforcingCaptcha(ip2, user1))

        // Test second IP

        service.loginFailed(ip2, user1)
        assertFalse(service.isBlocked(ip2))
        assertFalse(service.isUserBlocked(user1))
        assertFalse(service.isEnforcingCaptcha(ip2, user1))
        assertFalse(service.isEnforcingCaptcha(ip3, user1))

        service.loginFailed(ip2, user1)
        assertFalse(service.isBlocked(ip2))
        assertFalse(service.isUserBlocked(user1))
        assertTrue(service.isEnforcingCaptcha(ip2, user1))
        assertTrue(service.isEnforcingCaptcha(ip3, user1))

        service.loginFailed(ip2, user1)
        assertFalse(service.isBlocked(ip2))
        assertFalse(service.isUserBlocked(user1))
        assertTrue(service.isEnforcingCaptcha(ip2, user1))
        assertTrue(service.isEnforcingCaptcha(ip3, user1))

        service.loginFailed(ip2, user1)
        assertTrue(service.isBlocked(ip2))
        assertFalse(service.isUserBlocked(user1))
        assertTrue(service.isEnforcingCaptcha(ip2, user1))
        assertTrue(service.isEnforcingCaptcha(ip3, user1))

        // One final login from ip 3
        service.loginFailed(ip3, user1)
        assertFalse(service.isBlocked(ip3))
        assertTrue(service.isUserBlocked(user1))
        assertTrue(service.isEnforcingCaptcha(ip2, user1))
        assertTrue(service.isEnforcingCaptcha(ip3, user1))
        assertFalse(service.isEnforcingCaptcha(ip3, user2))
    }


    @Test
    fun loginSucceeded() {
        val service = BruteForceService(settings)

        // Test removal of failed attempts on successful attempt

        // All attempts for ip1 & user 1 should be deleted after succeed
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        assertFalse(service.isBlocked(ip1))

        service.loginSucceeded(ip1, user1)
        assertFalse(service.isBlocked(ip1))

        // Due to the fact that all requests should have been deleted,
        // we can make 3 new requests without getting locked
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        assertFalse(service.isBlocked(ip1))

        // Login with another user (shouldn't remove attempts to user1)
        service.loginSucceeded(ip1, user2)
        assertFalse(service.isBlocked(ip1))

        // Now after a 4th attempt for user1, ip should be locked
        service.loginFailed(ip1, user1)
        assertTrue(service.isBlocked(ip1))
    }

    @Test
    fun clearExpiredAttempts() {
        // Service with 100ms ttl for brute force tokens
        val service = BruteForceService(settings.copy(maxAge = 100))

        // Block ip1
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        service.loginFailed(ip1, user1)
        assertTrue(service.isBlocked(ip1))
        // Sleep for 120ms
        Thread.sleep(120)

        // Previous attempts should have timed out
        assertFalse(service.isBlocked(ip1))
    }
}