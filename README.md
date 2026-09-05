\# SmartAttend BLE Prototype



This branch contains the BLE-based attendance prototype for the

Attendance System project.



\## Overview



The SmartAttend BLE prototype uses Bluetooth Low Energy (BLE) to allow

a teacher's device to broadcast a classroom session ID.



Student devices scan for the BLE advertisement and detect the session ID.

This provides the basic communication mechanism required for automatic

attendance marking.



\## Current Prototype



The current prototype demonstrates:



\- Teacher device acting as a BLE advertiser

\- Student device acting as a BLE scanner

\- Broadcasting of a classroom Session ID

\- Detection of the Session ID by student devices

\- Multiple student devices being able to detect the same session

\- Android BLE permissions and scanning

\- Shared BLE logic using Kotlin Multiplatform



\### Example



Teacher broadcasts:



&#x20;   SMARTATTEND:BCS701



Student device receives:



&#x20;   Session ID: BCS701



\## Current Scope



At this stage, the prototype focuses on verifying the BLE communication

between teacher and multiple student devices.



The current prototype does \*\*not\*\* enforce:



\- RSSI-based attendance validation

\- Distance/range restrictions

\- Time-window restrictions

\- Door or classroom boundary detection

\- Database-based attendance marking



These features can be integrated in later stages.



\## Project Structure



&#x20;   androidApp/

&#x20;       Android application



&#x20;   sharedLogic/

&#x20;       Shared BLE logic

&#x20;       Android BLE advertiser

&#x20;       Android BLE scanner

&#x20;       BLE packet and state handling



&#x20;   sharedUI/

&#x20;       Shared UI components



&#x20;   iosApp/

&#x20;       iOS application



\## BLE Flow



&#x20;   Teacher Device

&#x20;         |

&#x20;         | BLE Advertisement

&#x20;         | Session ID

&#x20;         v

&#x20;   Student Device 1

&#x20;   Student Device 2

&#x20;   Student Device 3

&#x20;         |

&#x20;         v

&#x20;   Session ID Detected



Multiple student devices can listen to the same BLE advertisement.



\## Development Branch



This branch is dedicated to BLE development.



Branch:



&#x20;   ble-development



The stable/integrated project is maintained separately on the

`main` branch.



\## Status



BLE communication prototype implemented and tested between:



\- Android teacher device

\- Android student device



The current milestone is successful Session ID broadcasting and

detection.

