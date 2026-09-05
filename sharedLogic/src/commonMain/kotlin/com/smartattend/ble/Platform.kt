package com.smartattend.ble

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform