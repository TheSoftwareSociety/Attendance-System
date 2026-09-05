package com.smartattend.ble

sealed class BlePacket {

    data class Session(
        val sessionId: String
    ) : BlePacket()

    data class Student(
        val studentKey: String
    ) : BlePacket()
}