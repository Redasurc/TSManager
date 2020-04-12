package eu.redasurc.tsm.manager

import eu.redasurc.tsm.manager.config.BruteForceSettings
import eu.redasurc.tsm.manager.config.CaptchaSettings
import eu.redasurc.tsm.manager.config.EmailProperties
import eu.redasurc.tsm.manager.config.MonitorProperties
import eu.redasurc.tsm.manager.domain.entity.ServerPermissions
import eu.redasurc.tsm.manager.domain.entity.User
import eu.redasurc.tsm.manager.domain.entity.UserRepository
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
import javax.transaction.Transactional


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
    @Transactional
    fun init() {
//        println("Teamspeak points:")
//        userRepository.findAll().forEach {user ->
//            user.attributes["RankAttributes"] ?. run {
//                val attr = jacksonObjectMapper().treeToValue<UserRankAttributes>(this) ?: run {
//                    println("Deserializion for user ${user.login} failed, skipping...")
//                    return@forEach
//                }
//                println("${user.login} has ${attr.points} Teamspeak Points.")
//                return@forEach
//            }
//            println("No RankAttr object for user ${user.login}, skipping...")
//        }

        userRepository.findOneByLoginIgnoreCase("admin")?.run {
            return  // If admin exists skip this
        }

        val user = User("admin", "admin@localhost", pwEncoder.encode("password"),
                enabled = true, permission = ServerPermissions.SERVERADMIN)
        userRepository.save(user)
    }
}