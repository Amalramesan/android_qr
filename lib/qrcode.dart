import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class NativeQrScannerPage extends StatefulWidget {
  const NativeQrScannerPage({super.key});

  @override
  State<NativeQrScannerPage> createState() => _NativeQrScannerPageState();
}

class _NativeQrScannerPageState extends State<NativeQrScannerPage>
    with WidgetsBindingObserver {
  static const platform = MethodChannel('com.example.qrcode_scanner/methods');
  bool _isScanning = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _startNativeScanner();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);

    // When app resumes from background (like returning from browser)
    if (state == AppLifecycleState.resumed && !_isScanning) {
      // Small delay to ensure the activity is fully visible
      Future.delayed(const Duration(milliseconds: 300), () {
        if (mounted) {
          _startNativeScanner();
        }
      });
    }
  }

  Future<void> _startNativeScanner() async {
    if (_isScanning) return;

    setState(() {
      _isScanning = true;
    });

    try {
      final String? result = await platform.invokeMethod('startScanner');

      if (mounted) {
        setState(() {
          _isScanning = false;
        });

        if (result != null) {
          debugPrint("Scanned result: $result");

          // Handle non-URL results here if needed
          // For URLs, the Android side handles opening the browser
          if (!result.startsWith('http')) {
            // Handle non-URL QR codes
            _handleNonUrlResult(result);
          }
        }
      }
    } on PlatformException catch (e) {
      debugPrint("Failed to launch scanner: ${e.message}");
      if (mounted) {
        setState(() {
          _isScanning = false;
        });
      }
    }
  }

  void _handleNonUrlResult(String result) {
    // Handle non-URL QR codes here - you can process the result as needed
    // For example, you might want to navigate back or do something with the result
    debugPrint("Non-URL QR result: $result");

    // Navigate back to previous screen since scanning is complete
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
    return const SizedBox.shrink();
  }
}
