package com.smartattend.ble

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AndroidBleTestScreen() {

    val context = LocalContext.current

    val activity = context as? Activity

    /*
     * Main BLE manager.
     *
     * Teacher:
     * - Broadcast Session ID
     * - Scan Student Cryptographic Keys
     *
     * Student:
     * - Broadcast Cryptographic Key
     */
    val bleManager = remember {
        AndroidBleManager(context)
    }

    /*
     * Student cryptographic key advertiser.
     */
    val studentKeyAdvertiser = remember {
        AndroidStudentKeyAdvertiser(context)
    }

    /*
     * --------------------------------
     * TEACHER SESSION
     * --------------------------------
     */

    var sessionId by remember {
        mutableStateOf("BCS701")
    }

    var isTeacherBroadcasting by remember {
        mutableStateOf(false)
    }

    var isTeacherScanning by remember {
        mutableStateOf(false)
    }

    /*
     * --------------------------------
     * STUDENT
     * --------------------------------
     */

    var cryptographicKey by remember {
        mutableStateOf("081CS23")
    }

    var isStudentBroadcasting by remember {
        mutableStateOf(false)
    }

    /*
     * --------------------------------
     * DETECTED STUDENTS
     * --------------------------------
     *
     * A Set is used so that the same student
     * does not appear repeatedly.
     *
     * BLE advertisements are received
     * multiple times while broadcasting.
     */
    val detectedStudentKeys =
        remember {
            mutableStateListOf<String>()
        }

    /*
     * --------------------------------
     * STATUS
     * --------------------------------
     */

    var status by remember {
        mutableStateOf("BLE Idle")
    }

    /*
     * --------------------------------
     * PERMISSIONS
     * --------------------------------
     */

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                val scanGranted =
                    permissions[
                        Manifest.permission.BLUETOOTH_SCAN
                    ] == true

                val advertiseGranted =
                    permissions[
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    ] == true

                val connectGranted =
                    permissions[
                        Manifest.permission.BLUETOOTH_CONNECT
                    ] == true

                if (
                    scanGranted &&
                    advertiseGranted &&
                    connectGranted
                ) {

                    status =
                        "Bluetooth permissions granted"

                } else {

                    status =
                        "Bluetooth permissions denied"
                }

            } else {

                status =
                    "Bluetooth permissions handled by Android"
            }
        }

    /*
     * Request Bluetooth permissions.
     */
    fun requestBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )

        } else {

            status =
                "Bluetooth permissions handled by Android"
        }
    }

    /*
     * --------------------------------
     * TEACHER BROADCAST
     * --------------------------------
     */

    fun startTeacher() {

        if (sessionId.isBlank()) {

            status =
                "Enter a Session ID"

            return
        }

        requestBluetoothPermissions()

        bleManager.startTeacherBroadcast(
            sessionId =
                sessionId.trim()
        )

        isTeacherBroadcasting = true

        status =
            "Teacher broadcasting: ${sessionId.trim()}"
    }

    fun stopTeacher() {

        bleManager.stopTeacherBroadcast()

        isTeacherBroadcasting = false

        status =
            "Teacher broadcast stopped"
    }

    /*
     * --------------------------------
     * TEACHER SCANNING
     * --------------------------------
     *
     * Teacher receives cryptographic
     * keys from students.
     */
    fun startTeacherScanning() {

        requestBluetoothPermissions()

        bleManager.startTeacherScanning { key ->

            /*
             * Avoid duplicate keys.
             */
            if (
                !detectedStudentKeys.contains(key)
            ) {

                detectedStudentKeys.add(key)

                status =
                    "Student detected: $key"
            }
        }

        isTeacherScanning = true

        status =
            "Teacher scanning for students..."
    }

    fun stopTeacherScanning() {

        bleManager.stopTeacherScanning()

        isTeacherScanning = false

        status =
            "Teacher student scanning stopped"
    }

    /*
     * Clear detected students.
     */
    fun clearDetectedStudents() {

        detectedStudentKeys.clear()

        status =
            "Detected student list cleared"
    }

    /*
     * --------------------------------
     * STUDENT BROADCAST
     * --------------------------------
     */

    fun startStudentBroadcast() {

        if (cryptographicKey.isBlank()) {

            status =
                "Enter a Cryptographic Key"

            return
        }

        requestBluetoothPermissions()

        studentKeyAdvertiser.startAdvertising(
            cryptographicKey =
                cryptographicKey.trim()
        )

        isStudentBroadcasting = true

        status =
            "Student broadcasting key: ${cryptographicKey.trim()}"
    }

    fun stopStudentBroadcast() {

        studentKeyAdvertiser.stopAdvertising()

        isStudentBroadcasting = false

        status =
            "Student key broadcast stopped"
    }

    /*
     * --------------------------------
     * CLEANUP
     * --------------------------------
     */

    DisposableEffect(Unit) {

        onDispose {

            bleManager.stopAll()

            studentKeyAdvertiser.stopAdvertising()
        }
    }

    /*
     * --------------------------------
     * UI
     * --------------------------------
     */

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        /*
         * TITLE
         */

        Text(
            text =
                "SmartAttend BLE Test",

            style =
                MaterialTheme.typography.headlineMedium
        )

        /*
         * STATUS
         */

        Text(
            text =
                "Status: $status",

            style =
                MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        /*
         * ==================================
         * TEACHER
         * ==================================
         */

        Text(
            text =
                "Teacher",

            style =
                MaterialTheme.typography.titleLarge
        )

        /*
         * SESSION ID
         */

        OutlinedTextField(
            value =
                sessionId,

            onValueChange = {
                sessionId = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Session ID")
            },

            singleLine = true,

            enabled =
                !isTeacherBroadcasting
        )

        /*
         * TEACHER BROADCAST
         */

        Button(
            onClick = {

                if (isTeacherBroadcasting) {

                    stopTeacher()

                } else {

                    startTeacher()
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                if (isTeacherBroadcasting)
                    "STOP TEACHER BROADCAST"
                else
                    "START TEACHER BROADCAST"
            )
        }

        /*
         * TEACHER SCANNER
         */

        Button(
            onClick = {

                if (isTeacherScanning) {

                    stopTeacherScanning()

                } else {

                    startTeacherScanning()
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                if (isTeacherScanning)
                    "STOP STUDENT SCANNING"
                else
                    "START STUDENT SCANNING"
            )
        }

        /*
         * DETECTED STUDENTS
         */

        Text(
            text =
                "Detected Students: ${detectedStudentKeys.size}",

            style =
                MaterialTheme.typography.titleMedium
        )

        if (detectedStudentKeys.isEmpty()) {

            Text(
                text =
                    "No students detected yet.",

                style =
                    MaterialTheme.typography.bodyMedium
            )

        } else {

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                items(
                    detectedStudentKeys
                ) { key ->

                    Text(
                        text = "• $key",

                        style =
                            MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        /*
         * CLEAR BUTTON
         */

        Button(
            onClick = {
                clearDetectedStudents()
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "CLEAR DETECTED STUDENTS"
            )
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        /*
         * ==================================
         * STUDENT
         * ==================================
         *
         * Kept here for prototype testing.
         *
         * In the actual application this
         * section will be on the student's
         * mobile application.
         */

        Text(
            text =
                "Student",

            style =
                MaterialTheme.typography.titleLarge
        )

        /*
         * CRYPTOGRAPHIC KEY
         */

        OutlinedTextField(
            value =
                cryptographicKey,

            onValueChange = {
                cryptographicKey = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Cryptographic Key")
            },

            singleLine = true,

            enabled =
                !isStudentBroadcasting
        )

        /*
         * STUDENT BROADCAST
         */

        Button(
            onClick = {

                if (isStudentBroadcasting) {

                    stopStudentBroadcast()

                } else {

                    startStudentBroadcast()
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                if (isStudentBroadcasting)
                    "STOP STUDENT BROADCAST"
                else
                    "START STUDENT BROADCAST"
            )
        }
    }
}