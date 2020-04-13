package eu.redasurc.tsm.manager

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import eu.redasurc.tsm.manager.config.BruteForceSettings
import eu.redasurc.tsm.manager.config.CaptchaSettings
import eu.redasurc.tsm.manager.config.EmailProperties
import eu.redasurc.tsm.manager.config.MonitorProperties
import eu.redasurc.tsm.manager.domain.entity.ServerPermissions
import eu.redasurc.tsm.manager.domain.entity.User
import eu.redasurc.tsm.manager.domain.entity.UserRepository
import eu.redasurc.tsm.manager.service.bot.modules.UserRankAttributes
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
import kotlin.system.measureTimeMillis


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
        println("Teamspeak points:")

        var num = 0;
        println("Finished Query in " + measureTimeMillis {
            num = userRepository.findAll().count()
        } + " and found $num entries")

        var de = 0
        val objectMapper = jacksonObjectMapper()
        println("Finished in " + measureTimeMillis {
            userRepository.getAttributesForAllUsers("RankAttributes").forEach {attrObj ->
                val attr = objectMapper.readValue<UserRankAttributes>(attrObj.data) ?: run {
                    println("Deserializion for user ${attrObj.user.login} failed, skipping...")
                    return@forEach
                }
                println("${attrObj.user.login} has ${attr.points} Teamspeak Points.")
            }
        } + " and deserialized $de entries")
        userRepository.findOneByLoginIgnoreCase("admin")?.run {
            return  // If admin exists skip this
        }

        val user = User("admin", "admin@localhost", pwEncoder.encode("password"),
                enabled = true, permission = ServerPermissions.SERVERADMIN)
        userRepository.save(user)
    }
}