package com.cxj.plugins.commandExecutor

import com.cxj.dao.DB
import com.cxj.filter.Listener
import com.cxj.filter.Message
import com.cxj.filter.MessageRule
import com.cxj.plugins.Plugin
import com.cxj.plugins.PluginRegister
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText

@Listener
object CommandExecutor {
    @Message(filter = true, messageRule = MessageRule.COMMAND, text = "/permit", desc="允许指定用户指定命令")
    suspend fun permit(e: MessageEvent) {
        val sender = e.sender.id
        val content = e.message.contentToString()
        if (sender != 2326955513L) {
            e.subject.sendMessage("你没有此项功能的权限")
            return
        }
        val command = content.split(" ").filter { it.isNotEmpty() }
        if(command.size != 3) {
            e.subject.sendMessage("Usage: /permit [userId] [commandType]")
            return
        }
        val userId = command[1].toLong()
        val commandType = command[2]
        DB.addPerm(commandType, userId)
        e.subject.sendMessage("已为${userId}添加${commandType}的执行权限")
    }

    @Message(filter = true, messageRule = MessageRule.COMMAND, text = "/ban", desc="禁止指定用户指定命令")
    suspend fun ban(e: MessageEvent) {
        val sender = e.sender.id
        val content = e.message.contentToString()
        if (sender != 2326955513L) {
            e.subject.sendMessage("你没有此项功能的权限")
            return
        }
        val command = content.split(" ").filter { it.isNotEmpty() }
        if(command.size != 3) {
            e.subject.sendMessage("Usage: /ban [userId] [commandType]")
            return
        }
        val userId = command[1].toLong()
        val commandType = command[2]
        DB.addPerm(commandType, userId)
        e.subject.sendMessage("已取消${userId}${commandType}的执行权限")
    }
    @Message(filter = true, messageRule = MessageRule.CONTAINS, text = "\n", desc="执行命令")
    suspend fun runCommand(e: MessageEvent) {
        val sender = e.sender.id
        val content = e.message.contentToString()
        val type = content.substring(0, content.indexOf("\n"))
        val code = content.substring(content.indexOf("\n")+1)
        val result = MessageChainBuilder()
        with(result) {
            when(type) {
                "py" -> {
                    if(!DB.perms["py"]!!.contains(sender)) {
                        append(At(sender))
                        append(PlainText(" "))
                        append(PlainText("不认识你嗷 铁子"))
                        return@with
                    }
                    kotlin.runCatching {
                        val commandResult = Processor.runPyByRuntime(code, null)
                        append(commandResult)
                    }.onFailure {
                        e.subject.sendMessage("命令执行出错:${it.message}")
                        return
                    }
                }
                "js" -> {
                    if(!DB.perms["js"]!!.contains(sender)) {
                        append(At(sender))
                        append(PlainText(" "))
                        append(PlainText("不认识你嗷 铁子"))
                        return@with
                    }
                    kotlin.runCatching {
                        val commandResult = Processor.runJsByRuntime(code, null)
                        append(commandResult)
                    }.onFailure {
                        e.subject.sendMessage("命令执行出错:${it.message}")
                        return
                    }
                }
                "sh" -> {
                    if(!DB.perms["sh"]!!.contains(sender)) {
                        append(At(sender))
                        append(PlainText(" "))
                        append(PlainText("不认识你嗷 铁子"))
                        return@with
                    }
                    kotlin.runCatching {
                        val commandResult = Processor.runShellByRuntime(code, null)
                        append(commandResult)
                    }.onFailure {
                        e.subject.sendMessage("命令执行出错:${it.message}")
                        return
                    }
                }
            }
        }
        if (result.build().isNotEmpty()) {
            e.subject.sendMessage(result.build())
        }
    }
}
