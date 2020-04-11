package eu.redasurc.tsm.manager.security.captcha

import eu.redasurc.ts3botV2.security.BruteForceService
import eu.redasurc.ts3botV2.security.getClientIP
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * The filter to verify captcha.
 */
class CaptchaVerificationFilter(private val processUrl: String,
                                failureUrl: String,
                                private val captchaService: CaptchaService,
                                private val bruteForceService: BruteForceService)
                        : AbstractAuthenticationProcessingFilter(processUrl) {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req = request as HttpServletRequest
        val res = response as HttpServletResponse
        val captchaShown = request.getParameter("captcha")?.equals("true")?:false
        val username : String? = request.getParameter("username")
        if (processUrl == req.servletPath && "POST".equals(req.method, ignoreCase = true)
                && ((bruteForceService.isEnforcingCaptcha(getClientIP(request), username) || captchaShown))) {
            val captcha = request.getParameter("g-recaptcha-response")
            try {
                captchaService.processResponse(captcha)
            } catch (e: Exception) {
                if (captchaShown) {
                    unsuccessfulAuthentication(req, res, InsufficientAuthenticationException("Wrong captcha verification code."))
                } else {
                    unsuccessfulAuthentication(req, res, InsufficientAuthenticationException("You need to solve the captcha."))
                }
                return
            }

        }
        chain.doFilter(request, response)
    }

    @Throws(AuthenticationException::class, IOException::class, ServletException::class)
    override fun attemptAuthentication(httpServletRequest: HttpServletRequest, httpServletResponse: HttpServletResponse): Authentication? {
        return null
    }

    init {
        setAuthenticationFailureHandler(SimpleUrlAuthenticationFailureHandler(failureUrl))
    }
}