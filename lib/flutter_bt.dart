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
          final device = PairedDevice.fromMap(_channel, _eventChannel,
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
  final int bondState;
  final int type;
  final BluetoothClass bluetoothClass;
  List<BluetoothSocket> _sockets;
  final MethodChannel _channel;
  final EventChannel _eventChannel;

  /// gets a socket for this device matching [uuid]
  BluetoothSocket getSocket(String uuid) {
    if (_sockets == null) {
      _sockets = List.empty();
    }

    final socket =
        _sockets.firstWhere((element) => element.uuid == uuid, orElse: () {
      final newSocket = BluetoothSocket(uuid, address, _channel, _eventChannel);
      _sockets.add(newSocket);
      return newSocket;
    });

    return socket;
  }

  /// returns connected [sockets]
  Future<List<BluetoothSocket>> get sockets async {
    if (_sockets != null) {
      return _sockets;
    }
    final uuids = await this
        ._channel
        .invokeListMethod<String>("GET_UUIDS", {"address": address});

    if (uuids == null) {
      return null;
    }

    _sockets =
        uuids.map((e) => BluetoothSocket(e, address, _channel, _eventChannel));
    return _sockets;
  }

  PairedDevice(this._sockets, this._channel, this._eventChannel,
      {this.address,
      this.name,
      this.bondState,
      this.type,
      this.bluetoothClass});

  /// converts [map] from platform channel to a device object
  factory PairedDevice.fromMap(MethodChannel methodChannel,
      EventChannel eChannel, Map<String, dynamic> map) {
    final address = map['address'];
    final platformUuids = map['uuids'];
    final uuids = platformUuids == null
        ? null
        : platformUuids
            .map((e) => BluetoothSocket(e, address, methodChannel, eChannel));

    return PairedDevice(uuids, methodChannel, eChannel,
        address: address,
        name: map['name'],
        bondState: map['bondState'],
        type: map['type'],
        bluetoothClass: BluetoothClass.fromMap(
            new Map<String, int>.from(map['bluetoothClass'])));
  }
}

class BluetoothSocket {
  final String uuid;
  final MethodChannel _methodChannel;
  final EventChannel _eventChannel;
  final String _deviceAddress;
  Stream<dynamic> _readStream;

  BluetoothSocket(
      this.uuid, this._deviceAddress, this._methodChannel, this._eventChannel);

  Stream<dynamic> get listen {
    if (_readStream != null) {
      return _readStream;
    }
    _readStream = _eventChannel
        .receiveBroadcastStream({"address": _deviceAddress, "uuid": uuid});

    return _readStream;
  }

  Future write(String message) async {
    return await _methodChannel.invokeMethod("writeToSocket",
        {"address": _deviceAddress, "uuid": uuid, "message": message});
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
