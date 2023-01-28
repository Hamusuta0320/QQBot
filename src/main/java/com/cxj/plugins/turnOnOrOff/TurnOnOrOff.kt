package com.cxj.plugins.turnOnOrOff

import com.cxj.config.GlobalConfig
import com.cxj.plugins.Plugin
import com.cxj.plugins.PluginRegister
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content

@Plugin
object TurnOnOrOff: PluginRegister {
    override fun register(bot: Bot, channel: EventChannel<BotEvent>) {
        channel.subscribeAlways<MessageEvent> {
            if (it.message.content == "turn on" && it.sender.id == GlobalConfig.MANAGER) {
                GlobalConfig.TURN_ON = true
                it.subject.sendMessage("机器人已开机")
            } else if (it.message.content == "turn off" && it.sender.id == GlobalConfig.MANAGER) {
                GlobalConfig.TURN_ON = false
                it.subject.sendMessage("机器人已关机")
            }
        }
    }
}