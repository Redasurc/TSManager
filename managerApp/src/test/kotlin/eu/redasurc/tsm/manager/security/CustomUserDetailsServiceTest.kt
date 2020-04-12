package eu.redasurc.tsm.manager.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doReturnConsecutively
import com.nhaarman.mockitokotlin2.mock
import eu.redasurc.tsm.manager.domain.BruteForceException
import eu.redasurc.tsm.manager.domain.entity.User
import eu.redasurc.tsm.manager.domain.entity.UserRepository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.springframework.security.core.userdetails.UsernameNotFoundException
import javax.servlet.http.HttpServletRequest

internal class CustomUserDetailsServiceTest {

    @Test
    fun loadUserByUsername() {
        val user = User()
        user.login = "test"

        val repo = mock<UserRepository> {
            on { findOneByLoginIgnoreCase("test") } doReturn user
            on { findOneByLoginIgnoreCase("userBlocked") } doReturn user
        }
        val bruteforce = mock<BruteForceService> {
            on { isBlocked(any()) } doReturnConsecutively listOf(false, false, false, true)
            on { isUserBlocked("test") } doReturn false
            on { isUserBlocked("userBlocked") } doReturn true
        }
        val request = mock<HttpServletRequest> {
            on { remoteAddr } doReturn "192.168.0.1"
        }
        val service = CustomUserDetailsService(repo, bruteforce, request)

        val login = service.loadUserByUsername("test")
        assertNotNull(login)
        assertEquals(user.login, login.username)

        // test unknown user
        try {
            service.loadUserByUsername("unknown")
            fail<String>("Unknown user should throw authentication exception")
        } catch (e: Exception) {
            assertTrue(e is UsernameNotFoundException)
            assertFalse(e is BruteForceException)
        }
        // test user blocked
        try {
            service.loadUserByUsername("userBlocked")
            fail<String>("Blocked user should throw BruteForceException")
        } catch (e: Exception) {
            assertTrue(e is BruteForceException)
        }
        // test ip blocked
        try {
            service.loadUserByUsername("test")
            fail<String>("Blocked IP should throw BruteForceException")
        } catch (e: Exception) {
            assertTrue(e is BruteForceException)
        }



    }
}