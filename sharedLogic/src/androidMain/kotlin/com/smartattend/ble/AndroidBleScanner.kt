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

    private var onSessionDetected:
            ((String, Int) -> Unit)? = null

    /**
     * Start scanning for SmartAttend BLE.
     *
     * Returns:
     *
     * Session ID
     * RSSI
     */
    @SuppressLint("MissingPermission")
    fun startScanning(
        onSessionDetected: (String, Int) -> Unit
    ) {

        if (isScanning) {

            println(
                "SmartAttend BLE: " +
                        "Already scanning"
            )

            return
        }

        if (!hasScanPermission()) {

            println(
                "SmartAttend BLE: " +
                        "BLUETOOTH_SCAN permission missing"
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

        val scanner = bluetoothLeScanner

        if (scanner == null) {

            println(
                "SmartAttend BLE: " +
                        "BLE scanner unavailable"
            )

            return
        }

        this.onSessionDetected =
            onSessionDetected

        val scanSettings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        println(
            "SmartAttend BLE: " +
                    "Starting scanner"
        )

        scanner.startScan(
            null,
            scanSettings,
            scanCallback
        )

        isScanning = true

        println(
            "SmartAttend BLE: " +
                    "Scanner STARTED"
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
        onSessionDetected = null

        println(
            "SmartAttend BLE: " +
                    "Scanner STOPPED"
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
                            "Scan FAILED"
                )

                println(
                    "SmartAttend BLE: " +
                            "Scan error = $errorCode"
                )

                isScanning = false
            }
        }

    /**
     * Process discovered BLE packet.
     */
    private fun processScanResult(
        result: ScanResult
    ) {

        val scanRecord =
            result.scanRecord
                ?: return

        /*
         * Check whether the SmartAttend
         * Service UUID exists.
         */
        val serviceUuids =
            scanRecord.serviceUuids

        val smartAttendUuid =
            ParcelUuid(
                AndroidBleAdvertiser.SERVICE_UUID
            )

        if (
            serviceUuids == null ||
            !serviceUuids.contains(
                smartAttendUuid
            )
        ) {

            return
        }

        /*
         * Read Session ID from Service Data.
         *
         * Example:
         *
         * BCS701
         */
        val serviceData =
            scanRecord.getServiceData(
                smartAttendUuid
            ) ?: return

        val sessionId =
            serviceData
                .toString(Charsets.UTF_8)
                .trim()
                .takeIf {
                    it.isNotBlank()
                }
                ?: return

        val rssi =
            result.rssi

        println(
            "SmartAttend BLE: " +
                    "SmartAttend packet received"
        )

        println(
            "SmartAttend BLE: " +
                    "Session = $sessionId"
        )

        println(
            "SmartAttend BLE: " +
                    "RSSI = $rssi dBm"
        )

        onSessionDetected?.invoke(
            sessionId,
            rssi
        )
    }

    /**
     * Check BLE scan permission.
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