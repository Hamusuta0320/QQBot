package com.cxj.plugins.turnOnOrOff

import com.cxj.config.GlobalConfig
import com.cxj.filter.Filter
import com.cxj.filter.IChannelFilter
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content

@Filter
class ChannelFilter: IChannelFilter {
    override fun filter(it: BotEvent): Boolean {
        return if(GlobalConfig.TURN_ON || ((it as MessageEvent).message.content == "turn on" && it.sender.id == GlobalConfig.MANAGER)) {
            true
        } else {
            println("机器人已关闭")
            false
        }
    }
}