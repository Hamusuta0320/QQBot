package com.cxj.plugins.wuziqi

import com.cxj.filter.GroupMessage
import com.cxj.filter.Listener
import com.cxj.filter.MessageRule
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.contact.asFriend
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageChainBuilder
import java.io.ByteArrayInputStream
import java.util.concurrent.locks.ReentrantLock

@Listener
class WuziqiApp {
    private var game: WuziqiGame? = null
    private val lock = Mutex()

    fun getGameMode(s: String): GameMode {
        return if(s == "双人对战") {
            GameMode.HUMAN_HUMAN
        } else {
            GameMode.HUMAN_COM
        }
    }

    @GroupMessage(filter = true, messageRule = MessageRule.IN, text = "双人对战,人机对战,人机对战简单,人机对战普通,人机对战困难")
    suspend fun joinGame(e: GroupMessageEvent) {
        lock.lock()
        try {
            val m = e.message.contentToString()
            if(game == null) {
                game = WuziqiGame(getGameMode(m))
                if(m == "人机对战简单") {
                    game!!.aiLevel = ComputerLevel.EASY
                } else if(m == "人机对战普通") {
                    game!!.aiLevel = ComputerLevel.MEDIUM
                } else if(m == "人机对战困难") {
                    game!!.aiLevel = ComputerLevel.DIFFICULT
                }

            } else {
                if (getGameMode(m) != game!!.gameMode) {
                    e.subject.sendMessage("正在运行其他模式对局")
                    return
                }
            }
            val uid = e.sender.id
            val from = e.subject
            val r = game!!.join(uid)
            if(r.getCode() == 200) {
                val pick = if(game!!.isBlack(uid)) "黑" else "白"
                val mcb = MessageChainBuilder()
                mcb.append(At(uid))
                mcb.append("成功加入对局,执${pick}")
                from.sendMessage(mcb.build())
            } else {
                val mcb = MessageChainBuilder()
                mcb.append(At(uid))
                mcb.append(r.getErr())
                from.sendMessage(mcb.build())
                return
            }
            if(game!!.isReady()) {
                val mcb = MessageChainBuilder()
                game!!.players.filter {
                    it.value.id > 0
                }.forEach {
                    mcb.append(At(it.value.id))
                }
                val black = game!!.players["black"]!!.id
                mcb.append("游戏开始")
                mcb.append("请")
                mcb.append(At(black))
                mcb.append("先落子,落子规则:字母+数字,如:G6表示在G列6行落子")
                from.sendMessage(mcb.build())
                val curBoard = game!!.curBoard()
                e.subject.sendImage(
                    ByteArrayInputStream(
                        curBoard.toByteArray()
                    ),
                    "png"
                )
            }
        } finally {
            lock.unlock()
        }
    }

    @GroupMessage(filter = true, messageRule = MessageRule.EQUAL, text = "悔棋")
    suspend fun rePlace(e: GroupMessageEvent) {
        lock.lock()
        try {
            game?.let {
                val uid = e.sender.id
                if(it.gameMode == GameMode.HUMAN_COM && it.containsUser(uid)) {
                    it.rePlaceWithAi()
                } else if(it.containsUser(uid) && it.curUser().id != uid && it.started) {
                    it.rePlace()
                } else {
                    e.subject.sendMessage(uid.atWithMessage("未处于棋局中或者本回合轮到你下棋,无法悔棋"))
                    return
                }
                e.subject.sendMessage(uid.atWithMessage("提出了悔棋"))
                val curBoard = it.curBoard()
                e.subject.sendImage(
                    ByteArrayInputStream(
                        curBoard.toByteArray()
                    ),
                    "png"
                )
            }
        }finally {
            lock.unlock()
        }
    }

    @GroupMessage(filter = true, messageRule = MessageRule.EQUAL, text = "认输")
    suspend fun renshu(e: GroupMessageEvent) {
        lock.lock()
        try {
            game?.let {
                val uid = e.sender.id
                if(it.containsUser(uid)) {
                    e.subject.sendMessage(uid.atWithMessage("你已认输,游戏结束"))
                    game = null
                    return
                }
            }
        }finally {
            lock.unlock()
        }
    }

    @GroupMessage(filter = true, messageRule = MessageRule.REGEX, text = "\\w\\d{1,2}")
    suspend fun place(e: GroupMessageEvent) {
        lock.lock()
        try {
            game?.let {
                val uid = e.sender.id
                if(!it.started || !it.containsUser(uid)) {
                    e.subject.sendMessage(uid.atWithMessage("没有开始的对局,或者你不在游戏中"))
                    return
                }

                if(it.curUser().id != uid) {
                    e.subject.sendMessage(uid.atWithMessage("当前未轮到你"))
                    return
                }

                // 落子合法性
                val regex = Regex("(\\w)(\\d{1,2})")
                val matchEntire = regex.matchEntire(e.message.contentToString())
                if(matchEntire == null || matchEntire.groupValues.size < 3) {
                    e.subject.sendMessage(uid.atWithMessage("落子格式错误"))
                    return
                }

                val col = matchEntire.groupValues[1].uppercase()
                val row = matchEntire.groupValues[2].toInt() - 1

                if(!Global.m.contains(col)) {
                    e.subject.sendMessage(uid.atWithMessage("列错误"))
                    return
                }
                if(row < 0 || row > 14) {
                    e.subject.sendMessage(uid.atWithMessage("行错误"))
                    return
                }

                if(!it.canPlace(col, row)) {
                    e.subject.sendMessage(uid.atWithMessage("这个点已经落子了"))
                    return
                }

                it.place(Global.m[col]!!, row)
                val curBoard = it.curBoard()
                e.subject.sendImage(
                    ByteArrayInputStream(
                        curBoard.toByteArray()
                    ),
                    "png"
                )
                if(it.isWin() || it.aiDefeat) {
                    e.subject.sendMessage(it.winner!!.id.atWithMessage("我愿称你为最强,你赢了"))
                    game = null
                }
            }
        }finally {
            lock.unlock()
        }
    }

    private fun Long.atWithMessage(msg: String): MessageChain {
        val mcb = MessageChainBuilder()
        mcb.append(At(this))
        mcb.append(msg)
        return mcb.build()
    }
}