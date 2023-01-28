package com.cxj.plugins.vm

import com.cxj.filter.Listener
import com.cxj.filter.Message
import com.cxj.filter.MessageRule
import com.cxj.store.RedisStore
import com.google.gson.Gson
import io.ktor.client.*
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.MessageChainBuilder
import net.mamoe.mirai.message.data.PlainText
import java.io.*
import java.nio.file.Files
import kotlin.io.path.Path

@Listener
object VmManager {
    // 用来辨别端口是否被使用了
    private const val redisPortPrefix = "vm:port:"
    private const val redisVMInfoNamePrefix = "vm:info:name:"
    private const val redisVMInfoIdPrefix = "vm:info:id:"
    // 保存了一个可用的端口集合
    private var ports: MutableSet<Int>
    private const val vmSDir = "E:\\vms"

    init {
        // 生成包含一千个端口的集合
        ports = genSets(13000..14000)
    }

    private fun genSets(range: IntRange): MutableSet<Int> {
        val set = HashSet<Int>()
        for(port in range) {
            set.add(port)
        }
        clearUsedPort(set)
        return set
    }

    // 从redis从查询被使用的端口，然后剔除
    private fun clearUsedPort(ports: Set<Int>) {
        ports.dropWhile {port->
            RedisStore.get("${redisPortPrefix}${port}") != null
        }
    }

    // 随机选一个端口
    private fun pickAPort(): Int {
        return ports.random()
    }

    @Message(filter = true, messageRule = MessageRule.BEGIN_WITH, text = "/new")
    suspend fun newVM(e: MessageEvent) {
        val u = e.sender.id
        val from = e.subject
        val content = e.message.contentToString()
        val args = content.split(" ").filter { arg -> arg.isNotEmpty() }
        val needArgs = listOf("p", "m", "c", "h")
        if(args.size != needArgs.size + 1) {
            from.sendMessage("参数不足: /new p:root m:1024 c:2 h:centos")
            return
        }
        if(u != 2326955513) {
            from.sendMessage(MessageChainBuilder().append(At(u)).append(PlainText(" 你谁啊？命令我")).build())
            return
        }
        val argsMap = mutableMapOf<String, String>()
        kotlin.runCatching {
            args.drop(1).forEach { arg->
                val keyAndValue = arg.split(":")
                argsMap[keyAndValue[0]] = keyAndValue[1]
            }
        }.onFailure { e1->
            e1.printStackTrace()
            from.sendMessage("参数解析出错,需要用冒号来分割键值对")
            return
        }
        for(needArg in needArgs) {
            if(!argsMap.contains(needArg)) {
                from.sendMessage("需要填写参数${needArg}")
                return
            }
        }
        // 选一个端口
        val port = pickAPort()
        // 存入
        argsMap["port"] = port.toString()
        val hostname = argsMap["h"]
        val vmDir = Path(vmSDir, hostname!!)

        if(Files.exists(vmDir)) {
            from.sendMessage("虚拟机${hostname}已存在")
            return
        }
        val execResult = VM.new(hostname, vmDir, argsMap) {
            from.sendMessage("正在创建虚拟机${hostname},这可能需要几分钟")
        }
        if(!execResult.success) {
            from.sendMessage(execResult.error!!)
            return
        }
        from.sendMessage("虚拟机${hostname}创建成功")
        val vm = execResult.data!!
        kotlin.runCatching {
            val createTunnelResult = VMTunnel.newTunnel(port, hostname)
            if(!createTunnelResult.success) {
                from.sendMessage(createTunnelResult.error!!)
                return
            }
            val tunnel = createTunnelResult.data!!
            vm.vmTunnel = tunnel
            vm.vmPort = port.toString()
            from.sendMessage("frp映射为: ${tunnel.tunnelUrl}")
            RedisStore.set("${redisPortPrefix}${port}", "true")
            RedisStore.set("${redisVMInfoNamePrefix}${hostname}", Gson().toJson(vm))
            RedisStore.set("${redisVMInfoIdPrefix}${vm.vmId}", Gson().toJson(vm))
            ports.drop(port)
        }.onFailure {
            from.sendMessage("映射创建失败,正在清理虚拟机资源")
            vm.delete()
            RedisStore.del("${redisPortPrefix}${port}")
            RedisStore.del("${redisVMInfoNamePrefix}${hostname}")
            RedisStore.del("${redisVMInfoIdPrefix}${hostname}")
            ports.add(port)
            from.sendMessage("虚拟机资源清理完成")
        }
    }

    @Message(filter = true, messageRule = MessageRule.EQUAL, text = "/vms")
    suspend fun lsVm(e: MessageEvent) {
        val u = e.sender.id
        val from = e.subject
        e.message.contentToString()
        if(u != 2326955513) {
            from.sendMessage(MessageChainBuilder().append(At(u)).append(PlainText(" 你谁啊？命令我")).build())
            return
        }
        val vmInfo = VM.getVMSInfo()
        if(!vmInfo.success) {
            from.sendMessage(vmInfo.error!!)
            return
        }
        var res = vmInfo.data!!.map {
            // 拿不到id的
            val s = VM.getVMInfoFromVagrantById(it.vmId)
            "名称: ${it.vmName}\n" +
                    "运行状态: ${s.data!!.vmStatus}\n" +
                    "虚拟机位置: ${it.vmLocation}\n" +
                    "虚拟机frp: ${it.vmTunnel!!.tunnelUrl}"
        }.joinToString("\n\n")
        res = "所有虚拟机信息:\n$res"
        // 管道
        from.sendMessage(res)
    }

