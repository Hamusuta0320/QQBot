package com.cxj.filter


@Target(allowedTargets = [AnnotationTarget.FUNCTION])
@Retention(AnnotationRetention.RUNTIME)
annotation class UserMessage(val filter: Boolean = false, val messageRule: MessageRule = MessageRule.EQUAL, val text: String="", val desc: String="")
