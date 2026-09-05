package com.smartattend.ble

interface BleManager {

    fun startTeacherSession(sessionId: String)

    fun stopTeacherSession()

    fun startStudentScanning()

    fun stopStudentScanning()

    fun startStudentResponse(studentKey: String)

    fun stopStudentResponse()

    fun getState(): BleState
}