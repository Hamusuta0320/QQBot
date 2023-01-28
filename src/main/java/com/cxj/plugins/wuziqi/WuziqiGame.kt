package com.cxj.plugins.wuziqi

import ego.gomoku.enumeration.Level
import java.io.ByteArrayOutputStream
import java.util.LinkedList

class WuziqiGame(val gameMode: GameMode) {
    var started: Boolean = false
    var players: MutableMap<String, GameUser> = HashMap()
    private var board: Board = Board(15, 15)
    var num: Int = 0
    private val history = LinkedList<Point>()
    var winner: GameUser? = null
    var aiLevel: ComputerLevel = ComputerLevel.EASY
    var aiDefeat = false

    fun containsUser(uid: Long): Boolean = players.filter { it.value.id == uid }.isNotEmpty()

    fun isBlack(uid: Long): Boolean = players.filter { it.value.id == uid && it.key == "black" }.isNotEmpty()

    fun isBan(col: Int, row: Int, who: Int): Boolean {
        val p = Point(col, row, who)
        return board.isBan(p)
    }

    fun rePlace() {
        val p = history.last
        board.place(p.x, p.y, 0)
        num--
        history.removeLast()
    }
    fun rePlaceWithAi() {
        if(curUser().id < 0) {
            val p = history.last
            board.place(p.x, p.y, 0)
            num--
            history.removeLast()
        } else {
            val p = history.last
            board.place(p.x, p.y, 0)
            num--
            history.removeLast()
            val p2 = history.last
            board.place(p2.x, p2.y, 0)
            num--
            history.removeLast()
        }
    }
    fun join(uid: Long): R<Unit> {
        if(containsUser(uid)) {
            return R.error(201, "你已在游戏中,请不要重复进入游戏")
        }
        if(players.size >= 2) {
            val black = players["black"]
            val white = players["white"]
            return R.error(201, "对局已开始,现在是${black!!.id}(执黑)与${white!!.id}(执白)正在对局,请等待下局")
        }
        if(gameMode == GameMode.HUMAN_COM) {
            val u = randowPick()
            val o = otherColor(u)
            players[o] = ComputerUser(DefaultComputerFactory().getComputer(aiLevel, colorStringtoInt(o)), -4)
            players[u] = RealUser(uid)
            if(u != "black") {
                comPlace()
            }
        } else {
            if (players.isEmpty()) {
                players["black"] = RealUser(uid)
            } else {
                players["white"] = RealUser(uid)
            }
        }
        if(players.size == 2) {
            started = true
        }
        return R.ok()
    }

    private fun colorStringtoInt(color: String): Int {
        return if(color == "black") 1 else if (color == "white") 2 else 0
    }

    private fun otherColor(color: String): String {
        return if(color == "white") "black" else "white"
    }

    private fun randowPick(): String {
        return listOf("black", "white").random()
    }

    fun isReady(): Boolean {
        return players.size == 2
    }

    fun curUser(): GameUser {
        return if(num % 2 == 0) players["black"]!! else players["white"]!!
    }

    fun curBoard(): ByteArrayOutputStream {
        return board.patinBoardWithChequer(history.lastOrNull())
    }

    fun canPlace(col: String, row: Int): Boolean {
        return board.canPlace(col, row)
    }

    fun place(col: Int, row: Int) {
        val who = if(num % 2 == 0) 1 else 2
        board.place(col, row, who)
        num++
        history.add(Point(col, row, who))

        // 人机走棋
        if(gameMode == GameMode.HUMAN_COM) {
            val r = comPlace()
            if(!r) {
                aiDefeat = true
                winner = players.filter { it.value.id > 0 }.values.first()
            }
        }
    }

    private fun comPlace(): Boolean {
        val p = curUser().place(board._board) ?: return false
        board.place(p.x, p.y, p.who)
        num++
        history.add(p)
        return true
    }

    private fun intToColorString (color: Int): String {
        return if(color == 1) {
            "black"
        } else {
            "white"
        }
    }

    fun isWin(): Boolean {
        val last = history.last
        if(board.isWin(last)) {
            winner = players[intToColorString(last.who)]!!
            return true
        }
        return false
    }
}