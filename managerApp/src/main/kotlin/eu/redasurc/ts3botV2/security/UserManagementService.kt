package eu.redasurc.ts3botV2.security

import eu.redasurc.ts3botV2.config.EmailProperties
import eu.redasurc.ts3botV2.domain.*
import eu.redasurc.ts3botV2.domain.entity.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
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
     *
     * @return true if pw matches, false if not.
     * @throws UsernameNotFoundException if user cannot be found.
     */
    @Throws(UsernameNotFoundException::class)
    fun bind(username: Username, password: Password) : Boolean {
        val user = userRepository.findOneByLoginIgnoreCase(username) ?: throw UsernameNotFoundException("User not found")
        return pwEncoder.matches(password, user.pw)
    }

    /**
     * Register an user with given username, email and password
     * @param appUrl Application url for the registration email. Will be taken from config if empty.
     * @throws RegistrationException if registration encounters an error
     */
    @Throws(RegistrationException::class)
    fun registerUser(_username: Username, _email: String, password: Password, appUrl: String? = null) {
        val username = _username.trim()
        val email = _email.trim().toLowerCase()
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
        sendActivationEmail(user, appUrl)
    }

    @Throws(TokenException::class)
    fun useToken(token: String) : TokenType{
        val securityToken = tokenRepository.findByToken(token) ?: throw TokenException("Token unknown")

        // Check if token too old and if, resend the activation email
        if(tokenExpired(securityToken)) {
            val user = securityToken.user
            // Token either to old or no creation date set
            tokenRepository.delete(securityToken)

            // Resend token
            when(securityToken.type) {
                TokenType.ACTIVATION_TOKEN -> sendActivationEmail(user)
                TokenType.PW_RESET_TOKEN -> sendResetPwEmail(user)
            }

            throw TokenExpiredException("Token expired")
        }
        if(securityToken.type == TokenType.ACTIVATION_TOKEN) {
            securityToken.user.enabled = true
            userRepository.save(securityToken.user)
            tokenRepository.delete(securityToken)

        }
        // Activate user
        return securityToken.type
    }

    private fun sendActivationEmail(user: User, appUrl: String? = null) {
        sendTokenEmail(user, appUrl, TokenType.ACTIVATION_TOKEN,
                mailProperties.registrationSubject, mailProperties.registrationMessage)
    }

    /**
     * Send a pw reset email or ignore if email not found
     */
    fun sendResetPwEmail(email: String, appUrl: String? = null) {
        // Get user or abort without exception
        val user = userRepository.findByEmailIgnoreCase(email.trim()) ?: return
        sendResetPwEmail(user, appUrl)
    }
    fun sendResetPwEmail(user: User, appUrl: String? = null) {
        sendTokenEmail(user, appUrl, TokenType.PW_RESET_TOKEN,
                mailProperties.pwResetSubject, mailProperties.pwResetMessage)
    }

    private fun sendTokenEmail(user: User, appUrl: String?, tokenType: TokenType, subject: String, message: String) {
        val myAppUrl = appUrl ?: mailProperties.appUrl

        // prepare and save token
        val token = SecurityToken(UUID.randomUUID().toString(), user, tokenType)
        tokenRepository.save(token)

        val msg = message
                .replace("{USERNAME}", user.login, true)
                .replace("{TOKEN}", "${myAppUrl}/token?token=${token.token}", true)

        // Prepare Email
        val registrationEmail = SimpleMailMessage()
        registrationEmail.setTo(user.email)
        registrationEmail.setSubject(subject)
        registrationEmail.setText(msg)
        registrationEmail.setFrom(mailProperties.from)

        // Send Email Async to not block the current thread
        thread {
            mailSender.send(registrationEmail)
        }
    }

    private fun tokenNotExpired(activationToken: SecurityToken) : Boolean {
        // Check if token still valid
        val oldestDateToBeAccepted = LocalDateTime.now().minusSeconds(mailProperties.tokenMaxAge)
        return activationToken.createdDate.isPresent && activationToken.createdDate.get().isAfter(oldestDateToBeAccepted)
    }
    private fun tokenExpired(activationToken: SecurityToken) : Boolean {
        return !tokenNotExpired(activationToken)
    }

    fun resetPW(token: String, pw: String) {
        val securityToken = tokenRepository.findByTokenAndType(token, TokenType.PW_RESET_TOKEN)
                                                                    ?: throw TokenException("Token unknown")
        // Check if token too old
        if(tokenExpired(securityToken)) {
            throw TokenExpiredException("Token expired")
        }

        val user = securityToken.user
        user.pw = pwEncoder.encode(pw)

        // Activate user if not already activated
        // (Separate activation is not necessary. PW reset token gets send per mail)
        if(!user.enabled) {
            user.enabled = true
        }
        user.credentialsNonExpired = true
        userRepository.save(user)
        tokenRepository.delete(securityToken)
    }

    @Scheduled(cron = "0 15 05 * * ?") // everyday on 05:15
    fun scheduleTaskUsingCronExpression() {
        log.info("Cleaning up expired tokens and registrations")

        // 3 times expiration time before deleting
        val oldestDateToBeAccepted = Date.from(
                LocalDateTime.now()
                        .minusSeconds(mailProperties.tokenMaxAge * 3)
                        .atZone(ZoneId.systemDefault()).toInstant())
        val expiredToken = tokenRepository.findAllByCreatedDateBefore(oldestDateToBeAccepted)
        tokenRepository.deleteAll(expiredToken)
        log.info("Clean up removed ${expiredToken.size} expired tokens")
    }

}