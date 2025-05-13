import 'package:flutter/material.dart';
import 'package:mesh_base_flutter_example/TestScreen.dart';

void main() {
  runApp(
    MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('BLE Test')),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: const ExampleTestScreen(),
        ),
      ),
    ),
  );
}
