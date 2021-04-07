import Flutter
import UIKit

public class SwiftQrscanPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "qr_scan", binaryMessenger: registrar.messenger())
    let instance = SwiftQrscanPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if call.method == "generate_barcode" {
        let arguments = call.arguments as? [String:Any] ?? [String: Any]()
        guard let code = arguments["code"] as? String else {
            return
        }

        guard let data = code.data(using: String.Encoding.utf8) else {
            return
        }

        guard let qr_filter = CIFilter(name: "CIQRCodeGenerator", parameters: ["inputMessage": data, "inputCorrectionLevel": "M"]) else {
            return
        }

        guard let ciImage = qr_filter.outputImage else {
          return
        }

        let scale = 400.0 / ciImage.extent.height
        let sizeTransform = CGAffineTransform(scaleX:scale, y:scale)
        let qrImage = ciImage.transformed(by: sizeTransform)
        let uiImage = UIImage(ciImage: qrImage)
        guard let bytearray = uiImage.pngData() else {
            return
        }
        result(bytearray)
    } else {
        result("iOS " + UIDevice.current.systemVersion)
    }
  }
}
