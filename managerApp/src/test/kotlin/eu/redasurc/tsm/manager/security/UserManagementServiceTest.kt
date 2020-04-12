package eu.redasurc.tsm.manager.security

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import eu.redasurc.tsm.manager.config.EmailProperties
import eu.redasurc.tsm.manager.domain.EmailAlreadyRegisteredException
import eu.redasurc.tsm.manager.domain.RegistrationException
import eu.redasurc.tsm.manager.domain.UsernameAlreadyRegisteredException
import eu.redasurc.tsm.manager.domain.entity.SecurityToken
import eu.redasurc.tsm.manager.domain.entity.TokenRepository
import eu.redasurc.tsm.manager.domain.entity.User
import eu.redasurc.tsm.manager.domain.entity.UserRepository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailSender
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder

internal class UserManagementServiceTest {
    private val log = LoggerFactory.getLogger(this::class.java)

    lateinit var userRepository: UserRepository
    lateinit var tokenRepository: TokenRepository
    lateinit var pwEncoder: PasswordEncoder
    lateinit var mailSender: MailSender
    lateinit var mailProperties: EmailProperties
    lateinit var userManagementService: UserManagementService
    @BeforeEach
    fun setup() {
        userRepository = mock {}
        tokenRepository = mock {}
        pwEncoder = mock {
            on {encode(any())} doReturn "ENC-PW-HASH"
        }
        mailSender = mock {}
        mailProperties = EmailProperties()
        userManagementService = UserManagementService(userRepository, tokenRepository,
                pwEncoder, mailSender, mailProperties)
    }

    @Test
    fun bind() {
        val user = User()
        user.pw = "ENC-PW-HASH"
        whenever(userRepository.findOneByLoginIgnoreCase("user")).thenReturn(user)
        whenever(pwEncoder.matches("ASD", "ENC-PW-HASH")).thenReturn(true)
        assertTrue(userManagementService.bind("user", "ASD"), "Bind with existing user and correct pw failed")
        try {
            userManagementService.bind("unknown", "ASD")
            fail<String>("Bind with non-existing user passed")
        } catch (e: UsernameNotFoundException) {}
        assertFalse(userManagementService.bind("user", "ASDF"), "Bind with wrong pw passed.")
    }

    @Test
    fun registerUser() {
        val user = User()
        val token = SecurityToken()
        whenever(userRepository.findOneByLoginIgnoreCase("user")).thenReturn(user)
        whenever(userRepository.findByEmailIgnoreCase("user@local")).thenReturn(user)
        whenever(userRepository.save(any<User>())).thenReturn(user)
        whenever(tokenRepository.save(any<SecurityToken>())).thenReturn(token)
        whenever(pwEncoder.encode(any())).thenReturn("ENC-PW-HASH")


        // Should pass without exception
        userManagementService.registerUser("test", "test@test.local", "ASD")

        // Existing user
        try {
            userManagementService.registerUser("user", "test@test.local", "ASD")
            fail<String>("duplicate username registration allowed")
        } catch (e: UsernameAlreadyRegisteredException) {log.debug("",e)}

        // Existing email
        try {
            userManagementService.registerUser("test", "user@local", "ASD")
            fail<String>("duplicate username registration allowed")
        } catch (e: EmailAlreadyRegisteredException) { log.debug("", e) }
    }

    @Test
    fun useToken() {
        log.debug("Please implement")
    }

    @Test
    fun sendResetPwEmail() {
        log.debug("Please implement")
    }

    @Test
    fun testSendResetPwEmail() {
        log.debug("Please implement")
    }

    @Test
    fun resetPW() {
        log.debug("Please implement")
    }
}