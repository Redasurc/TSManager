package eu.redasurc.ts3botV2.security

import eu.redasurc.ts3botV2.security.captcha.CaptchaService
import eu.redasurc.ts3botV2.security.captcha.CaptchaVerificationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter




// TODO: Registration (https://www.baeldung.com/registration-with-spring-mvc-and-spring-security)

@EnableWebSecurity
class WebSecurityConfiguration(private val customUserDetailsService: CustomUserDetailsService,
                               private val passwordEncoderAndMatcher: PasswordEncoder,
                               private val captchaService: CaptchaService,
                               private val bruteForceService: BruteForceService)
    : WebSecurityConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        http
                .addFilterBefore(CaptchaVerificationFilter("/login", "/login?captchaError",
                        captchaService, bruteForceService), UsernamePasswordAuthenticationFilter::class.java)
        http
                .csrf().disable()
                .authorizeRequests()
                    .antMatchers("/api/*").authenticated()
                    .antMatchers("/register").permitAll()
                    .antMatchers("/confirm").permitAll()
                    .anyRequest().permitAll()
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll()
                    .and()
                .rememberMe()
                    .key("uniqueAndSecret")
                    .and()
                .logout()
                    .deleteCookies("JSESSIONID")
                    .permitAll()
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(configAuthenticationProvider())
    }


    fun configAuthenticationProvider(): DaoAuthenticationProvider {
        val impl = DaoAuthenticationProvider()
        impl.setUserDetailsService(customUserDetailsService)
        impl.setPasswordEncoder(passwordEncoderAndMatcher)
        impl.isHideUserNotFoundExceptions = false
        return impl
    }

}

/**
 * Password encode / decode config
 */
@Configuration
class PasswordEncoderAndMatcherConfig {

    @Bean
    fun passwordEncoderAndMatcher(): PasswordEncoder {
        return object : PasswordEncoder {
            override fun encode(rawPassword: CharSequence?): String {
                return BCryptPasswordEncoder().encode(rawPassword)
            }
            override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
                return BCryptPasswordEncoder().matches(rawPassword, encodedPassword)
            }
        }
    }
}