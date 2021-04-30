import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_bt/flutter_bt.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _lastResult = "N/A";
  PairedDevice pairedDevice;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    PairedDevice pairedDevice;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      // WePlenish.* matches regex for any BT device that begins with WePlenish
      pairedDevice = await FlutterBt.connect(
          new DeviceAttributes(nameRegex: "WePlenish.*"));
    } on PlatformException {
      pairedDevice = null;
    } catch (ex) {
      print(ex);
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _lastResult = pairedDevice != null
          ? pairedDevice.name
          : "Ensure Device is BT scanning";
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Bluetooth companion example'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Paired with Device: $_lastResult '),
              TextButton(
                  onPressed: () async {
                    final device = await FlutterBt.connect(
                        new DeviceAttributes(nameRegex: "WePlenish.*"));

                    setState(() {
                      pairedDevice = device;
                      _lastResult = pairedDevice != null
                          ? pairedDevice.name
                          : "Still N/A";
                    });
                  },
                  child: Text('Check For BT Device')),
              TextButton(
                  onPressed: () async {
                    final uuids = await pairedDevice.uuids;
                    setState(() {
                      _lastResult = uuids.first;
                    });
                  },
                  child: Text('Get UUIDS'))
            ],
          ),
        ),
      ),
    );
  }
}
