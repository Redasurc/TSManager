package eu.redasurc.tsm.manager.security

import eu.redasurc.tsm.manager.domain.*
import eu.redasurc.tsm.manager.domain.dto.User
import eu.redasurc.tsm.manager.domain.entity.TokenType
import eu.redasurc.tsm.manager.security.captcha.CaptchaService
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
import org.springframework.web.servlet.view.RedirectView
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid


@Controller
class RegisterController (private val userManagementService: UserManagementService,
                          private val bruteForceService: BruteForceService,
                          private val captchaService: CaptchaService) {

    @GetMapping("/login")
    fun login(request: HttpServletRequest, model: Model) : String {
        model.addAttribute("errorMessage", "Invalid username or password.")
        if (bruteForceService.isEnforcingCaptcha(getClientIP(request))
                || request.getParameter("captchaError") != null) {
            model.addAttribute("captchaSettings", captchaService.captchaSettings)
        }

        request.getSession(false)?.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)?.run {
            when(this) {
                is BruteForceException ->  model.addAttribute("errorMessage", this.message)
                is DisabledException -> model.addAttribute("errorMessage", "User not activated. Please check your email for the activation message.")
                else -> {}
            }

        }
        return "security/login";
    }

    // Return registration form template
    @RequestMapping(value = ["/register"], method = [RequestMethod.GET])
    fun showRegistrationPage(modelAndView: ModelAndView, user: User?): ModelAndView {
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)
        modelAndView.addObject("user", user)
        modelAndView.viewName = "security/register"
        return modelAndView
    }

    // Process form input data
    @RequestMapping(value = ["/register"], method = [RequestMethod.POST])
    fun processRegistrationForm(modelAndView: ModelAndView, user: @Valid User?, bindingResult: BindingResult, request: HttpServletRequest): ModelAndView {
        modelAndView.viewName = "security/register"
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)

        // Check Captcha
        if (captchaInvalid(request)) {
            modelAndView.addObject("warningMessage", "ReCaptcha not checked.")
            return modelAndView
        }

        user?: run {
            return modelAndView
        }

        val username = user.username
        val email = user.email

        if(bruteForceService.isRegistrationLocked(getClientIP(request))) {
            modelAndView.addObject("warningMessage", "Too many recent registrations. " +
                    "IP locked for 2 hours.")
            return modelAndView
        }

        if(bindingResult.hasErrors()) {
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
        checkPW(user) ?.run {
            modelAndView.addObject("passwordMessage", this)
            bindingResult.reject("pw")
            return modelAndView
        }


        try {
            userManagementService.registerUser(username, email, user.pw!!)  // PW = null is caught by checkPW()
        } catch (e: EmailAlreadyRegisteredException) {
            modelAndView.addObject("warningMessage", "Oops!  There is already a user registered with the email provided.")
            bindingResult.reject("email")
            return modelAndView
        } catch (e: UsernameAlreadyRegisteredException) {
            modelAndView.addObject("warningMessage", "Oops!  The username you've choosen is already taken.")
            bindingResult.reject("username")
            return modelAndView
        }

        // Log request with the BruteForce protection service
        bruteForceService.registrationAttempt(getClientIP(request))

        modelAndView.addObject("successMessage", "A confirmation e-mail has been sent to $email")
        modelAndView.addObject("title", "Registration success")
        modelAndView.viewName = "security/confirm"
        return modelAndView
    }

    // Process confirmation link
    @RequestMapping(value = ["/token"], method = [RequestMethod.GET])
    fun showConfirmationPage(modelAndView: ModelAndView, @RequestParam("token") token: String?, request: HttpServletRequest): ModelAndView {
        modelAndView.viewName = "security/confirm"
        modelAndView.addObject("title", "Account activation")

        // Error if BruteForce Service has blocked the IP.
        if (bruteForceService.isTokenLocked(getClientIP(request))) {
            modelAndView.addObject("invalidToken", "Too many invalid token attempts. IP address locked.")
            return modelAndView
        }

        // Error if toke is null.
        token?: run {
            modelAndView.addObject("invalidToken", "Oops!  This is an invalid token link.")
            return modelAndView
        }

        // Try activate the user with the given token.
        val tokenType = try {
             userManagementService.useToken(token)
        } catch (e: TokenExpiredException) {
            modelAndView.addObject("infoMessage", "Token has expired. We send you a new one per email.")
            return modelAndView
        } catch (e: TokenException) {
            modelAndView.addObject("invalidToken", "Oops!  This is an invalid token link.")
            bruteForceService.failedTokenAttempt(getClientIP(request))
            return modelAndView
        }

        when(tokenType) {
            TokenType.ACTIVATION_TOKEN -> {
                modelAndView.addObject("successMessage", "Success! You have been activated")
                modelAndView.addObject("gotoLogin", "Login")
            }
            TokenType.PW_RESET_TOKEN -> {
                modelAndView.addObject("captchaSettings", captchaService.captchaSettings)
                modelAndView.addObject("user", User())
                modelAndView.addObject("token", token)
                modelAndView.viewName = "security/forgot-password-enter-new"
            }
            else -> {
                modelAndView.addObject("invalidToken", "Oops!  I don't know what to do here... this shouldn't happen. Please contact an admin")
            }
        }
        return modelAndView

    }


    //  ---- RESET PW -----
    // Return reset pw form template
    @RequestMapping(value = ["/forgot-password"], method = [RequestMethod.GET])
    fun showResetPasswordPage(modelAndView: ModelAndView, user: User?): ModelAndView {
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)
        modelAndView.addObject("user", user)
        modelAndView.viewName = "security/forgot-password"
        return modelAndView
    }

    // Process form input data
    @RequestMapping(value = ["/forgot-password"], method = [RequestMethod.POST])
    fun processResetPasswordRequest(modelAndView: ModelAndView, user: @Valid User?, bindingResult: BindingResult, request: HttpServletRequest): ModelAndView {
        modelAndView.viewName = "security/forgot-password"
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)

        // Check Captcha
        if (captchaInvalid(request)) {
            modelAndView.addObject("errorMessage", "ReCaptcha not checked.")
            return modelAndView
        }

        if(bruteForceService.isRegistrationLocked(getClientIP(request))) {
            modelAndView.addObject("warningMessage", "Too many recent requests. " +
                    "IP locked for 2 hours.")
            return modelAndView
        }
        bruteForceService.registrationAttempt(getClientIP(request))

        // If email not blank try to send reset mail
        user?.email?.run {
            if (this.isNotBlank()) {
                userManagementService.sendResetPwEmail(this)
                modelAndView.addObject("user", User())
                modelAndView.addObject("successMessage", "Mail send.")
                return modelAndView
            }
        }

        modelAndView.addObject("errorMessage", "Please enter an email.")
        return modelAndView
    }

    @RequestMapping(value = ["/forgot-password/change"], method = [RequestMethod.GET])
    fun redirectResetPassword(): RedirectView {
        return RedirectView("/forgot-password");
    }

    @RequestMapping(value = ["/forgot-password/change"], method = [RequestMethod.POST])
    fun processResetPasswordForm(modelAndView: ModelAndView, user: @Valid User?,
                                 @RequestParam("token") token: String?,
                                 bindingResult: BindingResult, request: HttpServletRequest): ModelAndView {
        modelAndView.viewName = "security/forgot-password-enter-new"
        modelAndView.addObject("captchaSettings", captchaService.captchaSettings)
        modelAndView.addObject("token", token)
        // Check Captcha
        if(captchaInvalid(request)) {
            modelAndView.addObject("errorMessage", "ReCaptcha not checked.")
            return modelAndView
        }

        // Error if BruteForce Service has blocked the IP.
        if (bruteForceService.isTokenLocked(getClientIP(request))) {
            modelAndView.addObject("errorMessage", "Too many invalid token attempts. IP address locked.")
            return modelAndView
        }

        // Error if toke is null.
        if(token == null || user == null) {
            modelAndView.addObject("errorMessage", "Invalid request.")
            return modelAndView
        }

        checkPW(user) ?.run {
            modelAndView.addObject("errorMessage", this)
            bindingResult.reject("pw")
            return modelAndView
        }
        try {
            userManagementService.resetPW(token, user.pw!!) // PW null check in checkPW()
        } catch (e: TokenExpiredException) {
            modelAndView.addObject("errorMessage", "Token expired, please request a new password reset link.")
            return modelAndView
        } catch (e: TokenException) {
            modelAndView.addObject("errorMessage", "Token not found")
            bruteForceService.failedTokenAttempt(getClientIP(request))
            return modelAndView
        }
        modelAndView.viewName = "security/confirm"
        modelAndView.addObject("title", "Password reset successful")
        modelAndView.addObject("successMessage", "Success! Your password has been changed")
        modelAndView.addObject("gotoLogin", "Login")
        return modelAndView
    }

    /**
     * Checks given PW
     *
     * @return Error message or null if everything checks out
     */
    private fun checkPW(user: User) : String? {
        with(user) {
            if (pw.isNullOrBlank() || pwConfirm.isNullOrBlank()) {
                return "No password given."
            }
            if (pw != pwConfirm) {
                return "Oops! The passwords don't match."
            }
            // Check password strength
            val passwordCheck = Nbvcxz()
            val strength = passwordCheck.estimate(pw)
            if (strength.basicScore < 3) {
                return "Your password is too weak.  Choose a stronger one."
            }
        }
        return null
    }

    private fun captchaInvalid(request: HttpServletRequest): Boolean {
        val response = request.getParameter("g-recaptcha-response")
        try {
            captchaService.processResponse(response)
        } catch (e: Exception) {
            return true
        }
        return false
    }

}