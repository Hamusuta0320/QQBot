package com.cxj.plugins.wuziqi

import ego.gomoku.player.GomokuPlayer

class ComputerUser(private val com: Computer, override val id: Long): GameUser(id) {
    override fun place(map: Array<IntArray>): Point? {
        return com.cal(map)
    }
}