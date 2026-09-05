import Foundation
import SharedLogic
import Combine

final class SmartAttendBleViewModel: ObservableObject {

    // MARK: - BLE Components

    private let bleAdvertiser = IosStudentKeyAdvertiser()
    private let bleScanner = IosStudentKeyScanner()

    // MARK: - Student

    @Published var cryptographicKey: String = "STUDENT_KEY_001"

    @Published var isBroadcasting: Bool = false

    // MARK: - Teacher

    @Published var isScanning: Bool = false

    @Published var detectedStudents: [DetectedStudent] = []

    // MARK: - Status

    @Published var statusMessage: String = "BLE Ready"

    // MARK: - Student Broadcasting

    func startStudentBroadcast() {

        let key = cryptographicKey
            .trimmingCharacters(
                in: .whitespacesAndNewlines
            )

        guard !key.isEmpty else {

            statusMessage =
                "Cryptographic key cannot be empty"

            return
        }

        print(
            "SmartAttend iOS: Starting student BLE"
        )

        print(
            "SmartAttend iOS: Key = \(key)"
        )

        bleAdvertiser.startAdvertising(
            cryptographicKey: key
        )

        isBroadcasting = true

        statusMessage =
            "Broadcasting student key"
    }

    func stopStudentBroadcast() {

        print(
            "SmartAttend iOS: Stopping student BLE"
        )

        bleAdvertiser.stopAdvertising()

        isBroadcasting = false

        statusMessage =
            "Student broadcast stopped"
    }

    // MARK: - Teacher Scanning

    func startTeacherScanning() {

        if isScanning {

            statusMessage =
                "Student scanner already running"

            return
        }

        print(
            "SmartAttend iOS: Starting student scanner"
        )

        bleScanner.startScanning { [weak self] key, rssi in

            guard let self = self else {
                return
            }

            self.handleDetectedStudent(
                key: key,
                rssi: rssi
            )
        }

        isScanning = true

        statusMessage =
            "Scanning for student keys..."
    }

    func stopTeacherScanning() {

        print(
            "SmartAttend iOS: Stopping student scanner"
        )

        bleScanner.stopScanning()

        isScanning = false

        statusMessage =
            "Student scanning stopped"
    }

    // MARK: - Student Detection

    private func handleDetectedStudent(
        key: String,
        rssi: Int32
    ) {

        print(
            "SmartAttend iOS: Student detected"
        )

        print(
            "SmartAttend iOS: Key = \(key)"
        )

        print(
            "SmartAttend iOS: RSSI = \(rssi)"
        )

        DispatchQueue.main.async {

            /*
             * Avoid duplicate students.
             *
             * The same BLE advertisement may
             * be received many times.
             */
            if self.detectedStudents.contains(
                where: { $0.key == key }
            ) {

                return
            }

            let student =
                DetectedStudent(
                    key: key,
                    rssi: Int(rssi)
                )

            self.detectedStudents.append(
                student
            )

            self.statusMessage =
                "Student detected: \(key)"
        }
    }

    // MARK: - Clear Students

    func clearDetectedStudents() {

        detectedStudents.removeAll()

        statusMessage =
            "Detected student list cleared"
    }

    // MARK: - Stop Everything

    func stopAll() {

        print(
            "SmartAttend iOS: Stopping all BLE operations"
        )

        bleAdvertiser.stopAdvertising()

        bleScanner.stopScanning()

        isBroadcasting = false

        isScanning = false

        statusMessage =
            "BLE stopped"
    }

    // MARK: - Deinitialization

    deinit {

        bleAdvertiser.stopAdvertising()

        bleScanner.stopScanning()
    }
}

// MARK: - Detected Student

struct DetectedStudent: Identifiable {

    let id = UUID()

    let key: String

    let rssi: Int
}