package com.cxj.plugins.test

import com.cxj.filter.*
import com.cxj.plugins.Plugin
import com.cxj.plugins.PluginRegister
import com.cxj.plugins.vm.VMTunnel
import com.cxj.processor.PluginAnnotationProcessor
import com.cxj.processor.PluginAnnotationProcessor.once
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.UserMessageEvent

@Listener
object Test {

    @GroupMessage(messageRule = MessageRule.BEGIN_WITH, text = "陈")
    fun begin(e: GroupMessageEvent) {
        println("begin")
        println(e.message.contentToString())
    }

    @GroupMessage(messageRule = MessageRule.CONTAINS, text = "巧")
    fun contain(e: GroupMessageEvent) {
        println("contains")
        println(e.message.contentToString())
    }

    @GroupMessage(messageRule = MessageRule.EQUAL, text = "保存消息")
    suspend fun save(e: GroupMessageEvent) {
        e.subject.sendMessage("请输入你要保存的消息")
        once(e) {
            it.subject.sendMessage("保存成功:${it.message.contentToString()}")
        }
    }

    @GroupMessage(messageRule = MessageRule.END_WITH, text = "周")
    fun end(e: GroupMessageEvent) {
        println("end")
        println(e.message.contentToString())
    }

    @GroupMessage(messageRule = MessageRule.REGEX, text = "\\d+")
    fun regex(e: GroupMessageEvent) {
        println("regex")
        println(e.message.contentToString())
    }

    @UserMessage(text = "hi")
    suspend fun sayHello(e: UserMessageEvent) {

        e.subject.sendMessage("hello")
    }

    @Message(text = "晚安")
    suspend fun sayGoodNight(e: MessageEvent) {
        e.subject.sendMessage("晚安")
        VMTunnel.getAllTunnels()
    }
}