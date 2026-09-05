package com.smartattend.ble

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreBluetooth.CBAdvertisementDataServiceDataKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * SmartAttend iOS Student BLE Scanner.
 *
 * Responsibility:
 *
 * Teacher:
 * - Scan for student cryptographic keys
 * - Read RSSI
 *
 * The scanner searches for the same SmartAttend
 * service UUID used by AndroidStudentKeyAdvertiser
 * and IosStudentKeyAdvertiser.
 *
 * BLE validation/business logic is handled
 * outside this class.
 */
@OptIn(ExperimentalForeignApi::class)
class IosStudentKeyScanner {

    companion object {

        /**
         * SAME service UUID used by:
         *
         * AndroidStudentKeyAdvertiser
         * IosStudentKeyAdvertiser
         */
        const val STUDENT_KEY_SERVICE_UUID =
            "8B7A0001-7C42-4D91-9A21-123456789ABC"
    }

    /**
     * SmartAttend student-key service UUID.
     */
    private val serviceUuid =
        CBUUID.UUIDWithString(
            STUDENT_KEY_SERVICE_UUID
        )

    /**
     * iOS Central Manager.
     *
     * Used for BLE scanning.
     */
    private var centralManager:
            CBCentralManager? = null

    /**
     * Whether scanning has been requested.
     */
    private var isScanning =
        false

    /**
     * Callback invoked when a student key
     * is detected.
     *
     * String = cryptographic key
     * Int = RSSI
     */
    private var onStudentKeyDetected:
            ((String, Int) -> Unit)? = null

    /**
     * CoreBluetooth central delegate.
     */
    private val delegate =
        object : NSObject(),
            CBCentralManagerDelegateProtocol {

            /**
             * Called whenever the Bluetooth
             * state changes.
             *
             * In this Kotlin/Native environment:
             *
             * PoweredOff = 4
             * PoweredOn  = 5
             */
            override fun centralManagerDidUpdateState(
                central: CBCentralManager
            ) {

                when (central.state) {

                    /*
                     * Bluetooth Powered ON
                     */
                    5L -> {

                        println(
                            "SmartAttend BLE: " +
                                    "iOS Bluetooth powered ON"
                        )

                        /*
                         * Start scanning if the
                         * application requested scanning.
                         */
                        if (isScanning) {

                            startScanningInternal()
                        }
                    }

                    /*
                     * Bluetooth Powered OFF
                     */
                    4L -> {

                        println(
                            "SmartAttend BLE: " +
                                    "iOS Bluetooth powered OFF"
                        )

                        isScanning = false
                    }

                    /*
                     * Unknown / Resetting /
                     * Unsupported / Unauthorized
                     */
                    else -> {

                        println(
                            "SmartAttend BLE: " +
                                    "iOS Bluetooth unavailable"
                        )

                        isScanning = false
                    }
                }
            }

            /**
             * Called whenever a BLE peripheral
             * matching the scan is discovered.
             */
            override fun centralManager(
                central: CBCentralManager,
                didDiscoverPeripheral: CBPeripheral,
                advertisementData: Map<Any?, *>,
                RSSI: platform.Foundation.NSNumber
            ) {

                println(
                    "SmartAttend BLE: " +
                            "BLE peripheral discovered"
                )

                println(
                    "SmartAttend BLE: " +
                            "RSSI = ${RSSI.intValue}"
                )

                processAdvertisement(
                    advertisementData = advertisementData,
                    rssi = RSSI.intValue
                )
            }
        }

    /**
     * Start scanning for SmartAttend student keys.
     *
     * @param onStudentKeyDetected
     * Callback receiving:
     *
     * cryptographicKey
     * RSSI
     */
    fun startScanning(
        onStudentKeyDetected:
            (String, Int) -> Unit
    ) {

        /*
         * Prevent duplicate scanning requests.
         */
        if (isScanning) {

            println(
                "SmartAttend BLE: " +
                        "iOS student scanner already running"
            )

            return
        }

        /*
         * Store application callback.
         */
        this.onStudentKeyDetected =
            onStudentKeyDetected

        /*
         * Mark scanning as requested.
         */
        isScanning = true

        println(
            "SmartAttend BLE: " +
                    "Starting iOS student key scanner"
        )

        /*
         * Create Central Manager if it
         * does not already exist.
         *
         * Bluetooth state will be delivered
         * through centralManagerDidUpdateState().
         */
        if (centralManager == null) {

            centralManager =
                CBCentralManager(
                    delegate = delegate,
                    queue = null
                )

        } else {

            /*
             * Central Manager already exists.
             */
            startScanningInternal()
        }
    }

