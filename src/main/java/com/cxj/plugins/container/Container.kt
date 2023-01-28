package com.cxj.plugins.container

import com.cxj.filter.Listener
import com.cxj.filter.Message
import com.cxj.filter.MessageRule
import com.cxj.plugins.Plugin
import com.cxj.plugins.PluginRegister
import com.cxj.plugins.commandExecutor.Processor
import com.google.gson.Gson
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.EventChannel
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

@Listener
object Container {
    private val ports = ConcurrentHashMap<String, Any?>()
    private val client = OkHttpClient()

    @Message(filter = true, messageRule = MessageRule.COMMAND, text = "/create", desc = "创建docker容器")
    suspend fun createContainer(e: MessageEvent) {
        e.message.findIsInstance<At>()?.target?.let {
            e.subject.sendMessage(buildMessageChain {
                + "Hello "
                + At(it)
            })
        }
        val content = e.message.contentToString()
        val subject = e.subject
        val args = content.split(" ").filter(String::isNotEmpty)
        // /create memory:512M cpus:2 port:13333 hostname:centos
        if(args.size != 5) {
            subject.sendMessage("参数不足,格式: /create memory:512M cpus:2 port:13333 hostname:centos")
            return
        }
        if(e.sender.id != 2326955513) {
            subject.sendMessage(MessageChainBuilder().append(At(e.sender.id)).append(PlainText(" MLGB,去你的")).build())
            return
        }
        kotlin.runCatching {
            val argsMap = HashMap<String, Any>()
            for(i in 1 until args.size) {
                val kv = args[i].split(":")
                argsMap[kv[0]] = kv[1]
            }
            for(needArg in listOf("memory", "cpus", "port", "hostname")) {
                if(!argsMap.contains(needArg)) {
                    subject.sendMessage("需要参数:${needArg}")
                    return
                }
            }

            if(ports.contains(argsMap["port"])) {
                subject.sendMessage("端口${argsMap["port"]}已被占用")
                return
            }
            var output: String = ""
            kotlin.runCatching {
                output = Processor.runShellByRuntime("docker run --privileged=false --cpus=${argsMap["cpus"]} -m ${argsMap["memory"]} --memory-swap=${argsMap["memory"]} -dit --name ${argsMap["hostname"]} -p ${argsMap["port"]}:22 -h ${argsMap["hostname"]} mycentos:v", null)
            }.onFailure {createException->
                subject.sendMessage("容器创建失败:${createException.message}")
                return
            }.onSuccess {
//                            subject.sendMessage("容器创建成功:$output")
                subject.sendMessage("容器创建成功")
                kotlin.runCatching {
                    val req = Request.Builder()
                        .url("http://localhost:9200/api/v1/user/login")
                        .post(
                            Gson().toJson(mapOf("email" to "2326955513@qq.com", "password" to "qq.com@2326955513")).toRequestBody("application/json; charset=utf-8".toMediaType())
                        ).build()
                    val resp = client.newCall(req).execute()
                    resp.body?.let {body->
                        val loginData = Gson().fromJson(body.charStream().readText(), HashMap::class.java)
                        val token = (loginData["data"] as Map<*, *>)["token"]
                        println(token)
                        // 创建通道
                        val createTunnelReq = Request.Builder()
                            .url("http://localhost:9200/api/v1/tunnels")
                            .header("Authorization", "Bearer $token")
                            .post(
                                Gson().toJson(mapOf(
                                    "addr" to argsMap["port"],
                                    "auth" to "",
                                    "bind_tls" to "both",
                                    "client_cas" to "",
                                    "crt" to "",
                                    "disable_keep_alives" to "false",
                                    "host_header" to "",
                                    "hostname" to "",
                                    "inspect" to "false",
                                    "key" to "",
                                    "name" to argsMap["hostname"],
                                    "permanent" to true,
                                    "proto" to "tcp",
                                    "redirect_https" to "false",
                                    "region" to "cn_top",
                                    "remote_addr" to "",
                                    "start_type" to "enable",
                                    "subdomain" to "",
                                )).toRequestBody("application/json; charset=utf-8".toMediaType())
                            ).build()
                        var createResp = client.newCall(createTunnelReq).execute()
                        var createRespText = createResp.body!!.charStream().readText()
                        var createTunnelResult = Gson().fromJson(createRespText, HashMap::class.java)
                        while(createTunnelResult == null || createTunnelResult["code"] != 20000.0) {
                            println("${createTunnelResult["code"]}重新发起请求")
                            if(createTunnelResult != null) {
//                                            if()
                            }
                            Thread.sleep(3000)
                            createResp = client.newCall(createTunnelReq).execute()
                            createRespText = createResp.body!!.charStream().readText()
                            println(createRespText)
                            createTunnelResult = Gson().fromJson(createRespText, HashMap::class.java)
                        }
                        var tunnelId: String? = ""
                        kotlin.runCatching {
                            println(createTunnelResult)
                            tunnelId = (createTunnelResult["data"] as Map<*, *>)["id"] as String?
                            val tunnelName = (createTunnelResult["data"] as Map<*, *>)["name"]
                            val tunnelUrl = (createTunnelResult["data"] as Map<*, *>)["public_url"]
                            subject.sendMessage("隧道${tunnelName}已开通,访问url:${tunnelUrl}")
                            ports[argsMap["port"] as String] = createTunnelResult
                        }.onFailure {
                            if(tunnelId != null && tunnelId != "") {
                                val delTunnelReq = Request.Builder()
                                    .delete()
                                    .header("Authorization", "Bearer $token")
                                    .url("http://localhost:9200/api/v1/tunnels/${tunnelId}")
                                    .build()
                                client.newCall(delTunnelReq).execute()
                                subject.sendMessage("${argsMap["hostname"]}的隧道已删除")
                            }
                        }
                    }
                }.onFailure { createFrp->
                    subject.sendMessage("创建frp映射失败:${createFrp.message}")
                    // 删除容器
                    kotlin.runCatching {
                        Processor.runShellByRuntime("docker stop ${argsMap["hostname"]} && docker rm ${argsMap["hostname"]}", null)
                    }.onFailure { delContainerException->
                        subject.sendMessage("容器${argsMap["hostname"]}删除失败:${delContainerException.message}")
                    }.onSuccess {
                        subject.sendMessage("容器${argsMap["hostname"]}删除成功")
                    }
                    return
                }
            }

        }.onFailure {
            subject.sendMessage("命令解析错误")
            it.printStackTrace()
            return
        }
    }
}
