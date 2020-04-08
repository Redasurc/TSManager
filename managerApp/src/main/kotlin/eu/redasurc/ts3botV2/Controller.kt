package eu.redasurc.ts3botV2

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest


@Controller
class HtmlController {

    @GetMapping("/")
    fun blog(model: Model): String {
        model["title"] = "TestPAGE"
        return "test"
    }


}

@Controller
class MyErrorController : ErrorController {

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest, model: Model): String? {
        model["code"] = 505
        model["message"] = "Unknown error"
        model["slogan"] = "Either you or we messed up"

        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        if (status != null) {
            val statusCode = Integer.valueOf(status.toString())
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model["code"] = 404
                model["message"] = "Page Not Found"
                model["slogan"] = "It looks like you found a glitch in the matrix..."
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model["code"] = 500
                model["message"] = "Internal Server Error"
                model["slogan"] = "Ummm this is award...."
            }
        }
        return "error"
    }

    override fun getErrorPath(): String {
        return "/error"
    }

}