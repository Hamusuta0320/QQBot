package com.cxj.plugins

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent

interface PluginRegister {
    fun register(bot: Bot, channel: EventChannel<BotEvent>)
}