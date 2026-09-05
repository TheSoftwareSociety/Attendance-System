package com.smartattend.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.UUID

/**
 * SmartAttend Android Student BLE Scanner.
 *
 * Responsibility:
 *
 * Teacher:
 * - Scan for student cryptographic keys
 * - Read RSSI
 *
 * The scanner searches for the same SmartAttend
 * service UUID used by AndroidStudentKeyAdvertiser.
 *
 * BLE validation/business logic is handled
 * outside this class.
 */
class AndroidStudentKeyScanner(
    private val context: Context
) {

    companion object {

        /**
         * SAME service UUID used by:
         *
         * AndroidStudentKeyAdvertiser
         * IosStudentKeyAdvertiser
         * IosStudentKeyScanner
         */
        val STUDENT_KEY_SERVICE_UUID: UUID =
            UUID.fromString(
                "8b7a0001-7c42-4d91-9a21-123456789abc"
            )
    }

    /**
     * Android Bluetooth manager.
     */
    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    /**
     * Bluetooth adapter.
     */
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    /**
     * BLE scanner.
     */
    private val bleScanner: BluetoothLeScanner?
        get() =
            bluetoothAdapter
                ?.bluetoothLeScanner

    /**
     * Whether scanning is active.
     */
    private var isScanning =
        false

    /**
     * Callback used by the application layer.
     *
     * String = cryptographic key
     * Int = RSSI
     */
    private var onStudentKeyDetected:
            ((String, Int) -> Unit)? = null

    /**
     * Prevent sending the exact same
     * advertisement repeatedly.
     *
     * BLE advertisements can arrive many
     * times per second.
     */
    private val recentlyDetectedKeys =
        mutableMapOf<String, Long>()

    /**
     * Time during which the same key
     * will not immediately be reported again.
     */
    private val duplicateWindowMs =
        1000L

    /**
     * Android BLE scan callback.
     */
    private val scanCallback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                processScanResult(result)
            }

            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {

                for (result in results) {

                    processScanResult(result)
                }
            }

            override fun onScanFailed(
                errorCode: Int
            ) {

                println(
                    "SmartAttend BLE: " +
                            "Android student scanner FAILED"
                )

                println(
                    "SmartAttend BLE: " +
                            "Scan error code = $errorCode"
                )

                isScanning = false
            }
        }

    /**
     * Start scanning for student keys.
     *
     * @param onStudentKeyDetected
     * Callback receiving:
     *
     * cryptographicKey
     * RSSI
     */
    @SuppressLint("MissingPermission")
    fun startScanning(
        onStudentKeyDetected:
            (String, Int) -> Unit
    ) {

        /*
         * Prevent duplicate scan requests.
         */
        if (isScanning) {

            println(
                "SmartAttend BLE: " +
                        "Android student scanner already running"
            )

            return
        }

        /*
         * Store callback.
         */
        this.onStudentKeyDetected =
            onStudentKeyDetected

        /*
         * Check Android BLE permissions.
         */
        if (!hasBluetoothScanPermission()) {

            println(
                "SmartAttend BLE: " +
                        "BLUETOOTH_SCAN permission missing"
            )

            this.onStudentKeyDetected =
                null

            return
        }

        /*
         * Get Bluetooth adapter.
         */
        val adapter =
            bluetoothAdapter

        if (adapter == null) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth unavailable"
            )

            this.onStudentKeyDetected =
                null

            return
        }

        /*
         * Check whether Bluetooth is enabled.
         */
        if (!adapter.isEnabled) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth disabled"
            )

            this.onStudentKeyDetected =
                null

            return
        }

        /*
         * Get BLE scanner.
         */
        val scanner =
            bleScanner

        if (scanner == null) {

            println(
                "SmartAttend BLE: " +
                        "BLE scanner unavailable"
            )

            this.onStudentKeyDetected =
                null

            return
        }

        /*
         * Remove old duplicate tracking.
         */
        recentlyDetectedKeys.clear()

        /*
         * Scan specifically for the
         * SmartAttend student-key service.
         */
        val scanFilter =
            android.bluetooth.le.ScanFilter.Builder()
                .setServiceUuid(
                    android.os.ParcelUuid(
                        STUDENT_KEY_SERVICE_UUID
                    )
                )
                .build()

        /*
         * Low latency is useful for
         * attendance detection.
         */
        val scanSettings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        println(
            "SmartAttend BLE: " +
                    "Starting Android student key scanner"
        )

        println(
            "SmartAttend BLE: " +
                    "Looking for service = " +
                    STUDENT_KEY_SERVICE_UUID
        )

        try {

            scanner.startScan(
                listOf(scanFilter),
                scanSettings,
                scanCallback
            )

            isScanning = true

            println(
                "SmartAttend BLE: " +
                        "Android student scanner STARTED"
            )

        } catch (e: SecurityException) {

            println(
                "SmartAttend BLE: " +
                        "Unable to start BLE scanner"
            )

            println(
                "SmartAttend BLE: " +
                        "Permission error = ${e.message}"
            )

            isScanning = false

            this.onStudentKeyDetected =
                null
        }
    }

    /**
     * Process one BLE scan result.
     */
    @SuppressLint("MissingPermission")
    private fun processScanResult(
        result: ScanResult
    ) {

        /*
         * Obtain scan record.
         */
        val scanRecord =
            result.scanRecord
                ?: return

        /*
         * Get SmartAttend service data.
         *
         * AndroidStudentKeyAdvertiser places
         * the cryptographic key directly into
         * Service Data.
         */
        val serviceData =
            scanRecord.getServiceData(
                android.os.ParcelUuid(
                    STUDENT_KEY_SERVICE_UUID
                )
            )
                ?: return

        if (serviceData.isEmpty()) {

            println(
                "SmartAttend BLE: " +
                        "Received empty service data"
            )

            return
        }

        /*
         * Convert service data bytes into
         * the original UTF-8 cryptographic key.
         */
        val cryptographicKey =
            try {

                serviceData
                    .toString(
                        Charsets.UTF_8
                    )
                    .trim()

            } catch (e: Exception) {

                println(
                    "SmartAttend BLE: " +
                            "Unable to decode student key"
                )

                return
            }

        /*
         * Ignore invalid keys.
         */
        if (cryptographicKey.isBlank()) {

            return
        }

        /*
         * Get RSSI.
         */
        val rssi =
            result.rssi

        /*
         * Prevent the same BLE advertisement
         * from triggering the application
         * callback continuously.
         */
        val now =
            System.currentTimeMillis()

        val previousTime =
            recentlyDetectedKeys[
                cryptographicKey
            ]

        if (
            previousTime != null &&
            now - previousTime <
            duplicateWindowMs
        ) {

            return
        }

        recentlyDetectedKeys[
            cryptographicKey
        ] = now

        /*
         * Remove stale entries occasionally.
         */
        if (recentlyDetectedKeys.size > 100) {

            val cutoff =
                now - duplicateWindowMs

            recentlyDetectedKeys
                .entries
                .removeIf {
                    it.value < cutoff
                }
        }

        println(
            "SmartAttend BLE: " +
                    "Student key detected"
        )

        println(
            "SmartAttend BLE: " +
                    "RSSI = $rssi"
        )

        /*
         * DO NOT put attendance/business
         * validation inside the BLE scanner.
         *
         * Pass the raw key + RSSI to the
         * application/business layer.
         */
        onStudentKeyDetected?.invoke(
            cryptographicKey,
            rssi
        )
    }

    /**
     * Check Android BLE scan permission.
     */
    private fun hasBluetoothScanPermission():
            Boolean {

        /*
         * Android 12+
         */
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            return context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        }

        /*
         * Older Android versions use
         * location permission for BLE scanning.
         */
        return context.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Stop BLE scanning.
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {

        val scanner =
            bleScanner

        if (scanner != null) {

            try {

                scanner.stopScan(
                    scanCallback
                )

            } catch (e: SecurityException) {

                println(
                    "SmartAttend BLE: " +
                            "Unable to stop Android scanner"
                )
            }
        }

        isScanning =
            false

        onStudentKeyDetected =
            null

        recentlyDetectedKeys.clear()

        println(
            "SmartAttend BLE: " +
                    "Android student scanner STOPPED"
        )
    }

    /**
     * Check whether Android is currently
     * scanning for students.
     */
    fun isCurrentlyScanning():
            Boolean {

        return isScanning
    }
}