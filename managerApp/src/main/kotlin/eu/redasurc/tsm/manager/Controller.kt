package eu.redasurc.tsm.manager

import eu.redasurc.ts3botV2.security.getClientIP
import org.bouncycastle.util.encoders.Base64
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.concurrent.ThreadLocalRandom
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest


@Controller
class HtmlController {

    @GetMapping("/")
    fun blog(model: Model): String {
        model["title"] = "TestPAGE"
        return "test"
    }

    @PostMapping("/test")
    fun test(model: Model): String {
        model["title"] = "TestPAGE"
        return "test"
    }


}

@Controller
class CustomErrorController : ErrorController {
    private val log = LoggerFactory.getLogger(this::class.java)

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest, model: Model): String? {
        var code = -1
        var message = "Unknown error"
        var slogan = "It looks like you found a glitch in the matrix..."

        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        if (status != null && status is Int) {
            code = status
            try {
                message = HttpStatus.valueOf(status).reasonPhrase
            } catch (e: IllegalArgumentException) {}
            if (status == HttpStatus.NOT_FOUND.value()) {
                message = "Page Not Found"
            } else if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                slogan = "Ummm this is award...."
            }
        }

        // Generate Trace line in log
        model["trace"] = try {
            val r = ByteArray(9) //Means 2048 bit
            ThreadLocalRandom.current().nextBytes(r)
            val t = Base64.toBase64String(r)
            log.info("Error-page $code - $message, Trace: $t, Client: {}", getClientIP(request))
            t
        } catch (e: Exception) {
            log.warn("Generating Trace failed", e)
            ""
        }
        model["code"] = code
        model["message"] = message
        model["slogan"] = slogan
        return "error"
    }

    override fun getErrorPath(): String {
        return "/error"
    }

}