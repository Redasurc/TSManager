package eu.redasurc.ts3botV2.service.bot.modules

import org.junit.jupiter.api.Test

internal class AutoCreateTeamChannelsTest {

    @Test
    fun testRegex() {
        val testStrings = listOf("<<managed|Team|2>>", "<<managed|Raid|5>>", "<<managed|2>>", "HelloWorld")
        val regex = "<<managed(\\|[A-Za-z0-9]+)*(\\|[A-Za-z0-9]+)+>>".toRegex()
        testStrings.forEach {
            val matchResult = regex.find(it)
            if(matchResult == null) {
                println( "String $it invalid")
            } else {
                println("String $it resulted in c1: ${matchResult.groupValues[1]} - c2: ${matchResult.groupValues[2]}")
            }
        }


    }
}