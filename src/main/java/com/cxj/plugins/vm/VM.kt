package com.cxj.plugins.vm

import com.cxj.plugins.commandExecutor.Processor
import com.cxj.pojo.ExecResult
import com.cxj.store.RedisStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class VM(
    var vmId: String = "",
    var vmStatus: String = "",
    var vmLocation: String = "",
    var vmTunnel: VMTunnel? = null,
    var vmDir: String = "",
    var vmName: String = "",
    var vmPort: String = ""
) {

    fun delete(): ExecResult<Unit> {
        var deleteExecResult: ExecResult<Unit>
        kotlin.runCatching {
            // 删除redis
            RedisStore.del("${redisVMInfoNamePrefix}${vmName}")
            RedisStore.del("${redisVMInfoIdPrefix}${vmId}")
            RedisStore.del("${redisPortPrefix}${vmPort}")
            // 删除虚拟机 vagrant destroy
            Processor.runShellByRuntime("echo Y|vagrant destroy", vmDir)
            // 删除虚拟机文件夹 rm
            deleteAll(Path(vmDir))
            // 删除tunnel
            vmTunnel?.delete()?.let {
                if(!it.success) {
                    throw Exception("隧道没有删除成功")
                }
            }
        }.onFailure {
            it.printStackTrace()
            deleteExecResult = ExecResult(false, "删除失败", null)
        }
        deleteExecResult = ExecResult(true, null, null)
        return deleteExecResult
    }
    // 开机
    fun open(): ExecResult<Unit> {
        var execResult: ExecResult<Unit>
        // 进入目录, vagrant up
        kotlin.runCatching {
            Processor.runShellByRuntime("vagrant up", vmDir)
        }.onFailure {
            execResult = ExecResult(false, "${vmName}开机失败", null)
        }
        execResult = ExecResult(true, "${vmName}已开机", null)
        return execResult
    }
    // 关机
    fun close(): ExecResult<Unit> {
        var execResult: ExecResult<Unit>
        // 进入目录, vagrant up
        kotlin.runCatching {
            Processor.runShellByRuntime("vagrant halt", vmDir)
        }.onFailure {
            execResult = ExecResult(false, "${vmName}关机失败", null)
        }
        execResult = ExecResult(true, "${vmName}已关机", null)
        return execResult
    }
    companion object {
        fun deleteAll(p: Path) {
            p.toFile().listFiles()?.forEach {
                if(it.isFile) {
                    it.delete()
                } else {
                    deleteAll(it.toPath())
                }
            }
            Files.delete(p)
        }
        private const val redisVMInfoNamePrefix = "vm:info:name:"
        private const val redisVMInfoIdPrefix = "vm:info:id:"
        private const val redisPortPrefix = "vm:port:"
        fun getVMByNameFromRedis(name: String): ExecResult<VM> {
            val get = RedisStore.get("${redisVMInfoNamePrefix}${name}")
            return if(get == null) {
                ExecResult(false, "找不到对应的虚拟机", null)
            } else {
                val vm = Gson().fromJson(get, VM::class.java)
                ExecResult(true, null, vm)
            }
        }
        fun getVMByIdFromRedis(id: String): ExecResult<VM> {
            val get = RedisStore.get("${redisVMInfoIdPrefix}${id}")
            return if(get == null) {
                ExecResult(false, "找不到对应的虚拟机", null)
            } else {
                val vm = Gson().fromJson(get, VM::class.java)
                ExecResult(true, null, vm)
            }
        }
        fun getVMSInfo(): ExecResult<List<VM>> {
            var result: ExecResult<List<VM>> = ExecResult(false, "没有查询到虚拟机信息", null)
            RedisStore.useClient { jedis ->
                val resultList = jedis.keys("${redisVMInfoIdPrefix}*").map {
                    Gson().fromJson(jedis.get(it), VM::class.java)
                }.toList()
                result = if(resultList.isEmpty()) {
                    ExecResult(false, "没有查询到虚拟机信息", null)
                } else {
                    ExecResult(true, null, resultList)
                }
            }
            return result
        }
        fun getVMInfo(name: String): ExecResult<VM> {
            val vmsInfo = getVMSInfo()
            if(!vmsInfo.success) {
                return ExecResult(false, "没有查询到虚拟机信息", null)
            }
            val vm = vmsInfo.data!!.filter { it.vmName == name }
            return if(vm.isEmpty()) {
                ExecResult(false, "没有查询到虚拟机信息", null)
            } else {
                ExecResult(true, null, vm.first())
            }
        }
        fun getVMSInfoFromVagrant(): List<VM>? {
            var output = ""
            var result: List<VM>? = null
            kotlin.runCatching {
                output = Processor.runShellByRuntime("vagrant global-status", null)
            }.onFailure {
                it.printStackTrace()
            }.onSuccess {
                val reg = "(\\w{7}).*virtualbox *(\\w+) *(\\S+)"
                result = Regex(reg).findAll(output).map {
                    VM(it.groupValues[1], it.groupValues[2], it.groupValues[3])
                }.toList()
            }
            return result
        }
        fun getVMInfoFromVagrantById(id: String): ExecResult<VM> {
            var output = ""
            var result: ExecResult<VM> = ExecResult(false, "获取虚拟机信息失败", null)
            kotlin.runCatching {
                output = Processor.runShellByRuntime("vagrant global-status", null)
            }.onFailure {
                it.printStackTrace()
                result = ExecResult(false, "获取虚拟机信息失败", null)
            }.onSuccess {
                val reg = "(\\w{7}).*virtualbox *(\\w+) *(\\S+)"
                val temp = Regex(reg).findAll(output).map {
                    VM(it.groupValues[1], it.groupValues[2], it.groupValues[3])
                }.toList()
                val findRes = temp.filter { it.vmId == id }
                if(findRes.isEmpty()) {
                    result = ExecResult(false, "获取虚拟机信息失败", null)
                    return@onSuccess
                }
                result = ExecResult(true, null, findRes.first())
            }
            return result
        }
        private fun getVMInfoFromVagrant(vmDir: String): ExecResult<VM> {
            var output = ""
            var result: ExecResult<VM> = ExecResult(false, "获取虚拟机信息失败", null)
            kotlin.runCatching {
                output = Processor.runShellByRuntime("vagrant global-status", null)
            }.onFailure {
                it.printStackTrace()
                result = ExecResult(false, "获取虚拟机信息失败", null)
            }.onSuccess {
                val reg = "(\\w{7}).*virtualbox *(\\w+) *(\\S+)"
                val temp = Regex(reg).findAll(output).map {
                    VM(it.groupValues[1], it.groupValues[2], it.groupValues[3])
                }.toList()
                val findRes = temp.filter { Path(it.vmLocation).toString() == vmDir }

                result = ExecResult(true, null, findRes.first())
            }
            return result
        }
        suspend fun new(hostname: String, vmDir: Path, argsMap: MutableMap<String, String>, onCreating: suspend ()->Unit): ExecResult<VM> {
            // 创建虚拟机文件夹
            withContext(Dispatchers.IO) {
                Files.createDirectory(vmDir)
            }
            val templateInputStream = VmManager.javaClass.classLoader.getResourceAsStream("centos.vm.Vagrantfile")
                ?: return ExecResult(false, "无法找到模板文件", null)
            // 读取模板文件
            val br = BufferedReader(InputStreamReader(templateInputStream))
            var templateStr = br.readText()
            // 替换
            for (p in argsMap) {
                templateStr = templateStr.replace("{{${p.key}}}", p.value)
            }
            // 写入
            val bw =
                BufferedWriter(OutputStreamWriter(Path(vmDir.toString(),"Vagrantfile").toFile().outputStream()))
            withContext(Dispatchers.IO) {
                bw.write(templateStr)
                bw.flush()
                bw.close()
            }

            // 执行vagrant up
            var output = ""
            onCreating()
            var execResult: ExecResult<VM>? = null
            kotlin.runCatching {
                output = Processor.runShellByRuntime("vagrant up", vmDir.toString())
            }.onFailure { createVMException->
                println(createVMException.message)
                execResult = ExecResult(false, "创建虚拟机失败", null)
            }.onSuccess {
                // 需要为VM设置信息
                val vmInfoResult = getVMInfoFromVagrant(vmDir.toString())
                execResult = if(!vmInfoResult.success) {
                    ExecResult(false, "创建虚拟机成功,但获取虚拟机信息失败", null)
                } else {
                    val vmInfo = vmInfoResult.data!!
                    vmInfo.vmName = hostname
                    vmInfo.vmDir = vmDir.toString()
                    ExecResult(true, null, vmInfo)
                }
            }
            return execResult!!
        }
    }
}

