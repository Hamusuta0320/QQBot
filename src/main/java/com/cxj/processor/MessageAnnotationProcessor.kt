package com.cxj.processor

import com.cxj.config.GlobalConfig
import com.cxj.filter.*
import com.cxj.plugins.PluginRegister
import io.ktor.util.reflect.*
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.UserMessageEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

@Component
object MessageAnnotationProcessor: ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {
    var ctx: ApplicationContext? = null
    var channel: EventChannel<BotEvent>? = null
    var filteredChannel: EventChannel<BotEvent>? = null

    private fun toFilter(c: EventChannel<BotEvent>, e: GroupMessage): EventChannel<BotEvent> {
        return c.filter {
            if(it is GroupMessageEvent) {
                return@filter when(e.messageRule) {
                    MessageRule.EQUAL -> it.message.contentToString() == e.text
                    MessageRule.BEGIN_WITH -> it.message.contentToString().startsWith(e.text)
                    MessageRule.END_WITH -> it.message.contentToString().endsWith(e.text)
                    MessageRule.REGEX -> Regex(e.text).matches(it.message.contentToString())
                    MessageRule.CONTAINS -> it.message.contentToString().contains(e.text)
                    MessageRule.IN -> e.text.split(",").contains(it.message.contentToString())
                    MessageRule.COMMAND -> it.message.contentToString().startsWith(e.text)
                }
            } else {
                return@filter false
            }
        }
    }

    private fun toFilter(c: EventChannel<BotEvent>, e: UserMessage): EventChannel<BotEvent> {
        return c.filter {
            if(it is UserMessageEvent) {
                return@filter when(e.messageRule) {
                    MessageRule.EQUAL -> it.message.contentToString() == e.text
                    MessageRule.BEGIN_WITH -> it.message.contentToString().startsWith(e.text)
                    MessageRule.END_WITH -> it.message.contentToString().endsWith(e.text)
                    MessageRule.REGEX -> Regex(e.text).matches(it.message.contentToString())
                    MessageRule.CONTAINS -> it.message.contentToString().contains(e.text)
                    MessageRule.IN -> it.message.contentToString().split(",").contains(e.text)
                    MessageRule.COMMAND -> it.message.contentToString().startsWith(e.text)
                }
            } else {
                return@filter false
            }
        }
    }

    private fun toFilter(c: EventChannel<BotEvent>, e: Message): EventChannel<BotEvent> {
        return c.filter {
            if(it is MessageEvent) {
                return@filter when(e.messageRule) {
                    MessageRule.EQUAL -> it.message.contentToString() == e.text
                    MessageRule.BEGIN_WITH -> it.message.contentToString().startsWith(e.text)
                    MessageRule.END_WITH -> it.message.contentToString().endsWith(e.text)
                    MessageRule.REGEX -> Regex(e.text).matches(it.message.contentToString())
                    MessageRule.CONTAINS -> it.message.contentToString().contains(e.text)
                    MessageRule.IN -> it.message.contentToString().split(",").contains(e.text)
                    MessageRule.COMMAND -> it.message.contentToString().startsWith(e.text)
                }
            } else {
                return@filter false
            }
        }
    }

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val bot = PluginAnnotationProcessor.ctx!!.getBean(Bot::class.java)

        channel = bot.eventChannel
        filteredChannel = bot.eventChannel
        ctx!!.getBeansOfType(IChannelFilter::class.java).forEach { kv->
            filteredChannel = filteredChannel!!.filter(kv.value::filter)
        }

        ctx!!.beanDefinitionNames.forEach { beanName->
            val bean = ctx!!.getBean(beanName)
            println(beanName)
            bean::class.declaredMemberFunctions.forEach continuing@  { method->
                method.findAnnotation<Message>()?.let {message->
                    if(message.messageRule == MessageRule.COMMAND) {
                        GlobalConfig.commands[message.text] = mapOf("desc" to message.desc)
                    }
                    if(message.filter) {
                        toFilter(filteredChannel!!, message).subscribeAlways<MessageEvent> {
                            if(method.isSuspend) {
                                method.callSuspend(bean, it)
                            } else {
                                method.call(bean, it)
                            }
                        }
                    } else {
                        toFilter(channel!!, message).subscribeAlways<MessageEvent> {
                            if(method.isSuspend) {
                                method.callSuspend(bean, it)
                            } else {
                                method.call(bean, it)
                            }
                        }
                    }
                    return@continuing
                }

                method.findAnnotation<GroupMessage>()?.let {message->
                    if(message.messageRule == MessageRule.COMMAND) {
                        GlobalConfig.commands[message.text] = mapOf("desc" to message.desc)
                    }
                    if(message.filter) {
                        toFilter(filteredChannel!!, message).subscribeAlways<GroupMessageEvent> {
                            if(method.isSuspend) {
                                method.callSuspend(bean, it)
                            } else {
                                method.call(bean, it)
                            }
                        }
                    } else {
                        toFilter(channel!!, message).subscribeAlways<GroupMessageEvent> {
                            if(method.isSuspend) {
                                method.callSuspend(bean, it)
                            } else {
                                method.call(bean, it)
                            }
                        }
                    }
                    return@continuing
                }
                method.findAnnotation<UserMessage>()?.let { message->
                    if(message.messageRule == MessageRule.COMMAND) {
                        GlobalConfig.commands[message.text] = mapOf("desc" to message.desc)
                    }
                    if(message.filter) {
                        toFilter(filteredChannel!!, message).subscribeAlways<UserMessageEvent> {
                            if(method.isSuspend) {
                                method.callSuspend(bean, it)
                            } else {
                                method.call(bean, it)
                            }
                        }
                    } else {
                        toFilter(channel!!, message).subscribeAlways<UserMessageEvent> {
                            if(method.isSuspend) {
                                method.callSuspend(bean, it)
                            } else {
                                method.call(bean, it)
                            }
                        }
                    }
                    return@continuing
                }
            }
        }

    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.ctx = applicationContext
    }
}