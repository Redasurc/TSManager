package eu.redasurc.ts3botV2

import eu.redasurc.ts3botV2.entity.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@SpringBootApplication
@EnableJpaAuditing
class TS3Bot

fun main(args: Array<String>) {
    runApplication<TS3Bot>(*args){
        //setBannerMode(Banner.Mode.OFF)
    }
}


@Service
class Initializer (@Autowired val userRepository: UserRepository,
                   @Autowired val clanRepository: ClanRepository, @Autowired val pwEncoder: PasswordEncoder) {


    @PostConstruct
    fun init() {
        val user = User("redasurc", "me@redasurc.eu", pwEncoder.encode("ASD"), enabled = true)
        user.permission = ServerPermissions.SERVERADMIN

        val user2 = User("jack", "jack@allround-gaming.eu", "asd", enabled = true)

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


    }
}