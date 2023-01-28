package com.cxj

import com.cxj.config.BotConfig
import org.springframework.context.annotation.AnnotationConfigApplicationContext

fun main(args: Array<String>) {
    AnnotationConfigApplicationContext(BotConfig::class.java)
}