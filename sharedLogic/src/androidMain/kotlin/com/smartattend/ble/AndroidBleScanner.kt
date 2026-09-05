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
import android.os.ParcelUuid

class AndroidBleScanner(
    private val context: Context
) {

    private val bluetoothManager =
        context.getSystemService(
            Context.BLUETOOTH_SERVICE
        ) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    private val bluetoothLeScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false

    /*
     * Callback used to pass a detected
     * student cryptographic key to the
     * application/backend layer.
     */
    private var onStudentKeyDetected:
            ((String) -> Unit)? = null

    /*
     * Stores keys already detected during
     * the current scanning session.
     *
     * This prevents the same student's
     * advertisement from triggering the
     * backend repeatedly.
     */
    private val detectedStudentKeys =
        mutableSetOf<String>()

    /**
     * Student Cryptographic Key BLE Service UUID.
     *
     * This MUST be the same UUID used by
     * AndroidStudentKeyAdvertiser.
     */
    private val studentKeyServiceUuid =
        ParcelUuid(
            AndroidStudentKeyAdvertiser.STUDENT_KEY_SERVICE_UUID
        )

    /**
     * Start scanning for student
     * cryptographic key advertisements.
     *
     * The callback returns ONLY:
     *
     * cryptographicKey
     */
    @SuppressLint("MissingPermission")
    fun startScanning(
        onStudentKeyDetected: (String) -> Unit
    ) {

        /*
         * Prevent starting multiple
         * scanners at the same time.
         */
        if (isScanning) {

            println(
                "SmartAttend BLE: Already scanning"
            )

            return
        }

        /*
         * Check Bluetooth scan permission.
         */
        if (!hasScanPermission()) {

            println(
                "SmartAttend BLE: " +
                        "BLUETOOTH_SCAN permission missing"
            )

            return
        }

        /*
         * Get Bluetooth adapter.
         */
        val adapter = bluetoothAdapter

        if (adapter == null) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth unavailable"
            )

            return
        }

        /*
         * Bluetooth must be enabled.
         */
        if (!adapter.isEnabled) {

            println(
                "SmartAttend BLE: " +
                        "Bluetooth disabled"
            )

            return
        }

        /*
         * Get BLE scanner.
         */
        val scanner = bluetoothLeScanner

        if (scanner == null) {

            println(
                "SmartAttend BLE: " +
                        "BLE scanner unavailable"
            )

            return
        }

        /*
         * Store application callback.
         */
        this.onStudentKeyDetected =
            onStudentKeyDetected

        /*
         * Start a fresh detection session.
         *
         * Keys detected during a previous
         * scan are allowed again.
         */
        detectedStudentKeys.clear()

        /*
         * Configure BLE scanning.
         *
         * LOW_LATENCY is useful for the
         * attendance prototype because we
         * want student advertisements
         * to be detected quickly.
         */
        val scanSettings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        println(
            "SmartAttend BLE: " +
                    "Starting student key scanner"
        )

        /*
         * Start BLE scan.
         *
         * We intentionally scan broadly and
         * perform SmartAttend UUID filtering
         * inside processScanResult().
         */
        scanner.startScan(
            null,
            scanSettings,
            scanCallback
        )

        isScanning = true

        println(
            "SmartAttend BLE: " +
                    "Student scanner STARTED"
        )
    }

    /**
     * Stop BLE scanning.
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {

        if (!isScanning) {
            return
        }

        if (hasScanPermission()) {

            try {

                bluetoothLeScanner?.stopScan(
                    scanCallback
                )

            } catch (e: SecurityException) {

                println(
                    "SmartAttend BLE: " +
                            "Unable to stop scanner"
                )
            }
        }

        isScanning = false

        /*
         * Remove callback so that no more
         * keys are delivered to the
         * application layer.
         */
        onStudentKeyDetected = null

        /*
         * Clear detected keys.
         */
        detectedStudentKeys.clear()

        println(
            "SmartAttend BLE: " +
                    "Student scanner STOPPED"
        )
    }

    /**
     * BLE scan callback.
     */
    private val scanCallback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                processScanResult(result)
            }

            override fun onScanFailed(
                errorCode: Int
            ) {

                println(
                    "SmartAttend BLE: " +
                            "Student scan FAILED"
                )

                println(
                    "SmartAttend BLE: " +
                            "Scan error = $errorCode"
                )

                isScanning = false
            }
        }

    /**
     * Process a discovered BLE advertisement.
     */
    private fun processScanResult(
        result: ScanResult
    ) {

        /*
         * A scan record is required
         * to inspect the advertisement.
         */
        val scanRecord =
            result.scanRecord
                ?: return

        /*
         * Check whether the advertisement
         * contains the SmartAttend Student
         * Key Service UUID.
         */
        val serviceUuids =
            scanRecord.serviceUuids

        if (
            serviceUuids == null ||
            !serviceUuids.contains(
                studentKeyServiceUuid
            )
        ) {

            /*
             * Not a SmartAttend student
             * advertisement.
             */
            return
        }

        /*
         * Read Service Data associated
         * with the Student Key Service UUID.
         */
        val serviceData =
            scanRecord.getServiceData(
                studentKeyServiceUuid
            ) ?: return

        /*
         * Convert the service data into
         * the student's cryptographic key.
         */
        val cryptographicKey =
            serviceData
                .toString(Charsets.UTF_8)
                .trim()
                .takeIf {
                    it.isNotBlank()
                }
                ?: return

        /*
         * Ignore the same key if it has
         * already been detected during
         * this scanning session.
         */
        if (
            !detectedStudentKeys.add(
                cryptographicKey
            )
        ) {

            println(
                "SmartAttend BLE: " +
                        "Duplicate student key ignored = " +
                        cryptographicKey
            )

            return
        }

        /*
         * New student detected.
         */
        println(
            "SmartAttend BLE: " +
                    "Student key received"
        )

        println(
            "SmartAttend BLE: " +
                    "Key = $cryptographicKey"
        )

        /*
         * Pass ONLY the cryptographic key
         * to the application/backend layer.
         *
         * BLE processing ends here.
         */
        onStudentKeyDetected?.invoke(
            cryptographicKey
        )
    }

    /**
     * Check whether BLE scan permission
     * has been granted.
     */
    private fun hasScanPermission(): Boolean {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

        } else {

            context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}