package com.cxj.plugins.wuziqi

import ego.gomoku.enumeration.Color
import ego.gomoku.enumeration.Level
import ego.gomoku.helper.MapDriver
import ego.gomoku.player.GomokuPlayer

class MediumComputer(private val color: Int): Computer(color) {
    override fun cal(map: Array<IntArray>): Point? {
        map.forEach {
            it.forEach {iii->
                print(iii)
                print(",")
            }
            println()
        }
        val readMap = MapDriver.readMap(map)
        val aiColor = if(color == 1) Color.BLACK else Color.WHITE
        val gomokuPlayer = GomokuPlayer(readMap, Level.NORMAL)
        val result = gomokuPlayer.play(aiColor).point ?: return null
        return Point(result.y, result.x, color)
    }
}