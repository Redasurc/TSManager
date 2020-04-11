package eu.redasurc.tsm.manager.security

import eu.redasurc.tsm.manager.domain.BruteForceException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.WebAttributes
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.security.web.authentication.WebAuthenticationDetails
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
class AuthenticationFailureListener(@Autowired private val bruteForceService: BruteForceService)
                                         : ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    override fun onApplicationEvent(e: AuthenticationFailureBadCredentialsEvent) {
        val auth = e.authentication.details as WebAuthenticationDetails
        bruteForceService.loginFailed(auth.remoteAddress, e.authentication.name)
    }
}

@Component
class AuthenticationSuccessEventListener(@Autowired private val bruteForceService: BruteForceService)
                                        : ApplicationListener<AuthenticationSuccessEvent> {

    override fun onApplicationEvent(e: AuthenticationSuccessEvent) {
        val auth = e.authentication.details as WebAuthenticationDetails
        bruteForceService.loginSucceeded(auth.remoteAddress, e.authentication.name)
    }
}

@Component
class CustomAuthenticationFailureHandler: SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(request: HttpServletRequest?, response: HttpServletResponse?, exception: AuthenticationException?) {
        setDefaultFailureUrl("/login?error=true")

        super.onAuthenticationFailure(request, response, exception)

        if (exception is _root_ide_package_.eu.redasurc.tsm.manager.domain.BruteForceException) {
            request!!.session.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception.message)
        }
    }
}