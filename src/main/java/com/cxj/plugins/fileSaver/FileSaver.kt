package com.cxj.plugins.fileSaver
//
//import com.cxj.config.GlobalConfig
//import com.cxj.filter.Listener
//import com.cxj.plugins.Plugin
//import com.cxj.plugins.PluginRegister
//import com.cxj.store.DefaultStore
//import com.cxj.store.RedisStore
//import com.cxj.util.NetUtils
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import net.mamoe.mirai.Bot
//import net.mamoe.mirai.contact.Contact.Companion.uploadImage
//import net.mamoe.mirai.contact.Group
//import net.mamoe.mirai.contact.file.AbsoluteFileFolder.Companion.extension
//import net.mamoe.mirai.event.EventChannel
//import net.mamoe.mirai.event.events.BotEvent
//import net.mamoe.mirai.event.events.MessageEvent
//import net.mamoe.mirai.event.events.MessageRecallEvent
//import net.mamoe.mirai.message.data.*
//import net.mamoe.mirai.message.data.Image.Key.queryUrl
//import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
//import java.io.File
//import java.nio.file.Files
//import java.nio.file.Path
//import java.util.*
//import kotlin.io.path.absolutePathString
//
//@Listener
//object FileSaver {
//    var autoPick = false
//    var preventRecall = false
//    val storeLocation = if(GlobalConfig.OS.startsWith("Windows")) Path.of(GlobalConfig.HOME, "qq_files") else Path.of(GlobalConfig.HOME, "qq_files")
//    val picStoreLocation = Path.of(storeLocation.absolutePathString(), "pic")
//    val fileStoreLocation = Path.of(storeLocation.absolutePathString(), "file")
//    val audioStoreLocation = Path.of(storeLocation.absolutePathString(), "audio")
//
//    init {
//        if(!Files.exists(storeLocation)) {
//            Files.createDirectory(storeLocation)
//        }
//        if(!Files.exists(picStoreLocation)) {
//            Files.createDirectory(picStoreLocation)
//        }
//        if(!Files.exists(fileStoreLocation)) {
//            Files.createDirectory(fileStoreLocation)
//        }
//        if(!Files.exists(audioStoreLocation)) {
//            Files.createDirectory(audioStoreLocation)
//        }
//    }
//}
//
//@Plugin
//object FileSaver2: PluginRegister {
//
//    override fun register(bot: Bot, channel: EventChannel<BotEvent>) {
//        channel.subscribeAlways<MessageRecallEvent.GroupRecall> {
//            if(preventRecall) {
//                val messageChain = DefaultStore.find(it.messageIds[0])
//                messageChain?.let {mm->
//                    if(mm.contains(FileMessage.Key)) {
//                        return@subscribeAlways
//                    }
//                    it.group.sendMessage(PlainText("${it.authorId}撤回了一条消息↓"))
//                    it.group.sendMessage(mm)
//                }
//            }
//        }
//        channel.subscribeAlways<MessageEvent> { it ->
//            val ids = it.source.ids
//            val message1 = it.message
//            DefaultStore.save(ids[0], message1)
//            val sender = it.sender.id
//            val content = it.message.contentToString()
//            if(autoPick) {
//                for(m in message1) {
//                    if(m is Image) {
//                        val picUrl = m.queryUrl()
//                        try {
//                            NetUtils.downloadTo(picUrl, File(picStoreLocation.absolutePathString(), m.imageId))
//                            RedisStore.set("pic_" + m.imageId, m.imageId)
//                        }catch (e: Exception) {
//                            e.printStackTrace()
//                            return@subscribeAlways
//                        }
//                    }else if(m is PlainText) {
//                        try{
//                            RedisStore.set("plain_" + Date().time.toString(), m.content)
//                        } catch (e: Exception) {
//                            return@subscribeAlways
//                        }
//                    }else if(m is FileMessage) {
//                        if(it.subject is Group) {
//                            val file = m.toAbsoluteFile(it.subject as Group)
//                            file?.let {f->
//                                try {
//                                    NetUtils.downloadTo(f.getUrl()!!, File(fileStoreLocation.absolutePathString(), file.name))
//                                    RedisStore.set("file_" + file.id, file.name)
//                                }catch (e: Exception) {
//                                    e.printStackTrace()
//                                    return@subscribeAlways
//                                }
//                            }
//                        }
//
//                    }
//                }
//            }
//            when{
//                content.startsWith("/fput") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ").filter { it.isNotEmpty() }
//                    if(command.size != 2) {
//                        it.subject.sendMessage("Usage: /put [filename]")
//                        return@subscribeAlways
//                    }
//                    val ids1 = it.message[QuoteReply.Key]!!.source.ids
//                    val find = DefaultStore.find(ids1[0])
//                    if(find == null) {
//                        it.subject.sendMessage("你选中的消息无法找到")
//                        return@subscribeAlways
//                    }
//                    println(find)
//                    find.let {m->
//                        var saved = false
//                        for(ele in m) {
//                            if(ele is Image) {
//                                it.subject.sendMessage("正在保存消息中的图片...")
//                                val picUrl = ele.queryUrl()
//                                try {
//                                    NetUtils.downloadTo(picUrl, File(picStoreLocation.absolutePathString(), "${command[1]}.${ele.imageType.name}"))
//                                    RedisStore.set("pic_" + ele.imageId, "${command[1]}.${ele.imageType.name}")
//                                    saved = true
//                                }catch (e: Exception) {
//                                    it.subject.sendMessage(PlainText("图片保存失败").plus(PlainText("\n").plus(PlainText(e.message?:"未知错误"))))
//                                    return@subscribeAlways
//                                }
//                                it.subject.sendMessage("保存图片成功")
//                            }
//                            if(ele is PlainText) {
//                                it.subject.sendMessage("正在保存消息中的文本...")
//                                try{
//                                    RedisStore.set("plain_" + Date().time.toString(), ele.content)
//                                } catch (e: Exception) {
//                                    it.subject.sendMessage(PlainText("文本保存失败").plus(PlainText("\n").plus(PlainText(e.message?:"未知错误"))))
//                                    return@subscribeAlways
//                                }
//                                it.subject.sendMessage("保存文本成功")
//                            }
//                            if(ele is Audio) {
//
//                            }
//                            if(ele is FileMessage) {
//                                if(it.subject is Group) {
//                                    val file = ele.toAbsoluteFile(it.subject as Group)
//                                    file?.let {f->
//                                        it.subject.sendMessage("正在保存消息中的文件...")
//                                        try {
//                                            NetUtils.downloadTo(f.getUrl()!!, File(fileStoreLocation.absolutePathString(), "${command[1]}.${file.extension}"))
//                                            RedisStore.set("file_" + ele.id, "${command[1]}.${file.extension}")
//                                            saved = true
//                                        }catch (e: Exception) {
//                                            it.subject.sendMessage(PlainText("文件保存失败").plus(PlainText("\n").plus(PlainText(e.message?:"未知错误"))))
//                                            return@subscribeAlways
//                                        }
//                                        it.subject.sendMessage("保存文件成功")
//                                    }
//                                }
//
//                            }
//                        }
//                        if(!saved) {
//                            it.subject.sendMessage("你选中的消息没有可以保存的内容")
//                        }
//                    }
//                }
//                content.startsWith("/fget") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ").filter { it.isNotEmpty() }
//                    if(command.size != 3 && command.size != 5) {
//                        it.subject.sendMessage("Usage: /get [pic|file] [filename] or /get [pic|file] [filename] [width] [height]")
//                        return@subscribeAlways
//                    }
//                    when(command[1]) {
//                        "pic" ->{
//                            if(Files.exists(Path.of(picStoreLocation.absolutePathString(), command[2]))) {
//                                it.subject.sendMessage("正在发送图片...")
//                                try {
//                                    val uploadImage = it.subject.uploadImage(
//                                        File(
//                                            Path.of(picStoreLocation.absolutePathString(), command[2]).absolutePathString()
//                                        )
//                                    )
//                                    if(command.size == 5) {
//                                        val newBuilder = Image.Builder.newBuilder(uploadImage.imageId)
//                                        newBuilder.width = command[3].toInt()
//                                        newBuilder.height = command[4].toInt()
//                                        println("使用这个")
//                                        it.subject.sendMessage(newBuilder.build())
//                                        return@subscribeAlways
//                                    }
//                                    it.subject.sendMessage(uploadImage)
//                                } catch (e: Exception) {
//                                    it.subject.sendMessage(PlainText("文件发送失败").plus(PlainText("\n").plus(PlainText(e.message?:"未知错误"))))
//                                    return@subscribeAlways
//                                }
//                            } else {
//                                it.subject.sendMessage("没有这张图片")
//                            }
//                        }
//                        "file" -> {
//                            if(Files.exists(Path.of(fileStoreLocation.absolutePathString(), command[2]))) {
//                                if(it.subject is Group) {
//                                    it.subject.sendMessage("正在发送文件...")
//                                    try {
//                                        val toExternalResource = File(
//                                            Path.of(fileStoreLocation.absolutePathString(), command[2]).absolutePathString()
//                                        ).toExternalResource()
//                                        (it.subject as Group).files.uploadNewFile(command[2], toExternalResource)
//                                    } catch (e: Exception) {
//                                        it.subject.sendMessage(PlainText("文件发送失败").plus(PlainText("\n").plus(PlainText(e.message?:"未知错误"))))
//                                        return@subscribeAlways
//                                    }
//                                } else {
//                                    it.subject.sendMessage("只支持向群聊发送文件")
//                                }
//                            } else {
//                                it.subject.sendMessage("没有这个文件")
//                            }
//                        }
//                    }
//                }
//                content.startsWith("/fls") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ").filter { it.isNotEmpty() }
//                    if(command.size != 2) {
//                        it.subject.sendMessage("Usage: /ls [pic|file|plain]")
//                        return@subscribeAlways
//                    }
//                    when(command[1]) {
//                        "pic" ->{
//                            val m = RedisStore.getPic().joinToString("\n")
//                            if(m.isEmpty()) {
//                                it.subject.sendMessage("暂无图片")
//                            } else {
//                                it.subject.sendMessage(m)
//                            }
//                        }
//                        "file" -> {
//                            val m = RedisStore.getFile().joinToString("\n")
//                            if(m.isEmpty()) {
//                                it.subject.sendMessage("暂无文件")
//                            } else {
//                                it.subject.sendMessage(m)
//                            }
//                        }
//                        "plain" -> {
//                            val m = RedisStore.getPlain().joinToString("\n")
//                            if(m.isEmpty()) {
//                                it.subject.sendMessage("暂无文本")
//                            } else {
//                                it.subject.sendMessage(m)
//                            }
//                        }
//                    }
//                }
//                content.startsWith("/auto_pick") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ").filter { it.isNotEmpty() }
//                    if(command.size != 2) {
//                        it.subject.sendMessage("Usage: /auto_pick [true|false]")
//                        return@subscribeAlways
//                    }
//                    when(command[1]) {
//                        "true" ->{
//                            autoPick = true
//                            it.subject.sendMessage("已开启自动保存")
//                        }
//                        "false" -> {
//                            autoPick = false
//                            it.subject.sendMessage("已关闭自动保存")
//                        }
//                    }
//                }
//                content.startsWith("/prevent_recall") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ").filter { it.isNotEmpty() }
//                    if(command.size != 2) {
//                        it.subject.sendMessage("Usage: /prevent_recall [true|false]")
//                        return@subscribeAlways
//                    }
//                    when(command[1]) {
//                        "true" ->{
//                            preventRecall = true
//                            it.subject.sendMessage("已开启防撤回")
//                        }
//                        "false" -> {
//                            preventRecall = false
//                            it.subject.sendMessage("已关闭防撤回")
//                        }
//                    }
//                }
//                content.startsWith("/dl") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ").filter { it.isNotEmpty() }
//                    if(command.size != 3) {
//                        it.subject.sendMessage("Usage: /dl [url] [filename]")
//                        return@subscribeAlways
//                    }
//                    it.subject.sendMessage("正在下载文件...")
//                    try {
//                        NetUtils.downloadTo(command[1], File(fileStoreLocation.absolutePathString(), command[2]))
//                        RedisStore.set("file_${command[2]}", command[2])
//                    } catch(e: Exception) {
//                        it.subject.sendMessage(PlainText("文件下载失败").plus(PlainText("\n").plus(PlainText(e.message?:"未知错误"))))
//                        return@subscribeAlways
//                    }
//                    it.subject.sendMessage("文件${command[2]}下载完成")
//                }
//                content.startsWith("/frm") -> {
//                    if(sender != 2326955513) {
//                        it.subject.sendMessage("你没有此项功能的权限")
//                        return@subscribeAlways
//                    }
//                    val command = content.split(" ")
//                    if(command.size != 3) {
//                        it.subject.sendMessage("Usage: /rm [pic|file|plain] [filename]")
//                        return@subscribeAlways
//                    }
//                    when(command[1]) {
//                        "pic" ->{
//                            withContext(Dispatchers.IO) {
//                                Files.delete(Path.of(picStoreLocation.absolutePathString(), command[2]))
//                            }
//                            RedisStore.removePic(command[2])
//                            it.subject.sendMessage("删除图片[${command[2]}]成功")
//                        }
//                        "file" -> {
//                            withContext(Dispatchers.IO) {
//                                Files.delete(Path.of(fileStoreLocation.absolutePathString(), command[2]))
//                            }
//                            RedisStore.removeFile(command[2])
//                            it.subject.sendMessage("删除文件[${command[2]}]成功")
//                        }
//                        "plain" -> {
//                            RedisStore.removePlain(command[2])
//                            it.subject.sendMessage("删除文本[${command[2]}]成功")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}