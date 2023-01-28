package com.cxj.plugins.vm

import com.cxj.plugins.container.Container
import com.cxj.pojo.ExecResult
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class VMTunnel(
    var tunnelId: String,
    var tunnelUrl: String,
    var status: String
) {
    companion object {
        private val client = OkHttpClient()
        fun getAllTunnels(): ExecResult<List<VMTunnel>> {
            val tokenResult = getToken()
            if(!tokenResult.success) {
                return ExecResult(false, tokenResult.error, null)
            }
            val token = tokenResult.data!!
            val req = Request.Builder()
                .header("Authorization", "Bearer $token")
                .url("http://localhost:9200/api/v1/tunnels")
                .get().build()
            val resp = client.newCall(req).execute()
            if(resp.body == null) {
                return ExecResult(false, "获取frp映射列表失败", null)
            }
            val respText = resp.body!!.charStream().readText()
            println(respText)
            val respObj = Gson().fromJson(respText, JsonRootBean::class.java).data?.items?.map {
                VMTunnel(it.id!!, it.publicUrl!!, it.status!!)
            }
            if(respObj.isNullOrEmpty()) {
                return ExecResult(true, null, listOf())
            }
            return ExecResult(true, null, respObj)
        }
        fun queryTunnel(tunnelId: String): VMTunnel {
            TODO()
        }
        private fun getToken(): ExecResult<String> {
            // 创建端口映射
            val loginReq = Request.Builder()
                .url("http://localhost:9200/api/v1/user/login")
                .post(
                    Gson().toJson(mapOf("email" to "2326955513@qq.com", "password" to "qq.com@2326955513")).toRequestBody("application/json; charset=utf-8".toMediaType())
                ).build()
            val loginResp = client.newCall(loginReq).execute()
            if(loginResp.body == null) {
                return ExecResult(false, "获取frp映射token失败", null)
            }
            val loginRespText = loginResp.body!!.charStream().readText()
            val loginRespObj = Gson().fromJson(loginRespText, HashMap::class.java)
            val token = (loginRespObj["data"] as Map<*, *>)["token"] as String
            return ExecResult(true, null, token)
        }
        suspend fun newTunnel(port: Int, hostname: String): ExecResult<VMTunnel> {
            val tokenResult = getToken()
            if(!tokenResult.success) {
                return ExecResult(false, tokenResult.error, null)
            }
            val token = tokenResult.data!!
            // 创建通道
            val createTunnelReq = Request.Builder()
                .url("http://localhost:9200/api/v1/tunnels")
                .header("Authorization", "Bearer $token")
                .post(
                    Gson().toJson(mapOf(
                        "addr" to port.toString(),
                        "auth" to "",
                        "bind_tls" to "both",
                        "client_cas" to "",
                        "crt" to "",
                        "disable_keep_alives" to "false",
                        "host_header" to "",
                        "hostname" to "",
                        "inspect" to "false",
                        "key" to "",
                        "name" to hostname,
                        "permanent" to true,
                        "proto" to "tcp",
                        "redirect_https" to "false",
                        "region" to "cn_top",
                        "remote_addr" to "",
                        "start_type" to "enable",
                        "subdomain" to "",
                    )).toRequestBody("application/json; charset=utf-8".toMediaType())
                ).build()
            var createTunnelResp = client.newCall(createTunnelReq).execute()
            var createTunnelRespText = createTunnelResp.body!!.charStream().readText()
            var createTunnelRespObj = Gson().fromJson(createTunnelRespText, HashMap::class.java)
            var tunnelId: String? = null
            if (createTunnelRespObj["data"] != null) {
                tunnelId = (createTunnelRespObj["data"] as Map<*, *>)["id"] as String?
            }
            while(createTunnelRespObj["code"] != 20000.0) {
                // 获取所有tunnel,获取到未激活的干掉
                val allTunnels = getAllTunnels()
                if(allTunnels.success) {
                    allTunnels.data!!.filter {
                        it.status == "inactive"
                    }.forEach {
                        it.delete()
                    }
                }
                println("${createTunnelRespObj["code"]}重新发起请求")
                println(createTunnelRespText)
                withContext(Dispatchers.IO) {
                    Thread.sleep(3000)
                }
                createTunnelResp = client.newCall(createTunnelReq).execute()
                createTunnelRespText = createTunnelResp.body!!.charStream().readText()
                println(createTunnelRespText)
                createTunnelRespObj = Gson().fromJson(createTunnelRespText, HashMap::class.java)
                if (createTunnelRespObj["data"] != null) {
                    tunnelId = (createTunnelRespObj["data"] as Map<*, *>)["id"] as String?
                }
            }
            val tunnelUrl = (createTunnelRespObj["data"] as Map<*, *>)["public_url"] as String
            val vmTunnel = VMTunnel(tunnelId!!, tunnelUrl,  (createTunnelRespObj["data"] as Map<*, *>)["status"] as String)
            return ExecResult(true, null, vmTunnel)
        }
    }
    fun delete(): ExecResult<Unit> {
        val tokenResult = getToken()
        if(!tokenResult.success) {
            return ExecResult(false, tokenResult.error, null)
        }
        val token = tokenResult.data!!
        val delTunnelReq = Request.Builder()
            .delete()
            .header("Authorization", "Bearer $token")
            .url("http://localhost:9200/api/v1/tunnels/${tunnelId}")
            .build()
        client.newCall(delTunnelReq).execute()
        return ExecResult(true, null, null)
    }
}