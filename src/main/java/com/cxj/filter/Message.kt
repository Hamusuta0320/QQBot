package com.cxj.filter

@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class Message(val filter: Boolean = false, val messageRule: MessageRule = MessageRule.EQUAL, val text: String="", val desc: String="")
