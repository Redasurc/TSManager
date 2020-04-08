package eu.redasurc.ts3botV2.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class Beans {
    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
