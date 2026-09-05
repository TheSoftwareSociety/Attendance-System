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
     * Application callback.
     *
     * Returns:
     * - student cryptographic key
     */
    private var onStudentKeyDetected:
            ((String) -> Unit)? = null

    /*
     * Prevent the same key from being
     * delivered repeatedly during
     * one scanning session.
     */
    private val detectedStudentKeys =
        mutableSetOf<String>()

    /*
     * SmartAttend Student Key Service UUID.
     *
     * MUST match AndroidStudentKeyAdvertiser.
     */
    private val studentKeyServiceUuid =
        ParcelUuid(
            AndroidStudentKeyAdvertiser.STUDENT_KEY_SERVICE_UUID
        )

    /**
     * ---------------------------------------------------------
     * START SCANNING
     * ---------------------------------------------------------
     */
    @SuppressLint("MissingPermission")
    fun startScanning(
        onStudentKeyDetected: (String) -> Unit
    ) {

        println(
            "================================================="
        )

        println(
            "SmartAttend BLE DEBUG: START SCANNING"
        )

        println(
            "================================================="
        )

        /*
         * Prevent duplicate scanner instances.
         */
        if (isScanning) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Scanner already running"
            )

            return
        }

        /*
         * Check permissions.
         */
        if (!hasScanPermission()) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "BLUETOOTH_SCAN permission NOT granted"
            )

            return
        }

        println(
            "SmartAttend BLE DEBUG: " +
                    "BLUETOOTH_SCAN permission granted"
        )

        /*
         * Bluetooth adapter.
         */
        val adapter =
            bluetoothAdapter

        if (adapter == null) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Bluetooth adapter is NULL"
            )

            return
        }

        println(
            "SmartAttend BLE DEBUG: " +
                    "Bluetooth adapter available"
        )

        /*
         * Bluetooth state.
         */
        if (!adapter.isEnabled) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Bluetooth is DISABLED"
            )

            return
        }

        println(
            "SmartAttend BLE DEBUG: " +
                    "Bluetooth is ENABLED"
        )

        /*
         * BLE scanner.
         */
        val scanner =
            bluetoothLeScanner

        if (scanner == null) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "BluetoothLeScanner is NULL"
            )

            return
        }

        println(
            "SmartAttend BLE DEBUG: " +
                    "BluetoothLeScanner available"
        )

        /*
         * Save callback.
         */
        this.onStudentKeyDetected =
            onStudentKeyDetected

        /*
         * New scanning session.
         */
        detectedStudentKeys.clear()

        /*
         * LOW_LATENCY is appropriate for
         * the attendance prototype.
         */
        val scanSettings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        println(
            "SmartAttend BLE DEBUG: " +
                    "Scan mode = LOW_LATENCY"
        )

        println(
            "SmartAttend BLE DEBUG: " +
                    "Looking for Student UUID = " +
                    studentKeyServiceUuid.uuid
        )

        /*
         * IMPORTANT:
         *
         * No ScanFilter is used.
         *
         * We want to see EVERY BLE advertisement
         * during diagnosis.
         */
        try {

            scanner.startScan(
                null,
                scanSettings,
                scanCallback
            )

            isScanning = true

            println(
                "SmartAttend BLE DEBUG: " +
                        "BLE SCANNER STARTED SUCCESSFULLY"
            )

        } catch (e: SecurityException) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "SecurityException while starting scan"
            )

            println(
                "SmartAttend BLE DEBUG: " +
                        "Message = ${e.message}"
            )

        } catch (e: Exception) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Exception while starting scan"
            )

            println(
                "SmartAttend BLE DEBUG: " +
                        "Message = ${e.message}"
            )
        }
    }

    /**
     * ---------------------------------------------------------
     * STOP SCANNING
     * ---------------------------------------------------------
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {

        println(
            "SmartAttend BLE DEBUG: STOP SCANNING"
        )

        if (!isScanning) {

            println(
                "SmartAttend BLE DEBUG: " +
                        "Scanner was not running"
            )

            return
        }

        if (hasScanPermission()) {

            try {

                bluetoothLeScanner?.stopScan(
                    scanCallback
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "stopScan() called"
                )

            } catch (e: SecurityException) {

                println(
                    "SmartAttend BLE DEBUG: " +
                            "SecurityException stopping scanner"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Message = ${e.message}"
                )
            }
        }

        isScanning = false

        onStudentKeyDetected = null

        detectedStudentKeys.clear()

        println(
            "SmartAttend BLE DEBUG: " +
                    "Scanner stopped"
        )
    }

    /**
     * ---------------------------------------------------------
     * BLE CALLBACK
     * ---------------------------------------------------------
     */
    private val scanCallback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {

                /*
                 * THIS IS THE MOST IMPORTANT
                 * DIAGNOSTIC MESSAGE.
                 *
                 * If this appears repeatedly,
                 * Android BLE scanning itself works.
                 */
                println(
                    "-------------------------------------------------"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "BLE DEVICE DETECTED"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "RSSI = ${result.rssi}"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Callback type = $callbackType"
                )

                /*
                 * DO NOT call result.device.name here.
                 *
                 * That requires BLUETOOTH_CONNECT and
                 * is unnecessary for our test.
                 */

                val record =
                    result.scanRecord

                if (record == null) {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "ScanRecord = NULL"
                    )

                    return
                }

                /*
                 * Raw bytes.
                 */
                val bytes =
                    record.bytes

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Raw packet size = ${bytes.size} bytes"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Raw packet = " +
                            bytes.joinToString(" ") {
                                "%02X".format(it)
                            }
                )

                /*
                 * Device name from advertisement only.
                 *
                 * This does NOT access BluetoothDevice.name.
                 */
                println(
                    "SmartAttend BLE DEBUG: " +
                            "Advertised name = " +
                            record.deviceName
                )

                /*
                 * TX power if present.
                 */
                println(
                    "SmartAttend BLE DEBUG: " +
                            "TX power = ${record.txPowerLevel}"
                )

                /*
                 * Manufacturer data.
                 */
                val manufacturerData =
                    record.manufacturerSpecificData

                if (manufacturerData.size() > 0) {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "Manufacturer data FOUND"
                    )

                    for (i in 0 until manufacturerData.size()) {

                        val id =
                            manufacturerData.keyAt(i)

                        val data =
                            manufacturerData.valueAt(i)

                        println(
                            "SmartAttend BLE DEBUG: " +
                                    "Manufacturer ID = $id"
                        )

                        println(
                            "SmartAttend BLE DEBUG: " +
                                    "Manufacturer bytes = " +
                                    data.joinToString(" ") {
                                        "%02X".format(it)
                                    }
                        )
                    }

                } else {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "No manufacturer data"
                    )
                }

                /*
                 * Service UUIDs.
                 */
                val serviceUuids =
                    record.serviceUuids

                if (
                    serviceUuids != null &&
                    serviceUuids.isNotEmpty()
                ) {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "Service UUIDs:"
                    )

                    serviceUuids.forEach { uuid ->

                        println(
                            "SmartAttend BLE DEBUG: " +
                                    "UUID = ${uuid.uuid}"
                        )
                    }

                } else {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "NO SERVICE UUIDS"
                    )
                }

                /*
                 * -------------------------------------------------
                 * CHECK SMARTATTEND STUDENT SERVICE DATA
                 * -------------------------------------------------
                 *
                 * We intentionally check service data directly.
                 *
                 * We DO NOT require serviceUuids.contains(...)
                 *
                 * This makes diagnosis much more reliable.
                 */
                val studentServiceData =
                    record.getServiceData(
                        studentKeyServiceUuid
                    )

                if (studentServiceData == null) {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "NO SmartAttend Student Service Data"
                    )

                    println(
                        "-------------------------------------------------"
                    )

                    return
                }

                println(
                    "SmartAttend BLE DEBUG: " +
                            "!!! SMARTATTEND STUDENT SERVICE DATA FOUND !!!"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Service UUID = " +
                            studentKeyServiceUuid.uuid
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Service data length = " +
                            "${studentServiceData.size} bytes"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Service data HEX = " +
                            studentServiceData.joinToString(" ") {
                                "%02X".format(it)
                            }
                )

                /*
                 * Convert service data to UTF-8.
                 */
                val cryptographicKey =
                    studentServiceData
                        .toString(Charsets.UTF_8)
                        .trim()

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Decoded student key = " +
                            cryptographicKey
                )

                if (cryptographicKey.isBlank()) {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "Decoded student key is EMPTY"
                    )

                    return
                }

                /*
                 * Duplicate protection.
                 */
                if (
                    !detectedStudentKeys.add(
                        cryptographicKey
                    )
                ) {

                    println(
                        "SmartAttend BLE DEBUG: " +
                                "Duplicate student key ignored = " +
                                cryptographicKey
                    )

                    return
                }

                /*
                 * NEW STUDENT.
                 */
                println(
                    "================================================="
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "!!! NEW STUDENT DETECTED !!!"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Student Key = $cryptographicKey"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "RSSI = ${result.rssi}"
                )

                println(
                    "================================================="
                )

                /*
                 * Send only the key to application layer.
                 */
                onStudentKeyDetected?.invoke(
                    cryptographicKey
                )
            }

            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {

                println(
                    "SmartAttend BLE DEBUG: " +
                            "BATCH RESULTS = ${results.size}"
                )

                results.forEach { result ->

                    processScanResult(
                        result
                    )
                }
            }

            override fun onScanFailed(
                errorCode: Int
            ) {

                println(
                    "================================================="
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "!!! BLE SCAN FAILED !!!"
                )

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Error code = $errorCode"
                )

                val error =
                    when (errorCode) {

                        ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
                            "SCAN_FAILED_ALREADY_STARTED"

                        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
                            "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"

                        ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
                            "SCAN_FAILED_INTERNAL_ERROR"

                        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
                            "SCAN_FAILED_FEATURE_UNSUPPORTED"

                        else ->
                            "UNKNOWN_ERROR"
                    }

                println(
                    "SmartAttend BLE DEBUG: " +
                            "Error meaning = $error"
                )

                println(
                    "================================================="
                )

                isScanning = false
            }
        }

    /**
     * ---------------------------------------------------------
     * PROCESS BATCH RESULT
     * ---------------------------------------------------------
     */
    private fun processScanResult(
        result: ScanResult
    ) {

        val record =
            result.scanRecord
                ?: return

        println(
            "SmartAttend BLE DEBUG: " +
                    "Processing batch result"
        )

        println(
            "SmartAttend BLE DEBUG: " +
                    "RSSI = ${result.rssi}"
        )

        val studentServiceData =
            record.getServiceData(
                studentKeyServiceUuid
            )
                ?: return

        val cryptographicKey =
            studentServiceData
                .toString(Charsets.UTF_8)
                .trim()

        if (cryptographicKey.isBlank()) {
            return
        }

        if (
            !detectedStudentKeys.add(
                cryptographicKey
            )
        ) {
            return
        }

        println(
            "SmartAttend BLE DEBUG: " +
                    "NEW STUDENT FROM BATCH = " +
                    cryptographicKey
        )

        println(
            "SmartAttend BLE DEBUG: " +
                    "RSSI = ${result.rssi}"
        )

        onStudentKeyDetected?.invoke(
            cryptographicKey
        )
    }

    /**
     * ---------------------------------------------------------
     * PERMISSION CHECK
     * ---------------------------------------------------------
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