    @Message(filter = true, messageRule = MessageRule.BEGIN_WITH, text = "/rmvm")
    suspend fun rmVm(e: MessageEvent) {
        val u = e.sender.id
        val from = e.subject
        val content = e.message.contentToString()
        val args = content.split(" ").filter { arg -> arg.isNotEmpty() }
        val needArgs = listOf("n")
        if(args.size != needArgs.size + 1) {
            from.sendMessage("参数不足: /rmvm n:centos")
            return
        }
        if(u != 2326955513) {
            from.sendMessage(MessageChainBuilder().append(At(u)).append(PlainText(" 你谁啊？命令我")).build())
            return
        }
        val argsMap = mutableMapOf<String, String>()
        kotlin.runCatching {
            args.drop(1).forEach { arg->
                val keyAndValue = arg.split(":")
                argsMap[keyAndValue[0]] = keyAndValue[1]
            }
        }.onFailure { e1->
            e1.printStackTrace()
            from.sendMessage("参数解析出错,需要用冒号来分割键值对")
            return
        }
        for(needArg in needArgs) {
            if(!argsMap.contains(needArg)) {
                from.sendMessage("需要填写参数${needArg}")
                return
            }
        }
        val vmInfo = VM.getVMInfo(argsMap["n"] as String)
        if(!vmInfo.success) {
            from.sendMessage("没有名为${argsMap["n"]}的虚拟机")
            return
        }
        from.sendMessage("正在删除名为${argsMap["n"]}的虚拟机")
        val vm = vmInfo.data!!
        val delRes = vm.delete()
        if(!delRes.success) {
            from.sendMessage("删除虚拟机${argsMap["n"]}失败")
        } else {
            from.sendMessage("删除虚拟机${argsMap["n"]}成功")
        }
    }

    @Message(filter = true, messageRule = MessageRule.BEGIN_WITH, text = "/stopvm")
    suspend fun stopVm(e: MessageEvent) {
        val u = e.sender.id
        val from = e.subject
        val content = e.message.contentToString()
        val args = content.split(" ").filter { arg -> arg.isNotEmpty() }
        val needArgs = listOf("n")
        if(args.size != needArgs.size + 1) {
            from.sendMessage("参数不足: /stopvm n:centos")
            return
        }
        if(u != 2326955513) {
            from.sendMessage(MessageChainBuilder().append(At(u)).append(PlainText(" 你谁啊？命令我")).build())
            return
        }
        val argsMap = mutableMapOf<String, String>()
        kotlin.runCatching {
            args.drop(1).forEach { arg->
                val keyAndValue = arg.split(":")
                argsMap[keyAndValue[0]] = keyAndValue[1]
            }
        }.onFailure { e1->
            e1.printStackTrace()
            from.sendMessage("参数解析出错,需要用冒号来分割键值对")
            return
        }
        for(needArg in needArgs) {
            if(!argsMap.contains(needArg)) {
                from.sendMessage("需要填写参数${needArg}")
                return
            }
        }
        val vmInfo = VM.getVMInfo(argsMap["n"] as String)
        if(!vmInfo.success) {
            from.sendMessage("没有名为${argsMap["n"]}的虚拟机")
            return
        }

        from.sendMessage("正在关闭名为${argsMap["n"]}的虚拟机")
        val vm = vmInfo.data!!
        val delRes = vm.close()
        if(!delRes.success) {
            from.sendMessage(delRes.error!!)
        } else {
            from.sendMessage("关闭虚拟机${argsMap["n"]}成功")
        }
    }

    @Message(filter = true, messageRule = MessageRule.BEGIN_WITH, text = "/openvm")
    suspend fun openVm(e: MessageEvent) {
        val u = e.sender.id
        val from = e.subject
        val content = e.message.contentToString()
        val args = content.split(" ").filter { arg -> arg.isNotEmpty() }
        val needArgs = listOf("n")
        if(args.size != needArgs.size + 1) {
            from.sendMessage("参数不足: /openvm n:centos")
            return
        }
        if(u != 2326955513) {
            from.sendMessage(MessageChainBuilder().append(At(u)).append(PlainText(" 你谁啊？命令我")).build())
            return
        }
        val argsMap = mutableMapOf<String, String>()
        kotlin.runCatching {
            args.drop(1).forEach { arg->
                val keyAndValue = arg.split(":")
                argsMap[keyAndValue[0]] = keyAndValue[1]
            }
        }.onFailure { e1->
            e1.printStackTrace()
            from.sendMessage("参数解析出错,需要用冒号来分割键值对")
            return
        }
        for(needArg in needArgs) {
            if(!argsMap.contains(needArg)) {
                from.sendMessage("需要填写参数${needArg}")
                return
            }
        }
        val vmInfo = VM.getVMInfo(argsMap["n"] as String)
        if(!vmInfo.success) {
            from.sendMessage("没有名为${argsMap["n"]}的虚拟机")
            return
        }

        from.sendMessage("正在启动名为${argsMap["n"]}的虚拟机")
        val vm = vmInfo.data!!
        val delRes = vm.open()
        if(!delRes.success) {
            from.sendMessage(delRes.error!!)
        } else {
            from.sendMessage("启动虚拟机${argsMap["n"]}成功")
        }
    }
}



