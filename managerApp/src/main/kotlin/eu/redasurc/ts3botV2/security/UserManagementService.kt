package eu.redasurc.ts3botV2.security

import eu.redasurc.ts3botV2.config.EmailProperties
import eu.redasurc.ts3botV2.domain.EmailAlreadyRegisteredException
import eu.redasurc.ts3botV2.domain.RegistrationException
import eu.redasurc.ts3botV2.domain.TokenException
import eu.redasurc.ts3botV2.domain.UsernameAlreadyRegisteredException
import eu.redasurc.ts3botV2.domain.entity.ActivationToken
import eu.redasurc.ts3botV2.domain.entity.TokenRepository
import eu.redasurc.ts3botV2.domain.entity.User
import eu.redasurc.ts3botV2.domain.entity.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.thread

typealias Username = String
typealias Password = String

@Service
class UserManagementService(@Autowired private val userRepository: UserRepository,
                            @Autowired private val tokenRepository: TokenRepository,
                            @Autowired private val pwEncoder: PasswordEncoder,
                            @Autowired private val mailSender: MailSender,
                            @Autowired private val mailProperties: EmailProperties){
    private val log = LoggerFactory.getLogger(this::class.java)
    /**
     * Check username & password
     */
    fun bind(username: Username, password: Password) : Boolean {
        val user = userRepository.findOneByLoginIgnoreCase(username) ?: throw RegistrationException("User not found")
        return pwEncoder.matches(password, user.pw)
    }

    @Throws(RegistrationException::class)
    fun registerUser(_username: Username, _email: String, password: Password, appUrl: String? = null) {
        val username = _username.trim()
        val email = _email.trim().toLowerCase()
        val myAppUrl = appUrl ?: mailProperties.appUrl
        userRepository.findOneByLoginIgnoreCase(username) ?.run {
            log.info("Username $username already in use")
            throw UsernameAlreadyRegisteredException("Username already in use")
        }
        userRepository.findByEmailIgnoreCase(email) ?.run {
            log.info("Email $email already in use")
            throw EmailAlreadyRegisteredException("Email already in use")
        }
        // new user so we create user and send confirmation e-mail
        // Disable user until they click on confirmation link in email
        var user = User(username, email, pwEncoder.encode(password))
        user = userRepository.save(user)
        val token = ActivationToken(UUID.randomUUID().toString(), user)
        tokenRepository.save(token)

        // Send Email
        val registrationEmail = SimpleMailMessage()
        registrationEmail.setTo(email)
        registrationEmail.setSubject(mailProperties.registrationSubject)
        registrationEmail.setText("${mailProperties.registrationMessage}\n${myAppUrl}/confirm?token=${token.token}")
        registrationEmail.setFrom(mailProperties.from)

        // Run mail sending async
        thread {
            mailSender.send(registrationEmail)
        }
    }

    @Throws(TokenException::class)
    fun activateUser(token: String) {
        val activationToken = tokenRepository.findByToken(token) ?: throw TokenException("Token unknown")

        // Check if token still valid
        val oldestDateToBeAccepted = LocalDateTime.now().minusSeconds(mailProperties.tokenMaxAge)
        if(activationToken.createdDate.isPresent && activationToken.createdDate.get().isAfter(oldestDateToBeAccepted)) {
            // Activate user
            activationToken.user.enabled = true
            userRepository.save(activationToken.user)
            tokenRepository.delete(activationToken)
            return
        }

        // Token either to old or no creation date set
        tokenRepository.delete(activationToken)
        throw TokenException("Token expired")
    }

}