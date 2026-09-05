package com.smartattend.ble

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun AndroidBleTestScreen() {

    val context = LocalContext.current

    val activity = context as? Activity

    val bleManager = remember {
        AndroidBleManager(context)
    }

    var sessionId by remember {
        mutableStateOf("BCS701")
    }

    var detectedSessionId by remember {
        mutableStateOf<String?>(null)
    }

    var status by remember {
        mutableStateOf("BLE Idle")
    }

    var isTeacherBroadcasting by remember {
        mutableStateOf(false)
    }

    var isStudentScanning by remember {
        mutableStateOf(false)
    }

    /*
     * Android 12+
     *
     * BLUETOOTH_SCAN
     * BLUETOOTH_ADVERTISE
     * BLUETOOTH_CONNECT
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val scanGranted =
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true

            val advertiseGranted =
                permissions[Manifest.permission.BLUETOOTH_ADVERTISE] == true

            val connectGranted =
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true

            if (scanGranted && advertiseGranted && connectGranted) {

                status = "Bluetooth permissions granted"

            } else {

                status = "Bluetooth permissions denied"
            }
        }

    /*
     * Request Bluetooth permissions.
     */
    fun requestBluetoothPermissions() {

        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S
        ) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )

        } else {

            status = "Bluetooth permissions handled by Android version"
        }
    }

    /*
     * Teacher broadcast.
     */
    fun startTeacher() {

        if (sessionId.isBlank()) {

            status = "Enter a Session ID"

            return
        }

        requestBluetoothPermissions()

        bleManager.startTeacherBroadcast(
            sessionId = sessionId.trim()
        )

        isTeacherBroadcasting = true
        status = "Teacher broadcasting: ${sessionId.trim()}"
    }

    /*
     * Stop teacher broadcast.
     */
    fun stopTeacher() {

        bleManager.stopTeacherBroadcast()

        isTeacherBroadcasting = false

        status = "Teacher broadcast stopped"
    }

    /*
     * Student scanning.
     */
    fun startStudent() {

        requestBluetoothPermissions()

        bleManager.startStudentScanning { detectedId, rssi ->

            detectedSessionId = detectedId

            status =
                "Session detected: $detectedId\nRSSI: $rssi dBm"
        }

        isStudentScanning = true

        status = "Student scanning..."
    }

    /*
     * Stop student scanning.
     */
    fun stopStudent() {

        bleManager.stopStudentScanning()

        isStudentScanning = false

        status = "Student scanning stopped"
    }

    /*
     * Stop BLE when screen/activity is destroyed.
     */
    DisposableEffect(Unit) {

        onDispose {

            bleManager.stopAll()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "SmartAttend BLE Test",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Status: $status",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        /*
         * Session ID input.
         */
        OutlinedTextField(
            value = sessionId,

            onValueChange = {
                sessionId = it
            },

            modifier = Modifier.fillMaxWidth(),

            label = {
                Text("Session ID")
            },

            singleLine = true,

            enabled = !isTeacherBroadcasting
        )

        /*
         * Teacher section.
         */
        Text(
            text = "Teacher",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = {

                if (isTeacherBroadcasting) {
                    stopTeacher()
                } else {
                    startTeacher()
                }

            },

            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (isTeacherBroadcasting)
                    "STOP TEACHER BROADCAST"
                else
                    "START TEACHER BROADCAST"
            )
        }

        /*
         * Student section.
         */
        Text(
            text = "Student",
            style = MaterialTheme.typography.titleLarge
        )

        Button(
            onClick = {

                if (isStudentScanning) {
                    stopStudent()
                } else {
                    startStudent()
                }

            },

            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (isStudentScanning)
                    "STOP STUDENT SCANNING"
                else
                    "START STUDENT SCANNING"
            )
        }

        /*
         * Detected session.
         */
        detectedSessionId?.let { detectedId ->

            Text(
                text = "Detected Session ID:",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = detectedId,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}