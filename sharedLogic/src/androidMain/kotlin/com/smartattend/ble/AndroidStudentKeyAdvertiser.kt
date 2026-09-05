package com.smartattend.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

class AndroidStudentKeyAdvertiser(
    private val context: Context
) {

    companion object {

        /**
         * Separate BLE service for student cryptographic keys.
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

    private var isAdvertising = false

    private val advertiseCallback =
        object : AdvertiseCallback() {

            override fun onStartSuccess(
                settingsInEffect: AdvertiseSettings
            ) {

                println(
                    "SmartAttend BLE: " +
                            "Student key advertising STARTED"
                )

                isAdvertising = true
            }

            override fun onStartFailure(
                errorCode: Int
            ) {

                println(
                    "SmartAttend BLE: " +
                            "Student key advertising FAILED"
                )

                println(
                    "SmartAttend BLE: " +
                            "Error code = $errorCode"
                )

                isAdvertising = false
            }
        }

    @SuppressLint("MissingPermission")
    fun startAdvertising(
        cryptographicKey: String
    ) {

        if (isAdvertising) {

            println(
                "SmartAttend BLE: " +
                        "Student key already advertising"
            )

            return
        }

        if (cryptographicKey.isBlank()) {

            println(
                "SmartAttend BLE: " +
                        "Cryptographic key is empty"
            )

            return
        }

        val adapter = bluetoothAdapter

        if (adapter == null) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth unavailable"
            )

            return
        }

        if (!adapter.isEnabled) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth disabled"
            )

            return
        }

        if (!adapter.isMultipleAdvertisementSupported) {

            println(
                "SmartAttend BLE: " +
                        "BLE advertising not supported"
            )

            return
        }

        val advertiser =
            adapter.bluetoothLeAdvertiser

        if (advertiser == null) {

            println(
                "SmartAttend BLE: " +
                        "BLE advertiser unavailable"
            )

            return
        }

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

        /**
         * The actual service data contains ONLY
         * the student's cryptographic key.
         */
        val serviceData =
            cryptographicKey.toByteArray(
                Charsets.UTF_8
            )

        val advertiseData =
            AdvertiseData.Builder()
                .addServiceData(
                    ParcelUuid(
                        STUDENT_KEY_SERVICE_UUID
                    ),
                    serviceData
                )
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()

        println(
            "SmartAttend BLE: " +
                    "Starting student key advertising"
        )

        println(
            "SmartAttend BLE: " +
                    "Key = $cryptographicKey"
        )

        advertiser.startAdvertising(
            settings,
            advertiseData,
            advertiseCallback
        )
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {

        val advertiser =
            bluetoothAdapter
                ?.bluetoothLeAdvertiser

        if (advertiser != null) {

            try {

                advertiser.stopAdvertising(
                    advertiseCallback
                )

            } catch (e: SecurityException) {

                println(
                    "SmartAttend BLE: " +
                            "Unable to stop student advertising"
                )
            }
        }

        isAdvertising = false

        println(
            "SmartAttend BLE: " +
                    "Student key advertising STOPPED"
        )
    }
}