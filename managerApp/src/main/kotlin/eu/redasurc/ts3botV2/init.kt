package eu.redasurc.ts3botV2

import eu.redasurc.ts3botV2.config.MonitorProperties
import eu.redasurc.ts3botV2.entity.*
import eu.redasurc.tsclient.TeamspeakClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.annotation.PostConstruct


@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(MonitorProperties::class)
class TS3Bot

fun main(args: Array<String>) {
    runApplication<TS3Bot>(*args){
        //setBannerMode(Banner.Mode.OFF)
    }
}


@Service
class Initializer (@Autowired val userRepository: UserRepository,
                   @Autowired val clanRepository: ClanRepository,
                   @Autowired val pwEncoder: PasswordEncoder,
                   @Autowired val monProp: MonitorProperties,
                   @Autowired val tsClient: TeamspeakClient) {


    @PostConstruct
    fun init() {
        println("CONFIG:")
        println(monProp)
        val user = User("redasurc", "redasurc@redasurc.com", pwEncoder.encode("ASD"), enabled = true)
        user.permission = ServerPermissions.SERVERADMIN

        val user2 = User("jack", "jack@jack.de", "asd", enabled = true)

        val id = TS3Identity("ABC", user)
        user.identitys.add(id)
        userRepository.save(user)
        userRepository.save(user2)
        val findAll = userRepository.findAll()
        println("All: $findAll")
        val user4 = userRepository.findByIdentitys_Uuid("ABC")
        println("Found $user4")

        val clan = Clan("STG", user)
        clan.addMember(user2, ClanPosition.MOD)
        clanRepository.save(clan)
        //tsClient.start()
    }
}