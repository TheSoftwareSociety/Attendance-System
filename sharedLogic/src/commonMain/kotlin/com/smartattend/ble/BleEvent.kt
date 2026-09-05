package com.smartattend.ble

sealed class BleEvent {

    data class SessionDetected(
        val sessionId: String,
        val rssi: Int
    ) : BleEvent()

    data class StudentDetected(
        val studentKey: String,
        val rssi: Int
    ) : BleEvent()

    data class Error(
        val message: String
    ) : BleEvent()

    data object BluetoothReady : BleEvent()

    data object BluetoothUnavailable : BleEvent()
}