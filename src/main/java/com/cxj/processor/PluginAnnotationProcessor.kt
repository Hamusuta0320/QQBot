package com.cxj.processor

import com.cxj.filter.IChannelFilter
import com.cxj.plugins.PluginRegister
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component
object PluginAnnotationProcessor: BeanPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {
    var ctx: ApplicationContext? = null
    var channel: EventChannel<BotEvent>? = null
    var globalBot: Bot? = null

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.ctx = applicationContext
    }

    suspend fun once(e: MessageEvent, action: suspend (MessageEvent)->Unit) {
        val r = globalBot?.eventChannel?.nextEvent<MessageEvent> {
            return@nextEvent it.sender.id == e.sender.id
        }
        r?.let {
            action(it)
        }
    }

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        println("容器刷新事件")
        val bot = ctx!!.getBean(Bot::class.java)
        globalBot = bot
        channel = bot.eventChannel
        ctx!!.getBeansOfType(IChannelFilter::class.java).forEach {kv->
            channel = channel!!.filter(kv.value::filter)
        }

        println("执行事件过滤完成")
        ctx!!.getBeansOfType(PluginRegister::class.java).forEach {kv->
            kv.value.register(bot, channel!!)
        }
        println("订阅注册完成")

        runBlocking {
            bot.login()
        }
    }
}
