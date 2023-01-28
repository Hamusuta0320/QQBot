package com.cxj.helper

import com.cxj.config.GlobalConfig
import com.cxj.filter.Listener
import com.cxj.filter.Message
import com.cxj.filter.MessageRule
import net.mamoe.mirai.event.events.MessageEvent

@Listener
object Helper {
    @Message(filter = false, messageRule = MessageRule.COMMAND, text="/help", desc = "帮助命令，列出所有命令信息")
    suspend fun help(e: MessageEvent) {
        e.subject.sendMessage(GlobalConfig.commands.map {
            it.key + ":\t" + it.value["desc"]
        }.joinToString("\n"))
    }
}