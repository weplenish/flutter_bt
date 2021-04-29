package com.weplenish.flutter_bt

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Activity.RESULT_CANCELED
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.DeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContextCompat.getSystemService
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.util.*
import java.util.regex.Pattern

/** FlutterBtPlugin */
class FlutterBtPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var context: Context
    private lateinit var binaryMessenger: BinaryMessenger
    private var activity: Activity? = null
    private val connectedDevices: Set<ConnectedBtDevice> = emptySet()

    private val deviceManager: CompanionDeviceManager by lazy(LazyThreadSafetyMode.NONE) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            activity?.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
        } else {
            TODO("VERSION.SDK_INT < O")
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        binaryMessenger = flutterPluginBinding.binaryMessenger

        methodChannel = MethodChannel(binaryMessenger, METHOD_CHANNEL)
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(binaryMessenger, EVENT_CHANNEL)
        //eventChannel.setStreamHandler(this)

        context = flutterPluginBinding.applicationContext;
    }

    companion object {
        const val METHOD_CHANNEL = "com.weplenish.flutter_bt"
        const val EVENT_CHANNEL = "com.weplenish.flutter_bt.event"

        // channel methods
        const val IS_SUPPORTED = "isSupported"
        const val IS_ENABLED = "isEnabled"
        const val ENABLE_BT = "enableBT"
        const val CONNECT_BT = "connectBT"
        const val CONNECT_BLE = "connectBLE"
        const val WRITE_BT = "writeBT"
        const val CLOSE_SOCKET = "closeSocket"
        const val DISCONNECT = "disconnect"

        // connect bt attributes
        const val SINGLE_DEVICE = "singleDevice"
        const val NAME_PATTERN = "namePattern"
        const val UUID_STRING = "uuidString"

        // socket attributes
        const val SOCKET_UUID = "socketUUID"
        const val WRITE_DATA = "writeData"

        // device
        const val SELECT_BT_REQUEST_CODE = 242
        const val SELECT_BLE_REQUEST_CODE = 342

        // channel response methods
        const val BT_CANNOT_FIND_DEVICE = "BT_CANNOT_FIND_DEVICE"
        const val BT_SELECT_DEVICE_CANCELLED = "BT_SELECT_DEVICE_CANCELLED"
        const val DEVICE_PAIRED = "DEVICE_PAIRED"
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            CONNECT_BT -> when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
                    val singleDevice = call.argument(SINGLE_DEVICE) as Boolean?
                    val namePattern = call.argument(NAME_PATTERN) as String?
                    val uuid = call.argument(UUID_STRING) as String?

                    val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder().apply {
                        if (namePattern != null) {
                            setNamePattern(Pattern.compile(namePattern))
                        }
                        if (uuid != null) {
                            addServiceUuid(ParcelUuid(UUID.fromString(uuid)), null)
                        }
                    }.build()

                    connectToDevice(singleDevice, deviceFilter, SELECT_BT_REQUEST_CODE)

                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                    TODO("Add Android support pre O")
                }
            }
            CONNECT_BLE -> when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O -> {
                    val singleDevice = call.argument(SINGLE_DEVICE) as Boolean?
                    val namePattern = call.argument(NAME_PATTERN) as String?
                    val uuid = call.argument(UUID_STRING) as String?

                    val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder().apply {
                        if (namePattern != null) {
                            setNamePattern(Pattern.compile(namePattern))
                        }
                        if(uuid != null){
                            setScanFilter(android.bluetooth.le.ScanFilter.Builder().apply {
                                setServiceUuid(ParcelUuid(UUID.fromString(uuid)), null)
                            })
                        }
                    }.build()
                    connectToDevice(singleDevice, deviceFilter, SELECT_BLE_REQUEST_CODE)
                    result.success(true)
                }
                else -> {
                    result.notImplemented()
                    TODO("Add Android support pre O")
                }
            }
            else -> result.notImplemented()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun connectToDevice(singleDevice: Boolean?, deviceFilter: DeviceFilter, requestCode: Int) {
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .setSingleDevice(singleDevice ?: true)
                .build()

        deviceManager.associate(pairingRequest,
                object : CompanionDeviceManager.Callback() {
                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        activity?.startIntentSenderForResult(chooserLauncher,
                                requestCode, null, 0, 0, 0)
                    }

                    override fun onFailure(error: CharSequence?) {
                        Handler(Looper.getMainLooper()).post {
                            methodChannel.invokeMethod(BT_CANNOT_FIND_DEVICE, error)
                        }
                    }
                }, null)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            SELECT_BT_REQUEST_CODE -> when (resultCode) {
                RESULT_OK -> {
                    val pairedDevice: BluetoothDevice? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    pairedDevice?.let {
                        it.createBond()
                        it.fetchUuidsWithSdp()

                        Handler(Looper.getMainLooper()).post {
                            connectedDevices.plus(ConnectedBtDevice(pairedDevice, binaryMessenger))
                            methodChannel.invokeMethod(DEVICE_PAIRED, ConnectedBtDevice.deviceToMap(pairedDevice))
                        }
                    }
                    return true
                }
                RESULT_CANCELED -> {
                    Handler(Looper.getMainLooper()).post {
                        methodChannel.invokeMethod(BT_SELECT_DEVICE_CANCELLED, null)
                    }
                    return true
                }
            }
            SELECT_BLE_REQUEST_CODE -> when (resultCode) {
                RESULT_OK -> {
                    val pairedDevice: android.bluetooth.le.ScanResult? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    pairedDevice?.device?.let {
                        it.createBond()
                        it.fetchUuidsWithSdp()

                        Handler(Looper.getMainLooper()).post {
                            connectedDevices.plus(ConnectedBtDevice(pairedDevice, binaryMessenger))
                            methodChannel.invokeMethod(DEVICE_PAIRED, ConnectedBtDevice.deviceToMap(pairedDevice))
                        }
                    }
                    return true
                }
                RESULT_CANCELED -> {
                    Handler(Looper.getMainLooper()).post {
                        methodChannel.invokeMethod(BT_SELECT_DEVICE_CANCELLED, null)
                    }
                    return true
                }
            }
        }
        return false
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null;
    }
}
