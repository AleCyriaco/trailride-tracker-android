package com.trailride.tracker.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class SensorType(val value: String) {
    HR("hr"),
    CADENCE("cadence"),
    POWER("power"),
}

data class DiscoveredDevice(
    val address: String,
    val name: String?,
    val device: BluetoothDevice,
)

@SuppressLint("MissingPermission")
class BleSensorService(private val context: Context) {

    companion object {
        val HR_SERVICE_UUID: UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        val HR_CHAR_UUID: UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
        val CSC_SERVICE_UUID: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val CSC_CHAR_UUID: UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb")
        val POWER_SERVICE_UUID: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val POWER_CHAR_UUID: UUID = UUID.fromString("00002A63-0000-1000-8000-00805f9b34fb")
        val CLIENT_CHAR_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val SCAN_TIMEOUT_MS = 8000L
    }

    private val _discovered = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discovered: StateFlow<List<DiscoveredDevice>> = _discovered.asStateFlow()

    private val _connectedAddresses = MutableStateFlow<Set<String>>(emptySet())
    val connectedAddresses: StateFlow<Set<String>> = _connectedAddresses.asStateFlow()

    private val _latestReadings = MutableStateFlow<Map<SensorType, Double>>(emptyMap())
    val latestReadings: StateFlow<Map<SensorType, Double>> = _latestReadings.asStateFlow()

    var onSensorReading: ((SensorType, Double) -> Unit)? = null

    private var scanner: BluetoothLeScanner? = null
    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val handler = Handler(Looper.getMainLooper())

    // CSC state for cadence RPM
    private var lastCrankRevs: Int = 0
    private var lastCrankEventTime: Int = 0
    private var hasPreviousCrankData = false

    fun startScan() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return
        scanner = adapter.bluetoothLeScanner ?: return

        _discovered.value = emptyList()

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(CSC_SERVICE_UUID)).build(),
            ScanFilter.Builder().setServiceUuid(ParcelUuid(POWER_SERVICE_UUID)).build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner?.startScan(filters, settings, scanCallback)

        handler.postDelayed({ stopScan() }, SCAN_TIMEOUT_MS)
    }

    fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    fun connect(device: BluetoothDevice) {
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnectAll() {
        for (gatt in connectedGatts.values) {
            gatt.disconnect()
            gatt.close()
        }
        connectedGatts.clear()
        _connectedAddresses.value = emptySet()
        _latestReadings.value = emptyMap()
        hasPreviousCrankData = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val current = _discovered.value
            if (current.none { it.address == device.address }) {
                _discovered.value = current + DiscoveredDevice(
                    address = device.address,
                    name = device.name,
                    device = device,
                )
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedGatts[gatt.device.address] = gatt
                    _connectedAddresses.value = connectedGatts.keys.toSet()
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedGatts.remove(gatt.device.address)
                    _connectedAddresses.value = connectedGatts.keys.toSet()
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            for (service in gatt.services) {
                val charUuid = when (service.uuid) {
                    HR_SERVICE_UUID -> HR_CHAR_UUID
                    CSC_SERVICE_UUID -> CSC_CHAR_UUID
                    POWER_SERVICE_UUID -> POWER_CHAR_UUID
                    else -> continue
                }
                val char = service.getCharacteristic(charUuid) ?: continue
                gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(CLIENT_CHAR_CONFIG)
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(it)
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value ?: return
            if (data.isEmpty()) return

            when (characteristic.uuid) {
                HR_CHAR_UUID -> parseHeartRate(data)
                CSC_CHAR_UUID -> parseCadence(data)
                POWER_CHAR_UUID -> parsePower(data)
            }
        }
    }

    // --- Parsing (identical to iOS) ---

    private fun parseHeartRate(data: ByteArray) {
        if (data.size < 2) return
        val flags = data[0].toInt() and 0xFF
        val hr16Bit = (flags and 0x01) != 0
        val value: Double = if (hr16Bit && data.size >= 3) {
            ((data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)).toDouble()
        } else {
            (data[1].toInt() and 0xFF).toDouble()
        }
        handler.post {
            _latestReadings.value = _latestReadings.value + (SensorType.HR to value)
            onSensorReading?.invoke(SensorType.HR, value)
        }
    }

    private fun parseCadence(data: ByteArray) {
        if (data.isEmpty()) return
        val flags = data[0].toInt() and 0xFF
        val hasCrankData = (flags and 0x02) != 0
        if (!hasCrankData) return

        var offset = 1
        // Skip wheel data if present
        if ((flags and 0x01) != 0) offset += 6
        if (data.size < offset + 4) return

        val crankRevs = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        val crankEventTime = (data[offset + 2].toInt() and 0xFF) or ((data[offset + 3].toInt() and 0xFF) shl 8)

        if (hasPreviousCrankData) {
            val deltaRevs = (crankRevs - lastCrankRevs) and 0xFFFF
            val deltaTime = (crankEventTime - lastCrankEventTime) and 0xFFFF

            if (deltaTime > 0 && deltaRevs > 0) {
                val seconds = deltaTime.toDouble() / 1024.0
                val rpm = (deltaRevs.toDouble() / seconds) * 60.0
                if (rpm > 0 && rpm < 250) {
                    handler.post {
                        _latestReadings.value = _latestReadings.value + (SensorType.CADENCE to rpm)
                        onSensorReading?.invoke(SensorType.CADENCE, rpm)
                    }
                }
            }
        }

        lastCrankRevs = crankRevs
        lastCrankEventTime = crankEventTime
        hasPreviousCrankData = true
    }

    private fun parsePower(data: ByteArray) {
        if (data.size < 4) return
        // Bytes 2-3: instantaneous power (signed 16-bit LE)
        var raw = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
        if (raw and 0x8000 != 0) raw -= 0x10000
        val watts = raw.toDouble()

        handler.post {
            _latestReadings.value = _latestReadings.value + (SensorType.POWER to watts)
            onSensorReading?.invoke(SensorType.POWER, watts)
        }
    }
}
