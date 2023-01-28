package com.cxj.plugins.wuziqi

import ego.gomoku.player.GomokuPlayer

abstract class GameUser(open val id: Long) {
    abstract fun place(map: Array<IntArray>): Point?
}