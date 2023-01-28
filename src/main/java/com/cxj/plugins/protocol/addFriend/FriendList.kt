package com.cxj.plugins.protocol.addFriend

import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.Stranger

fun Member.add() {
    check(bot.friends[id] != null) {
        "Friend $id had already been deleted"
    }
}


fun Stranger.add() {

}