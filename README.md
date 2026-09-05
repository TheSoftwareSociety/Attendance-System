# SmartAttend BLE Prototype

This branch contains the BLE-based attendance prototype for the
Attendance System project.

## Overview

The SmartAttend BLE prototype uses Bluetooth Low Energy (BLE) to allow
a teacher's device to broadcast a classroom session ID.

Student devices scan for the BLE advertisement and detect the session ID.
This provides the basic communication mechanism required for automatic
attendance marking.

## Current Prototype

The current prototype demonstrates:

- Teacher device acting as a BLE advertiser
- Student device acting as a BLE scanner
- Broadcasting of a classroom Session ID
- Detection of the Session ID by student devices
- Multiple student devices being able to detect the same session
- Android BLE permissions and scanning
- Shared BLE logic using Kotlin Multiplatform

### Example

Teacher broadcasts:

    SMARTATTEND:BCS701

Student device receives:

    Session ID: BCS701

## Current Scope

At this stage, the prototype focuses on verifying the BLE communication
between teacher and multiple student devices.

The current prototype does **not** enforce:

- RSSI-based attendance validation
- Distance/range restrictions
- Time-window restrictions
- Door or classroom boundary detection
- Database-based attendance marking

These features can be integrated in later stages.

## Project Structure

    androidApp/
        Android application

    sharedLogic/
        Shared BLE logic
        Android BLE advertiser
        Android BLE scanner
        BLE packet and state handling

    sharedUI/
        Shared UI components

    iosApp/
        iOS application

## BLE Flow

    Teacher Device
          |
          | BLE Advertisement
          | Session ID
          v
    Student Device 1
    Student Device 2
    Student Device 3
          |
          v
    Session ID Detected

Multiple student devices can listen to the same BLE advertisement.

## Development Branch

This branch is dedicated to BLE development.

Branch:

    ble-development

The stable/integrated project is maintained separately on the
`main` branch.

## Status

BLE communication prototype implemented and tested between:

- Android teacher device
- Android student device

The current milestone is successful Session ID broadcasting and
detection.
