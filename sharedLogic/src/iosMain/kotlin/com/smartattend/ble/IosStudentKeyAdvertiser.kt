package com.smartattend.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreBluetooth.CBAdvertisementDataServiceUUIDsKey
import platform.CoreBluetooth.CBAttributePermissionsReadable
import platform.CoreBluetooth.CBCharacteristicPropertyRead
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBMutableCharacteristic
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSObject

/**
 * SmartAttend iOS Student BLE Advertiser.
 *
 * Responsibility:
 *
 * Student:
 * - Advertise SmartAttend BLE service UUID
 * - Expose cryptographic key through
 *   a readable BLE characteristic
 *
 * Backend validation is NOT handled here.
 */
@OptIn(ExperimentalForeignApi::class)
class IosStudentKeyAdvertiser {

    companion object {

        /**
         * SAME service UUID used by Android.
         */
        const val STUDENT_KEY_SERVICE_UUID =
            "8B7A0001-7C42-4D91-9A21-123456789ABC"

        /**
         * Characteristic containing
         * the student's cryptographic key.
         */
        const val STUDENT_KEY_CHARACTERISTIC_UUID =
            "8B7A0002-7C42-4D91-9A21-123456789ABC"
    }

    /**
     * SmartAttend student service.
     */
    private val serviceUuid =
        CBUUID.UUIDWithString(
            STUDENT_KEY_SERVICE_UUID
        )

    /**
     * Student key characteristic.
     */
    private val characteristicUuid =
        CBUUID.UUIDWithString(
            STUDENT_KEY_CHARACTERISTIC_UUID
        )

    /**
     * iOS peripheral manager.
     */
    private var peripheralManager:
            CBPeripheralManager? = null

    /**
     * Current student cryptographic key.
     */
    private var currentKey:
            String? = null

    /**
     * Current advertising state.
     */
    private var isAdvertising =
        false

    /**
     * Keep a strong reference to the
     * characteristic.
     */
    private var keyCharacteristic:
            CBMutableCharacteristic? = null

    /**
     * CoreBluetooth peripheral delegate.
     */
    private val delegate =
        object : NSObject(),
            CBPeripheralManagerDelegateProtocol {

            /**
             * Called whenever the Bluetooth
             * peripheral manager state changes.
             */
            override fun peripheralManagerDidUpdateState(
                peripheral: CBPeripheralManager
            ) {

                when (peripheral.state) {

                    CBManagerStatePoweredOn -> {

                        println(
                            "SmartAttend BLE: " +
                                    "iOS Bluetooth powered ON"
                        )

                        startAdvertisingInternal()
                    }

                    CBManagerStatePoweredOff -> {

                        println(
                            "SmartAttend BLE: " +
                                    "iOS Bluetooth powered OFF"
                        )

                        isAdvertising = false
                    }

                    else -> {

                        println(
                            "SmartAttend BLE: " +
                                    "iOS Bluetooth unavailable"
                        )

                        isAdvertising = false
                    }
                }
            }
        }

    /**
     * Start student BLE advertising.
     *
     * The cryptographic key is exposed
     * through the GATT characteristic.
     */
    fun startAdvertising(
        cryptographicKey: String
    ) {

        if (cryptographicKey.isBlank()) {

            println(
                "SmartAttend BLE: " +
                        "Cryptographic key is empty"
            )

            return
        }

        if (isAdvertising) {

            println(
                "SmartAttend BLE: " +
                        "Student key already advertising"
            )

            return
        }

        currentKey =
            cryptographicKey.trim()

        println(
            "SmartAttend BLE: " +
                    "Starting iOS student key advertising"
        )

        println(
            "SmartAttend BLE: " +
                    "Key = $currentKey"
        )

        /*
         * Create the peripheral manager.
         *
         * The delegate will receive
         * peripheralManagerDidUpdateState().
         */
        if (peripheralManager == null) {

            peripheralManager =
                CBPeripheralManager(
                    delegate = delegate,
                    queue = null
                )

        } else {

            startAdvertisingInternal()
        }
    }

    /**
     * Build the GATT service and
     * start BLE advertising.
     */
    private fun startAdvertisingInternal() {

        val manager =
            peripheralManager
                ?: return

        val key =
            currentKey
                ?: return

        /*
         * Bluetooth must be powered on.
         */
        if (
            manager.state !=
            CBManagerStatePoweredOn
        ) {

            println(
                "SmartAttend BLE: " +
                        "iOS Bluetooth is not ready"
            )

            return
        }

        /*
         * Stop an existing advertisement.
         */
        manager.stopAdvertising()

        /*
         * Remove previous services.
         */
        manager.removeAllServices()

        /*
         * Convert the student key
         * into NSData.
         */
        val keyBytes =
            key.encodeToByteArray()

        val keyData: NSData =
            keyBytes.usePinned { pinned ->

                NSData.create(
                    bytes =
                        pinned.addressOf(0),

                    length =
                        keyBytes.size.toULong()
                )
            }

        /*
         * Create readable characteristic.
         *
         * The key is stored as the
         * characteristic value.
         */
        val characteristic =
            CBMutableCharacteristic(
                type =
                    characteristicUuid,

                properties =
                    CBCharacteristicPropertyRead,

                value =
                    keyData,

                permissions =
                    CBAttributePermissionsReadable
            )

        keyCharacteristic =
            characteristic

        /*
         * Create SmartAttend service.
         */
        val service =
            CBMutableService(
                type =
                    serviceUuid,

                primary =
                    true
            )

        /*
         * Attach the key characteristic.
         */
        service.setCharacteristics(
            listOf(characteristic)
        )

        /*
         * Add the service to the
         * local GATT database.
         */
        manager.addService(
            service
        )

        /*
         * Advertise ONLY the SmartAttend
         * service UUID.
         *
         * The actual key remains inside
         * the GATT characteristic.
         */
        val advertisementData =
            mapOf<Any?, Any?>(
                CBAdvertisementDataServiceUUIDsKey
                        to listOf(serviceUuid)
            )

        manager.startAdvertising(
            advertisementData
        )

        isAdvertising =
            true

        println(
            "SmartAttend BLE: " +
                    "iOS student key advertising STARTED"
        )

        println(
            "SmartAttend BLE: " +
                    "Service UUID = " +
                    STUDENT_KEY_SERVICE_UUID
        )

        println(
            "SmartAttend BLE: " +
                    "Characteristic UUID = " +
                    STUDENT_KEY_CHARACTERISTIC_UUID
        )
    }

    /**
     * Stop student BLE advertising.
     */
    fun stopAdvertising() {

        peripheralManager?.stopAdvertising()

        peripheralManager?.removeAllServices()

        isAdvertising =
            false

        currentKey =
            null

        keyCharacteristic =
            null

        println(
            "SmartAttend BLE: " +
                    "iOS student key advertising STOPPED"
        )
    }

    /**
     * Check whether the iOS student
     * advertiser is currently active.
     */
    fun isCurrentlyAdvertising():
            Boolean {

        return isAdvertising
    }
}