    /**
     * Start the actual BLE scan after
     * Bluetooth becomes available.
     */
    private fun startScanningInternal() {

        val central =
            centralManager
                ?: return

        /*
         * CBManagerStatePoweredOn =
         * 5 in the generated Kotlin/Native API
         * available in this project.
         */
        if (central.state != 5L) {

            println(
                "SmartAttend BLE: " +
                        "iOS Bluetooth is not ready"
            )

            return
        }

        /*
         * Stop an existing scan before
         * starting a new one.
         */
        if (central.isScanning) {

            central.stopScan()
        }

        println(
            "SmartAttend BLE: " +
                    "Starting iOS BLE scan"
        )

        println(
            "SmartAttend BLE: " +
                    "Looking for service = " +
                    STUDENT_KEY_SERVICE_UUID
        )

        /*
         * Scan specifically for the
         * SmartAttend student-key service.
         *
         * This prevents us from processing
         * unrelated BLE devices.
         */
        central.scanForPeripheralsWithServices(
            listOf(serviceUuid),
            options = null
        )

        println(
            "SmartAttend BLE: " +
                    "iOS student scanner STARTED"
        )
    }

    /**
     * Process BLE advertisement data.
     *
     * AndroidStudentKeyAdvertiser broadcasts:
     *
     * Service UUID
     * +
     * Service Data
     *
     * Service Data contains:
     *
     * cryptographic key
     */
    private fun processAdvertisement(
        advertisementData:
        Map<Any?, *>,
        rssi: Int
    ) {

        /*
         * Obtain the Service Data section
         * from the advertisement packet.
         */
        val serviceData =
            advertisementData[
                CBAdvertisementDataServiceDataKey
            ] as? Map<*, *>
                ?: run {

                    println(
                        "SmartAttend BLE: " +
                                "No service data found"
                    )

                    return
                }

        /*
         * Find the SmartAttend service data.
         *
         * Normally CoreBluetooth gives us
         * the CBUUID as the map key.
         */
        val rawData =
            serviceData[serviceUuid]
                    as? NSData
                ?: findServiceData(
                    serviceData
                )
                ?: run {

                    println(
                        "SmartAttend BLE: " +
                                "SmartAttend service data not found"
                    )

                    return
                }

        /*
         * Convert NSData into ByteArray.
         *
         * NSData.toByteArray() is not available
         * in this Kotlin/Native setup, so we use
         * memcpy to copy the native bytes.
         */
        val keyBytes =
            nsDataToByteArray(
                rawData
            )

        if (keyBytes.isEmpty()) {

            println(
                "SmartAttend BLE: " +
                        "Received empty student key"
            )

            return
        }

        /*
         * Android advertiser sends the key
         * using UTF-8.
         *
         * Decode the received bytes.
         */
        val cryptographicKey =
            keyBytes
                .decodeToString()
                .trim()

        /*
         * Ignore invalid/empty keys.
         */
        if (cryptographicKey.isBlank()) {

            println(
                "SmartAttend BLE: " +
                        "Received blank student key"
            )

            return
        }

        /*
         * Log detection.
         */
        println(
            "SmartAttend BLE: " +
                    "Student key detected"
        )

        println(
            "SmartAttend BLE: " +
                    "RSSI = $rssi"
        )

        println(
            "SmartAttend BLE: " +
                    "Student key received"
        )

        /*
         * IMPORTANT:
         *
         * Do not perform attendance validation
         * here.
         *
         * Pass the key and RSSI to the
         * application/business layer.
         */
        onStudentKeyDetected?.invoke(
            cryptographicKey,
            rssi
        )
    }

    /**
     * Find SmartAttend service data when
     * direct CBUUID map lookup does not work
     * because of Kotlin/Native bridge behavior.
     */
    private fun findServiceData(
        serviceData: Map<*, *>
    ): NSData? {

        for (entry in serviceData.entries) {

            val key =
                entry.key

            val value =
                entry.value

            /*
             * Check that the value is NSData.
             */
            val data =
                value as? NSData
                    ?: continue

            /*
             * Check whether the map key
             * represents our service UUID.
             */
            if (key is CBUUID) {

                val uuidString =
                    key.UUIDString

                if (
                    uuidString.equals(
                        STUDENT_KEY_SERVICE_UUID,
                        ignoreCase = true
                    )
                ) {

                    return data
                }
            }
        }

        return null
    }

    /**
     * Convert Foundation NSData
     * into Kotlin ByteArray.
     */
    private fun nsDataToByteArray(
        data: NSData
    ): ByteArray {

        val length =
            data.length.toInt()

        if (length <= 0) {

            return ByteArray(0)
        }

        val bytes =
            ByteArray(length)

        bytes.usePinned { pinned ->

            memcpy(
                pinned.addressOf(0),
                data.bytes,
                length.toULong()
            )
        }

        return bytes
    }

    /**
     * Stop BLE scanning.
     */
    fun stopScanning() {

        val central =
            centralManager

        if (central != null) {

            try {

                if (central.isScanning) {

                    central.stopScan()
                }

            } catch (e: Exception) {

                println(
                    "SmartAttend BLE: " +
                            "Unable to stop iOS scanner"
                )
            }
        }

        /*
         * Reset scanning state.
         */
        isScanning =
            false

        /*
         * Remove application callback.
         */
        onStudentKeyDetected =
            null

        println(
            "SmartAttend BLE: " +
                    "iOS student scanner STOPPED"
        )
    }

    /**
     * Check whether iOS is currently scanning.
     */
    fun isCurrentlyScanning():
            Boolean {

        return isScanning
    }
}