package com.cxj.plugins.wuziqi

class RealUser(override val id: Long): GameUser(id) {
    override fun place(map: Array<IntArray>): Point {
        return Point(0, 0, 0)
    }
}