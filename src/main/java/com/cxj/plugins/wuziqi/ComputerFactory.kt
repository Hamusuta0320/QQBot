package com.cxj.plugins.wuziqi

interface ComputerFactory{
    fun getComputer(level: ComputerLevel, color: Int): Computer
}