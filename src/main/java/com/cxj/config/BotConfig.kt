package com.cxj.config

import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.BotConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.io.File
import java.util.Arrays
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan(basePackages = ["com.cxj"])
open class BotConfig {
    @Bean
    open fun bot(): Bot{
        val b = BotFactory.newBot(GlobalConfig.ACCOUNT, GlobalConfig.PASSWORD) {
            heartbeatStrategy = BotConfiguration.HeartbeatStrategy.STAT_HB
            protocol = BotConfiguration.MiraiProtocol.IPAD
            workingDir = File(GlobalConfig.HOME, GlobalConfig.WORKDIR)
        }
        return b
    }
}



