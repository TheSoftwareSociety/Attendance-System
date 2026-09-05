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
import android.os.ParcelUuid
import java.nio.charset.StandardCharsets

class AndroidBleAdvertiser(
    private val context: Context
) {

    companion object {

        /**
         * SmartAttend fixed BLE Service UUID.
         *
         * Teacher and Student use the same UUID.
         */
        val SERVICE_UUID =
            java.util.UUID.fromString(
                "7b3c0001-8f2a-4c91-a8d2-123456789abc"
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

    private var currentSessionId: String? = null

    /**
     * BLE advertising callback.
     */
    private val advertiseCallback =
        object : AdvertiseCallback() {

            override fun onStartSuccess(
                settingsInEffect: AdvertiseSettings?
            ) {

                println(
                    "SmartAttend BLE: Advertising STARTED"
                )

                println(
                    "SmartAttend BLE: " +
                            "Session = $currentSessionId"
                )
            }

            override fun onStartFailure(
                errorCode: Int
            ) {

                val errorMessage =
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
                    "SmartAttend BLE: " +
                            "Advertising FAILED"
                )

                println(
                    "SmartAttend BLE: " +
                            "Error code = $errorCode"
                )

                println(
                    "SmartAttend BLE: " +
                            "Error = $errorMessage"
                )
            }
        }

    /**
     * Start SmartAttend BLE advertising.
     *
     * The Service UUID is placed in the main
     * advertisement.
     *
     * The Session ID is placed in the
     * scan response.
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising(
        sessionId: String
    ) {

        if (!hasAdvertisePermission()) {

            println(
                "SmartAttend BLE: " +
                        "BLUETOOTH_ADVERTISE permission missing"
            )

            return
        }

        if (sessionId.isBlank()) {

            println(
                "SmartAttend BLE: " +
                        "Session ID is empty"
            )

            return
        }

        val adapter = bluetoothAdapter

        if (adapter == null) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth adapter unavailable"
            )

            return
        }

        if (!adapter.isEnabled) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth is disabled"
            )

            return
        }

        val bleAdvertiser = advertiser

        if (bleAdvertiser == null) {

            println(
                "SmartAttend BLE: " +
                        "BLE advertising not supported"
            )

            return
        }

        // Stop previous advertisement.
        stopAdvertising()

        currentSessionId = sessionId

        /*
         * MAIN ADVERTISEMENT
         *
         * Contains only the SmartAttend Service UUID.
         *
         * This keeps the main BLE packet small.
         */
        val advertiseData =
            AdvertiseData.Builder()
                .addServiceUuid(
                    ParcelUuid(SERVICE_UUID)
                )
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

        /*
         * SCAN RESPONSE
         *
         * Contains the actual Session ID.
         *
         * Example:
         *
         * BCS701
         */
        val sessionData =
            sessionId.toByteArray(
                StandardCharsets.UTF_8
            )

        val scanResponse =
            AdvertiseData.Builder()
                .addServiceData(
                    ParcelUuid(SERVICE_UUID),
                    sessionData
                )
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

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
            "SmartAttend BLE: " +
                    "Starting advertising"
        )

        println(
            "SmartAttend BLE: " +
                    "Session ID = $sessionId"
        )

        bleAdvertiser.startAdvertising(
            settings,
            advertiseData,
            scanResponse,
            advertiseCallback
        )
    }

    /**
     * Stop BLE advertising.
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {

        if (!hasAdvertisePermission()) {
            return
        }

        try {

            advertiser?.stopAdvertising(
                advertiseCallback
            )

        } catch (e: SecurityException) {

            println(
                "SmartAttend BLE: " +
                        "Unable to stop advertising"
            )
        }

        currentSessionId = null

        println(
            "SmartAttend BLE: " +
                    "Advertising STOPPED"
        )
    }

    /**
     * Check advertising permission.
     */
    private fun hasAdvertisePermission(): Boolean {

        return if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S
        ) {

            context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            true
        }
    }
}