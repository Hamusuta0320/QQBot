package com.cxj.config



object GlobalConfig {
    val OS: String = System.getProperty("os.name")
    val HOME: String = if (OS.startsWith("Windows")) System.getenv("USERPROFILE") else System.getenv("HOME")
    val ACCOUNT = System.getenv("BotQQ").toLong()
    val PASSWORD: String = System.getenv("BotPass")
    const val WORKDIR = ".config/.user"
    var TURN_ON = true
    const val MANAGER = 2326955513L
    var commands: MutableMap<String, Map<String, String>> = HashMap()
}