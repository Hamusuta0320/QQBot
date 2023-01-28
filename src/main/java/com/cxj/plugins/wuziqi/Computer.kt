package com.cxj.plugins.wuziqi

abstract class Computer(private val color: Int) {
    abstract fun cal(map: Array<IntArray>): Point?
}