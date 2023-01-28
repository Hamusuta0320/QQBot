package com.cxj.plugins.game

import com.cxj.plugins.Plugin
import com.cxj.plugins.PluginRegister
import com.cxj.plugins.game.entity.User
import com.cxj.store.RedisStore
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Calendar
import java.util.Date
import java.util.Random

//@Plugin
object Game: PluginRegister {
    private val client = OkHttpClient()
    private suspend fun signIn(sender: Member, group: Group) {
        val avatarUrl = sender.avatarUrl
        val toMsg = MessageChainBuilder()
        val request = Request.Builder().get().url(avatarUrl).build()
        val resp = client.newCall(request).execute()
        val inputStream = resp.body!!.byteStream()
        val uploadImage = group.uploadImage(inputStream)
        // 从redis获取个人数据
        val userData = User.fromJson(RedisStore.get("user:data:${sender.id}")) ?: User(sender.id, 0, 0, 0, 0, 0)
        val addMeiLi = 3
        val addTiLi = 10
        val addCoins = Random().nextInt(100)
        // 累计签到计算
        val userLastSignIn = userData.lastSignIn
        val userLastSignInDay = Calendar.Builder().setInstant(userLastSignIn).build()
        val prevSignIn = RedisStore.get("user:last:signIn") ?: Date().time.toString()
        val prevSignDay = Calendar.Builder().setInstant(prevSignIn.toLong()).build()
        val toDay = Calendar.getInstance()
        var rank = 0
        // 如果上次签到不是今天,重置签到计数器
        if(!(prevSignDay.get(Calendar.YEAR) == toDay.get(Calendar.YEAR) && prevSignDay.get(Calendar.DAY_OF_YEAR) == toDay.get(Calendar.DAY_OF_YEAR))) {
            RedisStore.set("user:signIn:rank", "0")
        }
        if(userLastSignInDay.get(Calendar.YEAR) == toDay.get(Calendar.YEAR) && userLastSignInDay.get(Calendar.DAY_OF_YEAR) == toDay.get(Calendar.DAY_OF_YEAR) - 1) {
            userData.alreadySignInDays += 1
            userData.lastSignIn = Date().time
        } else if(userLastSignInDay.get(Calendar.YEAR) == toDay.get(Calendar.YEAR) && userLastSignInDay.get(Calendar.DAY_OF_YEAR) == toDay.get(Calendar.DAY_OF_YEAR)){
            val a = MessageChainBuilder()
            a.append(At(sender))
            a.append(PlainText(" 你今天已经签到过了哦"))
            group.sendMessage(a.build())
            return
        } else {
            userData.alreadySignInDays = 1
            userData.lastSignIn = Date().time
        }
        if (RedisStore.get("user:signIn:rank") == null) {
            RedisStore.set("user:signIn:rank", "0")
        }
        rank = RedisStore.get("user:signIn:rank")!!.toInt()
        rank += 1
        RedisStore.set("user:signIn:rank", "$rank")
        RedisStore.set("user:last:signIn", Date().time.toString())
        with(userData) {
            tiLi += addTiLi
            meiLi += addMeiLi
            coins += addCoins
        }
        RedisStore.set("user:data:${sender.id}", userData.toJson())
        toMsg.append(Image.Builder.newBuilder(uploadImage.imageId).build())
        toMsg.append(PlainText("昵称: ${sender.nick}\n"))
        toMsg.append(PlainText("账号: ${sender.id}\n"))
        toMsg.append(PlainText("排行: 第 $rank 位签到\n"))
        toMsg.append(PlainText("累签: ${userData.alreadySignInDays} 天\n"))
        toMsg.append(PlainText("魅力: ${userData.meiLi}\n"))
        toMsg.append(PlainText("体力: ${userData.tiLi}\n"))
        toMsg.append(PlainText("金币: +${addCoins}\n"))
        toMsg.append(PlainText("金币余额: ${userData.coins}"))
        group.sendMessage(toMsg.build())
    }
    override fun register(bot: Bot, channel: EventChannel<BotEvent>) {
        channel.subscribeAlways<GroupMessageEvent> {
            val content = it.message.content
            val sender = it.sender
            val group = it.subject
            when {
                content == "签到" -> signIn(sender, group)
            }
        }
    }

}

