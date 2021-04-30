package com.weplenish.flutter_bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class ConnectedBtDevice(private val device: BluetoothDevice, private val binaryMessenger: BinaryMessenger) : MethodChannel.MethodCallHandler {
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val pairedDevice: BluetoothDevice? by lazy(LazyThreadSafetyMode.NONE) {
        bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == device.address }
    }

    private val methodChannel: MethodChannel = MethodChannel(binaryMessenger, "com.weplenish.flutter_bt.${device.address}")
    private val eventChannel: EventChannel = EventChannel(binaryMessenger, "com.weplenish.flutter_bt.event.${device.address}")

    companion object {
        const val GET_UUIDS = "GET_UUIDS"
        const val DEVICE_INFO = "DEVICE_INFO"

        // Device Map Attributes
        const val ADDRESS = "address"
        const val NAME = "name"
        const val UUIDS = "uuids"
        const val BOND_STATE = "bondState"
        const val TYPE = "type"
        const val BLUETOOTH_CLASS = "bluetoothClass"
        const val DEVICE_CLASS = "deviceClass"
        const val MAJOR_DEVICE_CLASS = "majorDeviceClass"

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        fun deviceToMap(device: BluetoothDevice?) : Map<String, Any?>? {
            return device?.let {
                mapOf(
                        ADDRESS to it.address,
                        NAME to it.name,
                        UUIDS to it.uuids?.map { uuidParcel -> uuidParcel.uuid.toString() },
                        BOND_STATE to it.bondState,
                        TYPE to it.type,
                        BLUETOOTH_CLASS to it.bluetoothClass.let { btClass ->
                            mapOf(
                                    DEVICE_CLASS to btClass.deviceClass,
                                    MAJOR_DEVICE_CLASS to btClass.majorDeviceClass
                            )
                        }
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when(call.method){
            GET_UUIDS -> {
                result.success(pairedDevice?.uuids?.map { uuidParcel -> uuidParcel.uuid.toString() })
            }
            else -> {
                result.notImplemented()
            }
        }
    }
}