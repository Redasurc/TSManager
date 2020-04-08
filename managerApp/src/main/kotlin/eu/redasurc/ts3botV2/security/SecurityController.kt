package eu.redasurc.ts3botV2.security

import eu.redasurc.ts3botV2.domain.BruteForceException
import eu.redasurc.ts3botV2.domain.EmailAlreadyRegisteredException
import eu.redasurc.ts3botV2.domain.TokenException
import eu.redasurc.ts3botV2.domain.UsernameAlreadyRegisteredException
import eu.redasurc.ts3botV2.domain.dto.User
import eu.redasurc.ts3botV2.security.captcha.CaptchaService
import me.gosimple.nbvcxz.Nbvcxz
import org.springframework.security.authentication.DisabledException
import org.springframework.security.web.WebAttributes
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


// https://www.codebyamir.com/blog/user-account-registration-with-spring-boot
// https://www.codebyamir.com/blog/forgot-password-feature-with-java-and-spring-boot

@Controller
class RegisterController (private val userManagementService: UserManagementService,
                          private val bruteForceService: BruteForceService,
                          private val captchaService: CaptchaService) {

    @GetMapping("/login")
    fun login(request: HttpServletRequest, model: Model) : String {
        model.addAttribute("errorMessage", "Invalid username or password.")
        if (bruteForceService.isEnforcingChapta(getClientIP(request))) {
            model.addAttribute("captchaSettings", captchaService.captchaSettings)
        }

        request.getSession(false)?.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)?.run {
            when(this) {
                is BruteForceException ->  model.addAttribute("errorMessage", this.message)
                is DisabledException -> model.addAttribute("errorMessage", "User not activated. Please check your email for the activation message.")
                else -> {}
            }

        }
        return "/login";
    }

    // Return registration form template
    @RequestMapping(value = ["/register"], method = [RequestMethod.GET])
    fun showRegistrationPage(modelAndView: ModelAndView, user: User?): ModelAndView {
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)
        modelAndView.addObject("user", user)
        modelAndView.viewName = "register"
        return modelAndView
    }

    // Process form input data
    @RequestMapping(value = ["/register"], method = [RequestMethod.POST])
    fun processRegistrationForm(modelAndView: ModelAndView, user: @Valid User?, bindingResult: BindingResult, request: HttpServletRequest): ModelAndView {
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)
        // Check Captcha
        val response = request.getParameter("g-recaptcha-response")
        try {
            captchaService.processResponse(response)
        } catch (e: Exception) {
            modelAndView.addObject("warningMessage", "ReCaptcha not checked.")
            return modelAndView
        }
        modelAndView.viewName = "register"
        user?: run {
            return modelAndView
        }

        val username = user.username
        val email = user.email
        val pw = user.pw
        val pwConfirm = user.pwConfirm

        if(bindingResult.hasErrors()) {
            return modelAndView
        }
        if(pw.isNullOrBlank() || pwConfirm.isNullOrBlank()){
            modelAndView.addObject("passwordMessage", "No password given")
            bindingResult.reject("pw")
            return modelAndView
        }
        if(email.isNullOrBlank()) {
            modelAndView.addObject("warningMessage", "No email provided.")
            bindingResult.reject("email")
            return modelAndView
        }
        if(username.isNullOrBlank()) {
            modelAndView.addObject("warningMessage", "No username provided.")
            bindingResult.reject("username")
            return modelAndView
        }
        if(pw != pwConfirm) {
            modelAndView.addObject("passwordMessage", "Oops! The passwords don't match")
            bindingResult.reject("pw")
            return modelAndView
        }
        // Check password strength
        val passwordCheck = Nbvcxz()
        val strength = passwordCheck.estimate(pw)
        if (strength.basicScore < 3) {
            modelAndView.addObject("passwordMessage", "Your password is too weak.  Choose a stronger one.")
            bindingResult.reject("pw")
            return modelAndView
        }


        try {
            userManagementService.registerUser(username, email, pw)
        } catch (e: EmailAlreadyRegisteredException) {
            modelAndView.addObject("warningMessage", "Oops!  There is already a user registered with the email provided.")
            bindingResult.reject("email")
            return modelAndView
        } catch (e: UsernameAlreadyRegisteredException) {
            modelAndView.addObject("warningMessage", "Oops!  The username you've choosen is already taken.")
            bindingResult.reject("username")
            return modelAndView
        }

        modelAndView.addObject("successMessage", "A confirmation e-mail has been sent to $email")
        modelAndView.addObject("title", "Registration success")
        modelAndView.viewName = "confirm"
        return modelAndView
    }

    // Process confirmation link
    @RequestMapping(value = ["/confirm"], method = [RequestMethod.GET])
    fun showConfirmationPage(modelAndView: ModelAndView, @RequestParam("token") token: String?): ModelAndView {
        modelAndView.viewName = "confirm"
        modelAndView.addObject("title", "Account activation")
        token?: run {
            modelAndView.addObject("invalidToken", "Oops!  This is an invalid confirmation link.")
            return modelAndView
        }
        try {
            userManagementService.activateUser(token)
        } catch (e: TokenException) {
            modelAndView.addObject("invalidToken", "Oops!  This is an invalid confirmation link.")
            return modelAndView
        }
        modelAndView.addObject("successMessage", "Success! You have been activated")
        modelAndView.addObject("gotoLogin", "Login")
        return modelAndView
    }

}