package com.example.ui.util.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

data class PairedBluetoothPrinter(
    val name: String,
    val address: String,
    val isLikelyPrinter: Boolean
)

sealed class PrinterResult {
    data class Success(val message: String) : PrinterResult()
    data class Error(val errorMessage: String) : PrinterResult()
    data object PermissionRequired : PrinterResult()
    data object BluetoothDisabled : PrinterResult()
}

object BluetoothThermalPrinter {

    // Standard Bluetooth Serial Port Profile (SPP) UUID used by 99% of thermal printers
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val adapter = getBluetoothAdapter(context) ?: return false
        return adapter.isEnabled
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(context: Context): List<PairedBluetoothPrinter> {
        val adapter = getBluetoothAdapter(context) ?: return emptyList()
        if (!hasBluetoothPermission(context) || !adapter.isEnabled) {
            return emptyList()
        }

        return try {
            val bondedDevices = adapter.bondedDevices ?: return emptyList()
            bondedDevices.map { device ->
                val name = device.name ?: "Perangkat Tidak Dikenal"
                val address = device.address ?: ""
                val lowerName = name.lowercase()
                val isLikelyPrinter = lowerName.contains("printer") ||
                        lowerName.contains("pos") ||
                        lowerName.contains("rpp") ||
                        lowerName.contains("mtp") ||
                        lowerName.contains("eppos") ||
                        lowerName.contains("panda") ||
                        lowerName.contains("thermal") ||
                        lowerName.contains("bt-") ||
                        lowerName.contains("pt-")
                PairedBluetoothPrinter(
                    name = name,
                    address = address,
                    isLikelyPrinter = isLikelyPrinter
                )
            }.sortedByDescending { it.isLikelyPrinter }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(
        context: Context,
        deviceAddress: String,
        bytes: ByteArray
    ): PrinterResult = withContext(Dispatchers.IO) {
        val adapter = getBluetoothAdapter(context)
            ?: return@withContext PrinterResult.Error("Perangkat tidak mendukung Bluetooth.")

        if (!hasBluetoothPermission(context)) {
            return@withContext PrinterResult.PermissionRequired
        }

        if (!adapter.isEnabled) {
            return@withContext PrinterResult.BluetoothDisabled
        }

        var socket: BluetoothSocket? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                ?: return@withContext PrinterResult.Error("Perangkat Bluetooth ($deviceAddress) tidak ditemukan.")

            // Cancel discovery to optimize bandwidth and connection speed
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (_: Exception) {}

            // Create Rfcomm SPP socket
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            val outputStream = socket.outputStream
            outputStream.write(bytes)
            outputStream.flush()

            // Small delay to allow printer buffer to process before socket close
            Thread.sleep(400)

            PrinterResult.Success("Struk berhasil dikirim ke printer Bluetooth ${device.name ?: deviceAddress}.")
        } catch (e: IOException) {
            PrinterResult.Error("Gagal terhubung ke printer Bluetooth: ${e.localizedMessage ?: "Koneksi terputus"}. Pastikan printer menyala dan sudah di-pairing.")
        } catch (e: Exception) {
            PrinterResult.Error("Terjadi kesalahan: ${e.localizedMessage ?: "Tidak diketahui"}")
        } finally {
            try {
                socket?.close()
            } catch (_: IOException) {}
        }
    }
}
