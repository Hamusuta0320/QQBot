package com.cxj.plugins.protocol.addFriend

import kotlinx.serialization.Serializable
//uniPacket.setServantName("mqq.IMService.FriendListServiceServantObj");
//uniPacket.setFuncName("AddFriendReq");
//uniPacket.put("AF", addFriendReq);

@Serializable
class AddFriendReq (
    val uin: Long,
    val adduin: Long,
    val adduinsetting: Int,
    val myAllowFlag: Int = 1,
    val myfriendgroupid: Byte,
    val msgLen: Int = 0,
    val f30514msg: String? = "",
    val srcFlag: Byte,
    val autoSend: Byte = 1,
    val sig: ByteArray?,
    val sourceID: Int = 3999,
    val sourceSubID: Int = 0,
    val name: ByteArray?,
    val src_description: ByteArray?,
    val friend_src_desc: ByteArray?,
    val contact_bothway_friend: Boolean = false,
    val remark: ByteArray?,
    val showMyCard: Byte,
    val token: ByteArray?,
    val verify: ByteArray?
)
