import SwiftUI

struct ContentView: View {

    @StateObject private var viewModel =
        SmartAttendBleViewModel()

    var body: some View {

        NavigationStack {

            ScrollView {

                VStack(spacing: 20) {

                    Text("SmartAttend")
                        .font(.largeTitle)
                        .fontWeight(.bold)

                    Text("BLE Test Console")
                        .font(.title3)
                        .foregroundColor(.secondary)

                    Divider()

                    // MARK: Student

                    VStack(alignment: .leading, spacing: 12) {

                        Text("Student Mode")
                            .font(.headline)

                        TextField(
                            "Cryptographic Key",
                            text: $viewModel.cryptographicKey
                        )
                        .textFieldStyle(.roundedBorder)
                        .disabled(
                            viewModel.isBroadcasting
                        )

                        Button {

                            if viewModel.isBroadcasting {

                                viewModel.stopStudentBroadcast()

                            } else {

                                viewModel.startStudentBroadcast()
                            }

                        } label: {

                            Text(
                                viewModel.isBroadcasting
                                ? "Stop Student Broadcast"
                                : "Start Student Broadcast"
                            )
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                    }

                    Divider()

                    // MARK: Teacher

                    VStack(alignment: .leading, spacing: 12) {

                        Text("Teacher Mode")
                            .font(.headline)

                        Button {

                            if viewModel.isScanning {

                                viewModel.stopTeacherScanning()

                            } else {

                                viewModel.startTeacherScanning()
                            }

                        } label: {

                            Text(
                                viewModel.isScanning
                                ? "Stop Student Scanning"
                                : "Start Student Scanning"
                            )
                            .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)

                        Button {

                            viewModel.clearDetectedStudents()

                        } label: {

                            Text("Clear Detected Students")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .disabled(
                            viewModel.detectedStudents.isEmpty
                        )
                    }

                    Divider()

                    // MARK: Status

                    VStack(alignment: .leading, spacing: 8) {

                        Text("Status")
                            .font(.headline)

                        Text(viewModel.statusMessage)
                            .foregroundColor(.secondary)
                    }
                    .frame(
                        maxWidth: .infinity,
                        alignment: .leading
                    )

                    // MARK: Detected Students

                    VStack(alignment: .leading, spacing: 10) {

                        HStack {

                            Text("Detected Students")
                                .font(.headline)

                            Spacer()

                            Text(
                                "\(viewModel.detectedStudents.count)"
                            )
                            .fontWeight(.bold)
                        }

                        if viewModel.detectedStudents.isEmpty {

                            Text("No students detected")
                                .foregroundColor(.secondary)

                        } else {

                            ForEach(
                                viewModel.detectedStudents
                            ) { student in

                                VStack(
                                    alignment: .leading,
                                    spacing: 4
                                ) {

                                    Text(student.key)
                                        .font(.body)
                                        .fontWeight(.medium)

                                    Text(
                                        "RSSI: \(student.rssi) dBm"
                                    )
                                    .font(.caption)
                                    .foregroundColor(
                                        .secondary
                                    )
                                }
                                .frame(
                                    maxWidth: .infinity,
                                    alignment: .leading
                                )
                                .padding()
                                .background(
                                    Color.gray.opacity(0.1)
                                )
                                .cornerRadius(10)
                            }
                        }
                    }

                    Spacer()
                }
                .padding()
            }
            .navigationTitle("SmartAttend BLE")
        }
        .onDisappear {

            viewModel.stopAll()
        }
    }
}

#Preview {
    ContentView()
}