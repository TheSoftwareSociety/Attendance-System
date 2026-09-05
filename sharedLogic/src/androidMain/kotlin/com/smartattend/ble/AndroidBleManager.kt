package com.smartattend.ble

import android.content.Context

/**
 * Main Android BLE controller for SmartAttend.
 *
 * Responsibilities:
 * - Start/stop teacher session broadcasting
 * - Start/stop student scanning
 * - Start/stop student cryptographic ID broadcasting
 * - Receive detected session IDs and RSSI
 *
 * BLE implementation is delegated to:
 * - AndroidBleAdvertiser
 * - AndroidBleScanner
 */
class AndroidBleManager(
    private val context: Context
) {

    private val advertiser =
        AndroidBleAdvertiser(context)

    private val scanner =
        AndroidBleScanner(context)

    private var currentSessionId: String? = null

    /**
     * ================================
     * TEACHER BROADCAST
     * ================================
     *
     * Teacher broadcasts the Session ID.
     *
     * Example:
     * BCS701
     */
    fun startTeacherBroadcast(
        sessionId: String
    ) {

        if (sessionId.isBlank()) {
            println(
                "SmartAttend BLE: Session ID is empty"
            )
            return
        }

        currentSessionId = sessionId

        println(
            "SmartAttend BLE: Starting teacher broadcast"
        )

        println(
            "SmartAttend BLE: Session ID = $sessionId"
        )

        advertiser.startAdvertising(
            sessionId
        )
    }

    /**
     * Stop Teacher BLE broadcasting.
     */
    fun stopTeacherBroadcast() {

        println(
            "SmartAttend BLE: Stopping teacher broadcast"
        )

        advertiser.stopAdvertising()

        currentSessionId = null
    }

    /**
     * ================================
     * STUDENT SCANNING
     * ================================
     *
     * Student searches for the teacher's
     * SmartAttend BLE session.
     *
     * Returns:
     * - Session ID
     * - RSSI
     */
    fun startStudentScanning(
        onSessionDetected: (String, Int) -> Unit
    ) {

        println(
            "SmartAttend BLE: Starting student scanning"
        )

        scanner.startScanning { sessionId, rssi ->

            println(
                "SmartAttend BLE: " +
                        "Session detected = $sessionId"
            )

            println(
                "SmartAttend BLE: " +
                        "RSSI = $rssi dBm"
            )

            currentSessionId = sessionId

            onSessionDetected(
                sessionId,
                rssi
            )
        }
    }

    /**
     * Stop Student BLE scanning.
     */
    fun stopStudentScanning() {

        println(
            "SmartAttend BLE: Stopping student scanning"
        )

        scanner.stopScanning()

        currentSessionId = null
    }

    /**
     * ================================
     * STUDENT BROADCAST
     * ================================
     *
     * Student broadcasts only the
     * cryptographic ID.
     */
    fun startStudentBroadcast(
        cryptographicId: String
    ) {

        if (cryptographicId.isBlank()) {
            println(
                "SmartAttend BLE: Cryptographic ID is empty"
            )
            return
        }

        println(
            "SmartAttend BLE: Starting student broadcast"
        )

        println(
            "SmartAttend BLE: Cryptographic ID = $cryptographicId"
        )

        advertiser.startAdvertising(
            cryptographicId
        )
    }

    /**
     * Stop Student BLE broadcasting.
     */
    fun stopStudentBroadcast() {

        println(
            "SmartAttend BLE: Stopping student broadcast"
        )

        advertiser.stopAdvertising()
    }

    /**
     * Get the currently detected/active
     * session ID.
     */
    fun getCurrentSessionId(): String? {
        return currentSessionId
    }

    /**
     * ================================
     * STOP EVERYTHING
     * ================================
     */
    fun stopAll() {

        advertiser.stopAdvertising()
        scanner.stopScanning()

        currentSessionId = null

        println(
            "SmartAttend BLE: All BLE operations stopped"
        )
    }
}