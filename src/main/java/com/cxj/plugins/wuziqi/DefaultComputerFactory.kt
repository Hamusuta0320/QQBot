package com.cxj.plugins.wuziqi

class DefaultComputerFactory: ComputerFactory {
    override fun getComputer(level: ComputerLevel, color: Int): Computer {
        return when(level) {
            ComputerLevel.EASY->EasyComputer(color)
            ComputerLevel.MEDIUM->MediumComputer(color)
            ComputerLevel.DIFFICULT->DifficultComputer(color)
        }
    }
}