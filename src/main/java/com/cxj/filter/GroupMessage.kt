package com.cxj.filter

import java.lang.annotation.ElementType

@Target(allowedTargets = [AnnotationTarget.FUNCTION])
annotation class GroupMessage(val filter: Boolean = false, val messageRule: MessageRule = MessageRule.EQUAL, val text: String="", val desc: String="")