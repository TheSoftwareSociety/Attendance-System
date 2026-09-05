package com.smartattend.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import java.nio.charset.StandardCharsets
import java.util.UUID

class AndroidStudentKeyAdvertiser(
    private val context: Context
) {

    companion object {

        /**
         * Separate BLE service for
         * student cryptographic keys.
         */
        val STUDENT_KEY_SERVICE_UUID: UUID =
            UUID.fromString(
                "8b7a0001-7c42-4d91-9a21-123456789abc"
            )
    }

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val advertiser
        get() = bluetoothAdapter?.bluetoothLeAdvertiser

    private var isAdvertising = false

    /**
     * ---------------------------------------------------------
     * ADVERTISE CALLBACK
     * ---------------------------------------------------------
     */
    private val advertiseCallback =
        object : AdvertiseCallback() {

            override fun onStartSuccess(
                settingsInEffect: AdvertiseSettings?
            ) {

                isAdvertising = true

                println(
                    "================================================="
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "STUDENT ADVERTISING STARTED"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Student Service UUID = " +
                            STUDENT_KEY_SERVICE_UUID
                )

                println(
                    "================================================="
                )
            }

            override fun onStartFailure(
                errorCode: Int
            ) {

                isAdvertising = false

                val error =
                    when (errorCode) {

                        ADVERTISE_FAILED_ALREADY_STARTED ->
                            "ALREADY_STARTED"

                        ADVERTISE_FAILED_DATA_TOO_LARGE ->
                            "DATA_TOO_LARGE"

                        ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                            "FEATURE_UNSUPPORTED"

                        ADVERTISE_FAILED_INTERNAL_ERROR ->
                            "INTERNAL_ERROR"

                        ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                            "TOO_MANY_ADVERTISERS"

                        else ->
                            "UNKNOWN_ERROR"
                    }

                println(
                    "================================================="
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "!!! STUDENT ADVERTISING FAILED !!!"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Error code = $errorCode"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Error = $error"
                )

                println(
                    "================================================="
                )
            }
        }

    /**
     * ---------------------------------------------------------
     * START STUDENT ADVERTISING
     * ---------------------------------------------------------
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising(
        cryptographicKey: String
    ) {

        println(
            "SmartAttend BLE DEBUG: " +
                    "Starting student advertising"
        )

        if (!hasAdvertisePermission()) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "BLUETOOTH_ADVERTISE permission missing"
            )

            return
        }

        if (cryptographicKey.isBlank()) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Student key is empty"
            )

            return
        }

        val adapter =
            bluetoothAdapter

        if (adapter == null) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Bluetooth adapter unavailable"
            )

            return
        }

        if (!adapter.isEnabled) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Bluetooth disabled"
            )

            return
        }

        val bleAdvertiser =
            advertiser

        if (bleAdvertiser == null) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "BLE advertiser unavailable"
            )

            return
        }

        /*
         * Stop an existing advertisement.
         */
        if (isAdvertising) {

            stopAdvertising()
        }

        /*
         * -----------------------------------------------------
         * MAIN ADVERTISEMENT
         * -----------------------------------------------------
         *
         * Contains ONLY the Student Service UUID.
         */
        val advertiseData =
            AdvertiseData.Builder()
                .addServiceUuid(
                    ParcelUuid(
                        STUDENT_KEY_SERVICE_UUID
                    )
                )
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

        /*
         * -----------------------------------------------------
         * SCAN RESPONSE
         * -----------------------------------------------------
         *
         * Contains the actual student key.
         */
        val keyData =
            cryptographicKey
                .trim()
                .toByteArray(
                    StandardCharsets.UTF_8
                )

        val scanResponse =
            AdvertiseData.Builder()
                .addServiceData(
                    ParcelUuid(
                        STUDENT_KEY_SERVICE_UUID
                    ),
                    keyData
                )
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

        /*
         * -----------------------------------------------------
         * ADVERTISING SETTINGS
         * -----------------------------------------------------
         */
        val settings =
            AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                )
                .setTxPowerLevel(
                    AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                )
                .setConnectable(false)
                .build()

        println(
            "SmartAttend BLE DEBUG: " +
                    "Student key = $cryptographicKey"
        )

        println(
            "SmartAttend BLE DEBUG: " +
                    "Student key bytes = ${keyData.size}"
        )

        println(
            "SmartAttend BLE DEBUG: " +
                    "Student UUID = " +
                    STUDENT_KEY_SERVICE_UUID
        )

        try {

            bleAdvertiser.startAdvertising(
                settings,
                advertiseData,
                scanResponse,
                advertiseCallback
            )

        } catch (e: SecurityException) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "SecurityException starting student advertising"
            )

            println(
                "SmartAttend BLE DEBUG: " +
                        "Message = ${e.message}"
            )

        } catch (e: Exception) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Exception starting student advertising"
            )

            println(
                "SmartAttend BLE DEBUG: " +
                        "Message = ${e.message}"
            )
        }
    }

    /**
     * ---------------------------------------------------------
     * STOP STUDENT ADVERTISING
     * ---------------------------------------------------------
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {

        try {

            advertiser?.stopAdvertising(
                advertiseCallback
            )

        } catch (e: SecurityException) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "SecurityException stopping student advertising"
            )
        }

        isAdvertising = false

        println(
            "SmartAttend BLE DEBUG: " +
                    "Student advertising STOPPED"
        )
    }

    /**
     * ---------------------------------------------------------
     * PERMISSION
     * ---------------------------------------------------------
     */
    private fun hasAdvertisePermission(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            true
        }
    }
}