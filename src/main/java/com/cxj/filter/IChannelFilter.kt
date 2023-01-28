package com.cxj.filter

import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent

interface IChannelFilter {
    fun filter(it: BotEvent): Boolean
}