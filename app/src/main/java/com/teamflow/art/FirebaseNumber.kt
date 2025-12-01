package com.teamflow.art

fun Any?.asInt(default: Int = 0): Int {
    return when (this) {
        is Long -> this.toInt()
        is Int -> this
        is Double -> this.toInt()
        else -> default
    }
}