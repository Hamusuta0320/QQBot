package com.cxj.plugins.protocol.addFriend

import kotlin.reflect.KClass

internal interface ProtocolStruct
internal interface ProtoBuf : ProtocolStruct
internal interface JceStruct : ProtocolStruct

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
internal annotation class NestedStructure(
    val serializer: KClass<out NestedStructureDesensitizer<*, *>>
)

internal interface NestedStructureDesensitizer<in C : ProtocolStruct, T> {
    fun deserialize(context: C, byteArray: ByteArray): T?
}