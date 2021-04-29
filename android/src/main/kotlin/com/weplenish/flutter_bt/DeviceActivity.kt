//package com.weplenish.flutter_bt
//
//import android.app.Activity
//import android.bluetooth.BluetoothDevice
//import android.bluetooth.BluetoothSocket
//import android.companion.AssociationRequest
//import android.companion.BluetoothDeviceFilter
//import android.companion.CompanionDeviceManager
//import android.content.Context
//import android.content.Intent
//import android.content.IntentSender
//import android.os.Build
//import android.os.Handler
//import android.os.Looper
//import android.os.ParcelUuid
//import androidx.annotation.RequiresApi
//import io.flutter.plugin.common.EventChannel
//import io.flutter.plugin.common.MethodChannel
//import java.util.*
//import java.util.regex.Pattern
//import kotlin.collections.HashMap
//import kotlin.collections.HashSet
//
//@RequiresApi(Build.VERSION_CODES.O)
//class DeviceActivity(private val channel: MethodChannel, private val eventChannel: EventChannel) : Activity() {
//    companion object {
//        const val SELECT_DEVICE_REQUEST_CODE = 242
//
//        // intents
//        const val SINGLE_DEVICE = "singleDevice"
//        const val NAME_PATTERN = "namePattern"
//        const val UUID_STRING = "uuidString"
//
//        // channel response methods
//        const val BT_CANNOT_FIND_DEVICE = "BT_CANNOT_FIND_DEVICE"
//        const val BT_SELECT_DEVICE_CANCELLED = "BT_SELECT_DEVICE_CANCELLED"
//        const val DEVICE_PAIRED = "DEVICE_PAIRED"
//
//        // Device Paired Attributes
//        const val ADDRESS = "address"
//        const val NAME = "name"
//        const val UUIDS = "uuids"
//        const val BOND_STATE = "bondState"
//        const val TYPE = "type"
//        const val BLUETOOTH_CLASS = "bluetoothClass"
//        const val DEVICE_CLASS = "deviceClass"
//        const val MAJOR_DEVICE_CLASS = "majorDeviceClass"
//    }
//
//    private var pairedDevice: BluetoothDevice? = null
//    // private val openSockets: HashMap<UUID, OpenSocket> = HashMap()
//    private val deviceManager: CompanionDeviceManager by lazy(LazyThreadSafetyMode.NONE) {
//        getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
//    }
//
//    override fun onCreate(savedInstanceState: Bundle) {
//        super.onCreate(savedInstanceState)
//
//        val intent = getIntent()
//
//        val singleDevice = intent.getBooleanExtra(SINGLE_DEVICE);
//        val namePattern = intent.getStringExtra(NAME_PATTERN);
//        val uuid = intent.getStringExtra(UUID_STRING)
//
//        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder().apply {
//            if(namePattern != null){
//                setNamePattern(Pattern.compile(namePattern))
//            }
//
//            if(uuid != null){
//                addServiceUuid(ParcelUuid(UUID.fromString(uuid)), null)
//            }
//        }.build()
//
//        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
//            .addDeviceFilter(deviceFilter)
//            .setSingleDevice(singleDevice ?: true)
//            .build()
//
//        deviceManager.associate(pairingRequest,
//            object : CompanionDeviceManager.Callback() {
//                override fun onDeviceFound(chooserLauncher: IntentSender) {
//                    startIntentSenderForResult(chooserLauncher,
//                            SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
//                }
//                override fun onFailure(error: CharSequence?) {
//                    Handler(Looper.getMainLooper()).post {
//                        channel.invokeMethod(BT_CANNOT_FIND_DEVICE, error)
//                    }
//                }
//            }, null)
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//        when (requestCode) {
//            SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
//                RESULT_OK -> {
//                    pairedDevice = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
//                    pairedDevice?.let{
//                        it.createBond()
//                        Handler(Looper.getMainLooper()).post {
//                            channel.invokeMethod(DEVICE_PAIRED, mapOf(
//                                ADDRESS to it.address,
//                                NAME to it.name,
//                                UUIDS to it.uuids.map { uuidParcel -> uuidParcel.uuid.toString() },
//                                BOND_STATE to it.bondState,
//                                TYPE to it.type,
//                                BLUETOOTH_CLASS to it.bluetoothClass.let { btClass ->
//                                    mapOf(
//                                        DEVICE_CLASS to btClass.deviceClass,
//                                        MAJOR_DEVICE_CLASS to btClass.majorDeviceClass
//                                    )
//                                }
//                            ))
//                        }
//                    }
//                }
//                RESULT_CANCELED -> Handler(Looper.getMainLooper()).post {
//                    channel.invokeMethod(BT_SELECT_DEVICE_CANCELLED, null)
//                }
//            }
//        }
//    }
//
//    // fun disconnect(){
//    //     openSockets.forEach { (_, openSocket) ->
//    //         openSocket.close()
//    //     }
//    //     pairedDevice?.let {
//    //         deviceManager.disassociate(it.address)
//    //     }
//    // }
//
//    // fun getSocket(uuid: UUID): DeviceActivity.OpenSocket {
//    //     return when {
//    //         openSockets.containsKey(uuid) -> openSockets[uuid]!!
//    //         else -> OpenSocket(uuid)
//    //     }
//    // }
//
//    // inner class OpenSocket(private val uuid: UUID) : Thread(){
//    //     private val emitters: HashMap<String, EventChannel.EventSink> = HashMap()
//    //     private val mmBuffer: ByteArray = ByteArray(1024)
//    //     private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
//    //         pairedDevice?.createRfcommSocketToServiceRecord(uuid)
//    //     }
//
//    //     override fun run() {
//    //         BluetoothActivity.cancelDiscovery()
//
//    //         mmSocket?.use { socket ->
//    //             socket.connect()
//
//    //             while (true) {
//    //                 if(emitters.isNotEmpty()) {
//    //                     // Read from the InputStream.
//    //                     try {
//    //                         socket.inputStream.read(mmBuffer)
//    //                     } catch (e: Throwable) {
//    //                         if(openSockets.containsKey(uuid)) {
//    //                             openSockets.remove(uuid)
//    //                         }
//    //                         TODO("Maybe throw an error here")
//    //                         break
//    //                     }
//    //                     emitters.forEach { it.value.success(mmBuffer) }
//    //                 }
//    //             }
//    //         }
//    //     }
//
//    //     fun addReadEmitter(id: String, emitter: EventChannel.EventSink){
//    //         emitters[id] = emitter
//    //     }
//
//    //     fun removeReadEmitter(id: String){
//    //         emitters.minus(id)
//    //     }
//
//    //     fun write(bytes: ByteArray): Boolean {
//    //         try {
//    //             mmSocket?.use{
//    //                 it.outputStream.write(bytes)
//    //                 return true
//    //             }
//    //         } catch (e: Throwable) {
//    //             TODO("Throw an error here")
//    //         }
//    //         return false
//    //     }
//
//    //     fun close(): Boolean{
//    //         try {
//    //             mmSocket?.close()
//    //         } catch (e: Throwable) {
//    //             return false
//    //             TODO("Maybe throw an error here")
//    //         }
//
//    //         if(openSockets.containsKey(uuid)) {
//    //             openSockets.remove(uuid)
//    //         }
//    //         return true
//    //     }
//    // }
//}