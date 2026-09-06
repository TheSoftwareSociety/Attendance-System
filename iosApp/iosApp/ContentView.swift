import CoreBluetooth
import SwiftUI

struct ContentView: View {
    @StateObject private var scanner = BLEScanner()

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(scanner.statusTitle)
                                .font(.headline)
                            Text(scanner.statusMessage)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }

                        Spacer()

                        Image(systemName: scanner.isScanning ? "dot.radiowaves.left.and.right" : "antenna.radiowaves.left.and.right.slash")
                            .font(.title2)
                            .foregroundStyle(scanner.isScanning ? .green : .secondary)
                    }
                    .padding(.vertical, 6)
                }

                Section {
                    Toggle("Simulation Mode", isOn: $scanner.isSimulationMode)

                    Button {
                        scanner.toggleScanning()
                    } label: {
                        Label(scanner.isScanning ? "Stop Scan" : "Start Scan", systemImage: scanner.isScanning ? "stop.fill" : "play.fill")
                    }
                    .disabled(!scanner.canScan)
                }

                Section("Discovered Devices") {
                    if scanner.devices.isEmpty {
                        ContentUnavailableView(
                            "No Devices",
                            systemImage: "antenna.radiowaves.left.and.right",
                            description: Text(scanner.emptyStateMessage)
                        )
                    } else {
                        ForEach(scanner.devices) { device in
                            DeviceRow(device: device)
                        }
                    }
                }
            }
            .navigationTitle("SmartAttend BLE")
        }
    }
}

struct DeviceRow: View {
    let device: BLEDevice

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "sensor.tag.radiowaves.forward")
                .font(.title3)
                .foregroundStyle(.blue)
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 4) {
                Text(device.name)
                    .font(.headline)
                Text(device.identifier)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text("\(device.rssi) dBm")
                    .font(.subheadline.monospacedDigit())
                Text(device.signalDescription)
                    .font(.caption)
                    .foregroundStyle(device.signalColor)
            }
        }
        .padding(.vertical, 4)
    }
}

struct BLEDevice: Identifiable, Equatable {
    let id: UUID
    var name: String
    var identifier: String
    var rssi: Int
    var lastSeen: Date

    var signalDescription: String {
        switch rssi {
        case (-59)...:
            return "Strong"
        case (-79)...(-60):
            return "Good"
        default:
            return "Weak"
        }
    }

    var signalColor: Color {
        switch rssi {
        case (-59)...:
            return .green
        case (-79)...(-60):
            return .orange
        default:
            return .red
        }
    }
}

final class BLEScanner: NSObject, ObservableObject, CBCentralManagerDelegate {
    @Published var devices: [BLEDevice] = []
    @Published var isScanning = false
    @Published var isSimulationMode = true {
        didSet {
            stopScanning()
            devices.removeAll()
            statusMessage = isSimulationMode ? "Use simulated attendance beacons in the simulator." : "Use a physical iPhone to scan nearby BLE devices."
        }
    }
    @Published private(set) var statusTitle = "Ready"
    @Published private(set) var statusMessage = "Use simulated attendance beacons in the simulator."

    private var centralManager: CBCentralManager?
    private var simulationTimer: Timer?

    var canScan: Bool {
        isSimulationMode || centralManager?.state == .poweredOn
    }

    var emptyStateMessage: String {
        isScanning ? "Scanning for nearby BLE attendance beacons." : "Start scanning to discover devices."
    }

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }

    func toggleScanning() {
        isScanning ? stopScanning() : startScanning()
    }

    func startScanning() {
        devices.removeAll()

        if isSimulationMode {
            startSimulation()
            return
        }

        guard centralManager?.state == .poweredOn else {
            statusTitle = "Bluetooth Unavailable"
            statusMessage = bluetoothStateMessage
            return
        }

        isScanning = true
        statusTitle = "Scanning"
        statusMessage = "Looking for nearby BLE devices."
        centralManager?.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
    }

    func stopScanning() {
        simulationTimer?.invalidate()
        simulationTimer = nil
        centralManager?.stopScan()
        isScanning = false
        statusTitle = "Ready"
        statusMessage = isSimulationMode ? "Use simulated attendance beacons in the simulator." : bluetoothStateMessage
    }

    @objc func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if !isSimulationMode {
            statusTitle = central.state == .poweredOn ? "Ready" : "Bluetooth Unavailable"
            statusMessage = bluetoothStateMessage
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        let fallbackName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let name = peripheral.name ?? fallbackName ?? "Unknown Device"
        let device = BLEDevice(
            id: peripheral.identifier,
            name: name,
            identifier: peripheral.identifier.uuidString,
            rssi: RSSI.intValue,
            lastSeen: Date()
        )

        upsert(device)
    }

    private func startSimulation() {
        isScanning = true
        statusTitle = "Simulating"
        statusMessage = "Generating sample SmartAttend BLE beacons."

        simulationTimer?.invalidate()
        simulationTimer = Timer.scheduledTimer(withTimeInterval: 1.2, repeats: true) { [weak self] _ in
            self?.addSimulatedDevice()
        }
        addSimulatedDevice()
    }

    private func addSimulatedDevice() {
        let sampleDevices = [
            ("SmartAttend Beacon A", UUID(uuidString: "A918812A-42D2-4C88-91D4-668676D36C35")!),
            ("Classroom Door Beacon", UUID(uuidString: "B67B23AA-A36D-4713-83D3-E93FB5D7AE65")!),
            ("Lab Entry Beacon", UUID(uuidString: "C51662C5-9E92-4B55-90BE-D9CBF86C326B")!)
        ]

        guard let sample = sampleDevices.randomElement() else {
            return
        }

        let rssi = Int.random(in: -88 ... -42)
        let device = BLEDevice(
            id: sample.1,
            name: sample.0,
            identifier: sample.1.uuidString,
            rssi: rssi,
            lastSeen: Date()
        )

        upsert(device)
    }

    private func upsert(_ device: BLEDevice) {
        if let index = devices.firstIndex(where: { $0.id == device.id }) {
            devices[index] = device
        } else {
            devices.append(device)
        }

        devices.sort { $0.rssi > $1.rssi }
    }

    private var bluetoothStateMessage: String {
        switch centralManager?.state {
        case .poweredOn:
            return "Bluetooth is on and ready."
        case .poweredOff:
            return "Turn on Bluetooth to scan for devices."
        case .unauthorized:
            return "Allow Bluetooth access in Settings to scan."
        case .unsupported:
            return "BLE scanning is not supported on this device."
        case .resetting:
            return "Bluetooth is resetting. Try again shortly."
        case .unknown, nil:
            return "Checking Bluetooth status."
        @unknown default:
            return "Bluetooth is unavailable."
        }
    }
}

#Preview {
    ContentView()
}
