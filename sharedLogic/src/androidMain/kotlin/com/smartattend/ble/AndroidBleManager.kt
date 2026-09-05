package com.smartattend.ble

import android.content.Context

/**
 * Main Android BLE controller for SmartAttend.
 *
 * Responsibilities:
 *
 * Teacher:
 * - Broadcast Session ID
 * - Scan Student Cryptographic Keys
 *
 * Student:
 * - Broadcast Cryptographic Key
 *
 * BLE is responsible only for communication.
 * Database validation and attendance processing
 * are handled by the application/backend layer.
 */
class AndroidBleManager(
    private val context: Context
) {

    /*
     * Teacher Session ID advertiser.
     */
    private val advertiser =
        AndroidBleAdvertiser(context)

    /*
     * Teacher scanner for student
     * cryptographic keys.
     */
    private val scanner =
        AndroidBleScanner(context)

    /*
     * Student cryptographic key advertiser.
     */
    private val studentKeyAdvertiser =
        AndroidStudentKeyAdvertiser(context)

    /*
     * Currently active teacher session.
     */
    private var currentSessionId: String? = null

    /**
     * ---------------------------------------------------------
     * TEACHER - SESSION BROADCAST
     * ---------------------------------------------------------
     *
     * Start broadcasting the teacher's
     * current Session ID.
     *
     * Example:
     *
     * BCS701
     */
    fun startTeacherBroadcast(
        sessionId: String
    ) {

        if (sessionId.isBlank()) {

            println(
                "SmartAttend BLE: " +
                        "Session ID is empty"
            )

            return
        }

        currentSessionId =
            sessionId.trim()

        println(
            "SmartAttend BLE: " +
                    "Starting teacher broadcast"
        )

        println(
            "SmartAttend BLE: " +
                    "Session ID = $currentSessionId"
        )

        advertiser.startAdvertising(
            currentSessionId!!
        )
    }

    /**
     * Stop Teacher Session ID broadcasting.
     */
    fun stopTeacherBroadcast() {

        println(
            "SmartAttend BLE: " +
                    "Stopping teacher broadcast"
        )

        advertiser.stopAdvertising()

        currentSessionId = null
    }

    /**
     * ---------------------------------------------------------
     * TEACHER - STUDENT KEY SCANNING
     * ---------------------------------------------------------
     *
     * Start scanning for student
     * cryptographic key advertisements.
     *
     * Every NEW student key detected by BLE
     * is passed to this callback.
     *
     * The application/backend layer can then
     * send the key to the backend for validation.
     *
     * BLE does NOT:
     * - access the database
     * - validate the key
     * - mark attendance
     */
    fun startTeacherScanning(
        onStudentKeyDetected: (String) -> Unit
    ) {

        println(
            "SmartAttend BLE: " +
                    "Starting teacher scanning"
        )

        scanner.startScanning { key ->

            println(
                "SmartAttend BLE: " +
                        "Student key received = $key"
            )

            /*
             * Pass ONLY the cryptographic key
             * to the application/backend layer.
             *
             * BLE processing ends here.
             */
            onStudentKeyDetected(
                key
            )
        }
    }

    /**
     * Stop Teacher scanning for
     * student cryptographic keys.
     */
    fun stopTeacherScanning() {

        println(
            "SmartAttend BLE: " +
                    "Stopping teacher scanning"
        )

        scanner.stopScanning()
    }

    /**
     * ---------------------------------------------------------
     * STUDENT - KEY BROADCAST
     * ---------------------------------------------------------
     *
     * Start broadcasting the student's
     * cryptographic key.
     *
     * Example:
     *
     * 081CS23
     */
    fun startStudentBroadcast(
        cryptographicKey: String
    ) {

        if (cryptographicKey.isBlank()) {

            println(
                "SmartAttend BLE: " +
                        "Cryptographic key is empty"
            )

            return
        }

        val key =
            cryptographicKey.trim()

        println(
            "SmartAttend BLE: " +
                    "Starting student key broadcast"
        )

        println(
            "SmartAttend BLE: " +
                    "Student key = $key"
        )

        studentKeyAdvertiser.startAdvertising(
            key
        )
    }

    /**
     * Stop Student cryptographic
     * key broadcasting.
     */
    fun stopStudentBroadcast() {

        println(
            "SmartAttend BLE: " +
                    "Stopping student key broadcast"
        )

        studentKeyAdvertiser.stopAdvertising()
    }

    /**
     * ---------------------------------------------------------
     * STOP ALL BLE OPERATIONS
     * ---------------------------------------------------------
     *
     * Stops:
     *
     * - Teacher Session ID advertising
     * - Teacher student-key scanning
     * - Student cryptographic-key advertising
     */
    fun stopAll() {

        println(
            "SmartAttend BLE: " +
                    "Stopping all BLE operations"
        )

        advertiser.stopAdvertising()

        scanner.stopScanning()

        studentKeyAdvertiser.stopAdvertising()

        currentSessionId = null

        println(
            "SmartAttend BLE: " +
                    "All BLE operations stopped"
        )
    }

    /**
     * Get the currently active
     * teacher Session ID.
     */
    fun getCurrentSessionId(): String? {

        return currentSessionId
    }
}