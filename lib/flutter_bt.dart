import 'dart:async';

import 'package:flutter/services.dart';

class FlutterBt {
  static const MethodChannel _channel =
      const MethodChannel('com.weplenish.flutter_bt');
  static const EventChannel _eventChannel =
      const EventChannel('com.weplenish.flutter_bt.event');

  /// Prompt user to connect to device name matching a regex, uuid, or both
  /// Only one connect attempt can be made at a time
  /// consider adding a timeout to the future
  static Future<PairedDevice> connect(DeviceAttributes attributes) async {
    final completer = new Completer<PairedDevice>();

    _channel.setMethodCallHandler((call) async {
      if (completer.isCompleted) {
        _channel.setMethodCallHandler(null);
        return;
      }

      switch (call.method) {
        case PairDeviceResponse.devicePaired:
          final device = PairedDevice.fromMap(
              new Map<String, dynamic>.from(call.arguments));
          completer.complete(device);
          break;
        case PairDeviceResponse.cannotFindDevice:
          completer.completeError("Cannot find device");
          break;
        case PairDeviceResponse.deviceSelectCancelled:
          completer.completeError("Device select cancelled by user");
          break;
        default:
          completer.completeError("Unknown");
          break;
      }

      _channel.setMethodCallHandler(null);
      return;
    });

    await _channel.invokeMethod(OutgoingMethod.connectBT, attributes.toMap());

    return completer.future;
  }
}

class OutgoingMethod {
  static const String isSupported = "isSupported";
  static const String isEnabled = "isEnabled";
  static const String enableBT = "enableBT";
  static const String connectBT = "connectBT";
  static const String writeBt = "writeBT";
  static const String closeSocket = "closeSocket";
  static const String disconnect = "disconnect";
}

class PairDeviceResponse {
  static const String devicePaired = "DEVICE_PAIRED";
  static const String deviceSelectCancelled = "BT_SELECT_DEVICE_CANCELLED";
  static const String cannotFindDevice = "BT_CANNOT_FIND_DEVICE";
}

class DeviceAttributes {
  final bool singleDevice;
  final String nameRegex;
  final String serviceUUID;

  DeviceAttributes(
      {this.nameRegex, this.serviceUUID, this.singleDevice = true});

  Map<String, dynamic> toMap() {
    var result = <String, dynamic>{"singleDevice": singleDevice};
    if (nameRegex != null) {
      result["namePattern"] = nameRegex;
    }
    if (serviceUUID != null) {
      result["uuidString"] = serviceUUID;
    }
    return result;
  }
}

class PairedDevice {
  final String address;
  final String name;
  List<String> _uuids;
  final int bondState;
  final int type;
  final BluetoothClass bluetoothClass;
  final MethodChannel _channel;
  final EventChannel _eventChannel;

  Future<List<String>> get uuids async {
    if (_uuids != null) {
      return _uuids;
    }
    _uuids = await this._channel.invokeMethod("GET_UUIDS");
    return _uuids;
  }

  PairedDevice(this._channel, this._eventChannel, this._uuids,
      {this.address,
      this.name,
      this.bondState,
      this.type,
      this.bluetoothClass});

  factory PairedDevice.fromMap(Map<String, dynamic> map) {
    final address = map['address'].toString();
    final channel = MethodChannel("com.weplenish.flutter_bt.$address");
    final eventChannel =
        EventChannel("com.weplenish.flutter_bt.event.$address");

    return PairedDevice(channel, eventChannel, map['uuids'],
        address: address,
        name: map['name'],
        bondState: map['bondState'],
        type: map['type'],
        bluetoothClass: BluetoothClass.fromMap(
            new Map<String, int>.from(map['bluetoothClass'])));
  }
}

class BluetoothClass {
  final int deviceClass;
  final int majorDeviceClass;

  BluetoothClass({this.deviceClass, this.majorDeviceClass});

  factory BluetoothClass.fromMap(Map<String, int> map) {
    return BluetoothClass(
        deviceClass: map['deviceClass'],
        majorDeviceClass: map['majorDeviceClass']);
  }
}
