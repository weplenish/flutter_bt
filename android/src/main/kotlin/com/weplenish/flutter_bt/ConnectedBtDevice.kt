package com.weplenish.flutter_bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.annotation.RequiresApi

class ConnectedBtDevice {
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

        fun deviceUuidToList(device: BluetoothDevice) : List<String>? {
            return device.uuids?.map { uuidParcel -> uuidParcel.uuid.toString() }
        }

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        fun deviceToMap(device: BluetoothDevice?) : Map<String, Any?>? {
            return device?.let {
                mapOf(
                        ADDRESS to it.address,
                        NAME to it.name,
                        UUIDS to deviceUuidToList(it),
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
}