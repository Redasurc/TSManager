package eu.redasurc.tsm.manager.security.captcha

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import eu.redasurc.tsm.manager.config.CaptchaSettings
import eu.redasurc.tsm.manager.security.getClientIP
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.util.regex.Pattern
import javax.servlet.http.HttpServletRequest


val RECAPTCHA_RESPONSE_PATTERN : Pattern = Pattern.compile("[A-Za-z0-9_-]+")
const val RECAPTCHA_URL_TEMPLATE = "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s&remoteip=%s"

@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleResponse(var success: Boolean = false, var quote: String = "")

@Service
class CaptchaService(val request: HttpServletRequest,
                     val captchaSettings: CaptchaSettings,
                     val restTemplate: RestTemplate)  {


    private val log = LoggerFactory.getLogger(this::class.java)


    fun processResponse(response: String?) {
        securityCheck(response)
        val verifyUri: URI = URI.create(String.format(RECAPTCHA_URL_TEMPLATE, captchaSettings.secret, response, getClientIP(request)))
        try {
            val googleResponse: GoogleResponse = restTemplate.getForObject(verifyUri, GoogleResponse::class.java)
                    ?: throw ReCaptchaUnavailableException("ReCaptcha returned null object")
            log.debug("Google's response: {} ", googleResponse.toString())
            if (!googleResponse.success) {
//                if (googleResponse.hasClientError()) {
//                    // TODO: BRUTE FORCE PROTECTION FOR CAPTCHAS?
//                }
                throw ReCaptchaInvalidException("reCaptcha was not successfully validated")
            }
        } catch (rce: RestClientException) {
            throw ReCaptchaUnavailableException("Registration unavailable at this time.  Please try again later.", rce)
        }
    }

    protected fun securityCheck(response: String?) {
        log.debug("Attempting to validate response {}", response)
        // TODO: BRUTE FORCE PROTECTION FOR CAPTCHAS?
//        if (reCaptchaAttemptService.isBlocked(clientIP)) {
//            throw ReCaptchaInvalidException("Client exceeded maximum number of failed attempts")
//        }
        response?: throw ReCaptchaInvalidException("ReCaptcha missing")
        if (!responseSanityCheck(response)) {
            throw ReCaptchaInvalidException("Response contains invalid characters")
        }
    }

    protected fun responseSanityCheck(response: String): Boolean {
        return StringUtils.hasLength(response) && RECAPTCHA_RESPONSE_PATTERN.matcher(response).matches()
    }
}