package eu.redasurc.ts3botV2

import eu.redasurc.ts3botV2.config.BruteForceSettings
import eu.redasurc.ts3botV2.config.CaptchaSettings
import eu.redasurc.ts3botV2.config.EmailProperties
import eu.redasurc.ts3botV2.config.MonitorProperties
import eu.redasurc.ts3botV2.domain.entity.ClanRepository
import eu.redasurc.ts3botV2.domain.entity.ServerPermissions
import eu.redasurc.ts3botV2.domain.entity.User
import eu.redasurc.ts3botV2.domain.entity.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct


@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties(MonitorProperties::class, EmailProperties::class,
        CaptchaSettings::class, BruteForceSettings::class)
class TS3Bot

fun main(args: Array<String>) {
    runApplication<TS3Bot>(*args){
        setBannerMode(Banner.Mode.OFF)
    }
}


@Service
class Initializer (@Autowired val userRepository: UserRepository,
                   @Autowired val pwEncoder: PasswordEncoder) {


    @PostConstruct
    fun init() {
        userRepository.findOneByLoginIgnoreCase("admin")?.run {
            return  // If admin exists skip this
        }

        val user = User("admin", "admin@localhost", pwEncoder.encode("password"),
                enabled = true, permission = ServerPermissions.SERVERADMIN)
        userRepository.save(user)
    }
}