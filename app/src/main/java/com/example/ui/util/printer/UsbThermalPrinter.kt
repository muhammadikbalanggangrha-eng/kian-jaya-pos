package com.example.ui.util.printer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ConnectedUsbPrinter(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String,
    val productName: String,
    val hasPermission: Boolean,
    val usbDevice: UsbDevice
)

object UsbThermalPrinter {

    const val ACTION_USB_PERMISSION = "com.example.USB_PRINTER_PERMISSION"

    fun getUsbManager(context: Context): UsbManager? {
        return context.getSystemService(Context.USB_SERVICE) as? UsbManager
    }

    /**
     * Lists connected USB devices that are either declared as Printer class
     * or have a Bulk Transfer OUT endpoint (used by thermal receipt printers).
     */
    fun getConnectedPrinters(context: Context): List<ConnectedUsbPrinter> {
        val usbManager = getUsbManager(context) ?: return emptyList()
        val deviceList = usbManager.deviceList ?: return emptyList()

        val printers = mutableListOf<ConnectedUsbPrinter>()

        for ((_, device) in deviceList) {
            val isPrinter = isPrinterDevice(device)
            if (isPrinter) {
                val hasPerm = usbManager.hasPermission(device)
                val mfg = try {
                    device.manufacturerName ?: ""
                } catch (_: Exception) { "" }
                val prod = try {
                    device.productName ?: ""
                } catch (_: Exception) { "" }

                val displayName = when {
                    prod.isNotBlank() -> prod
                    mfg.isNotBlank() -> "$mfg (VID: ${device.vendorId})"
                    else -> "USB Printer (VID: ${device.vendorId}, PID: ${device.productId})"
                }

                printers.add(
                    ConnectedUsbPrinter(
                        deviceName = displayName,
                        vendorId = device.vendorId,
                        productId = device.productId,
                        manufacturerName = mfg,
                        productName = prod,
                        hasPermission = hasPerm,
                        usbDevice = device
                    )
                )
            }
        }

        return printers
    }

    /**
     * Identifies if a USB device is a printer by checking device class or interface endpoints.
     */
    private fun isPrinterDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) {
            return true
        }

        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                return true
            }
            // Also check for vendor specific or custom interface with Bulk OUT endpoint
            for (j in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(j)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                    endpoint.direction == UsbConstants.USB_DIR_OUT
                ) {
                    return true
                }
            }
        }
        return false
    }

    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = getUsbManager(context) ?: return
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(ACTION_USB_PERMISSION)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager.requestPermission(device, pendingIntent)
    }

    /**
     * Prints byte array directly to a USB thermal printer.
     */
    suspend fun printBytes(
        context: Context,
        device: UsbDevice,
        bytes: ByteArray
    ): PrinterResult = withContext(Dispatchers.IO) {
        val usbManager = getUsbManager(context)
            ?: return@withContext PrinterResult.Error("USB Service tidak tersedia di perangkat ini.")

        if (!usbManager.hasPermission(device)) {
            requestUsbPermission(context, device)
            return@withContext PrinterResult.Error("Izin USB belum diberikan. Silakan konfirmasi dialog izin USB pada layar.")
        }

        var connection: UsbDeviceConnection? = null
        var claimedInterface: UsbInterface? = null

        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext PrinterResult.Error("Gagal membuka koneksi USB ke printer.")

            // Find interface and bulk OUT endpoint
            var endpointOut: UsbEndpoint? = null

            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                for (j in 0 until usbInterface.endpointCount) {
                    val endpoint = usbInterface.getEndpoint(j)
                    if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                        endpoint.direction == UsbConstants.USB_DIR_OUT
                    ) {
                        claimedInterface = usbInterface
                        endpointOut = endpoint
                        break
                    }
                }
                if (endpointOut != null) break
            }

            if (claimedInterface == null || endpointOut == null) {
                return@withContext PrinterResult.Error("Endpoint transfer data printer USB tidak ditemukan.")
            }

            val claimed = connection.claimInterface(claimedInterface, true)
            if (!claimed) {
                return@withContext PrinterResult.Error("Gagal mengklaim interface printer USB.")
            }

            // Transfer byte array via bulkTransfer (chunked if large)
            val chunkSize = 4096
            var offset = 0
            while (offset < bytes.size) {
                val length = (bytes.size - offset).coerceAtMost(chunkSize)
                val chunk = bytes.copyOfRange(offset, offset + length)
                val transferred = connection.bulkTransfer(endpointOut, chunk, length, 5000)
                if (transferred < 0) {
                    return@withContext PrinterResult.Error("Gagal mentransfer data cetak ke printer USB (Kode transfer: $transferred).")
                }
                offset += length
            }

            Thread.sleep(300)
            PrinterResult.Success("Struk berhasil dicetak via USB Printer!")
        } catch (e: Exception) {
            PrinterResult.Error("Gagal mencetak via USB: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}")
        } finally {
            try {
                if (claimedInterface != null) {
                    connection?.releaseInterface(claimedInterface)
                }
                connection?.close()
            } catch (_: Exception) {}
        }
    }
}
