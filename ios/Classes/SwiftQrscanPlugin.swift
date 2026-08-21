import AVFoundation
import Flutter
import UIKit
import Vision

public class SwiftQrscanPlugin: NSObject, FlutterPlugin {
  private var pendingResult: FlutterResult?

  fileprivate static let twoD: [VNBarcodeSymbology] = [.qr, .aztec, .dataMatrix, .pdf417]
  fileprivate static let oneD: [VNBarcodeSymbology] = [
    .code128, .code39, .code93, .ean8, .ean13, .upce, .itf14, .i2of5
  ]

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "qr_scan", binaryMessenger: registrar.messenger())
    let instance = SwiftQrscanPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "scan":
      pendingResult = result
      presentScanner()
    case "scan_photo":
      pendingResult = result
      presentPicker()
    case "scan_path":
      let args = call.arguments as? [String: Any]
      let path = args?["path"] as? String
      result(Self.decode(path: path))
    case "scan_bytes":
      let args = call.arguments as? [String: Any]
      let bytes = args?["bytes"] as? FlutterStandardTypedData
      result(Self.decode(data: bytes?.data))
    case "generate_barcode":
      let args = call.arguments as? [String: Any]
      guard let code = args?["code"] as? String else {
        result(FlutterError(code: "INVALID_ARGUMENT", message: "code is required", details: nil))
        return
      }
      result(Self.generateQr(code: code))
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func presentScanner() {
    let status = AVCaptureDevice.authorizationStatus(for: .video)
    if status == .denied || status == .restricted {
      finish(errorCode: "PERMISSION_NOT_GRANTED", message: "Camera permission denied")
      return
    }
    if status == .notDetermined {
      AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
        DispatchQueue.main.async {
          guard let self else { return }
          if granted {
            self.presentScanner()
          } else {
            self.finish(errorCode: "PERMISSION_NOT_GRANTED", message: "Camera permission denied")
          }
        }
      }
      return
    }
    let controller = ScanViewController { [weak self] value, error in
      if let error {
        self?.finish(errorCode: error, message: "Camera start failed")
      } else {
        self?.finish(value: value)
      }
    }
    present(controller)
  }

  private func presentPicker() {
    let picker = UIImagePickerController()
    picker.sourceType = .photoLibrary
    picker.delegate = self
    present(picker)
  }

  private func present(_ controller: UIViewController) {
    guard let root = Self.topViewController() else {
      finish(errorCode: "NO_ACTIVITY", message: "Unable to present scanner")
      return
    }
    controller.modalPresentationStyle = .fullScreen
    root.present(controller, animated: true)
  }

  private func finish(value: String?) {
    pendingResult?(value)
    pendingResult = nil
  }

  private func finish(errorCode: String, message: String) {
    pendingResult?(FlutterError(code: errorCode, message: message, details: nil))
    pendingResult = nil
  }

  static func topViewController() -> UIViewController? {
    let window = UIApplication.shared.windows.first { $0.isKeyWindow }
    var top = window?.rootViewController
    while let presented = top?.presentedViewController {
      top = presented
    }
    return top
  }

  static func decode(path: String?) -> String? {
    guard let path, let image = UIImage(contentsOfFile: path) else { return nil }
    return decode(image: image)
  }

  static func decode(data: Data?) -> String? {
    guard let data, let image = UIImage(data: data) else { return nil }
    return decode(image: image)
  }

  static func decode(image: UIImage) -> String? {
    guard let cgImage = image.cgImage else { return nil }
    let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
    if let text = decode(handler: handler, symbologies: twoD) {
      return text
    }
    return decode(handler: handler, symbologies: oneD)
  }

  static func decode(handler: VNImageRequestHandler, symbologies: [VNBarcodeSymbology]) -> String? {
    let request = VNDetectBarcodesRequest()
    request.symbologies = symbologies
    do {
      try handler.perform([request])
      return request.results?.first?.payloadStringValue
    } catch {
      return nil
    }
  }

  static func generateQr(code: String) -> FlutterStandardTypedData? {
    guard let data = code.data(using: .utf8),
          let filter = CIFilter(name: "CIQRCodeGenerator",
                                parameters: ["inputMessage": data, "inputCorrectionLevel": "M"]),
          let ciImage = filter.outputImage else {
      return nil
    }
    let scale = 400.0 / ciImage.extent.height
    let scaled = ciImage.transformed(by: CGAffineTransform(scaleX: scale, y: scale))
    let uiImage = UIImage(ciImage: scaled)
    guard let png = uiImage.pngData() else { return nil }
    return FlutterStandardTypedData(bytes: png)
  }
}

extension SwiftQrscanPlugin: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
  public func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
    picker.dismiss(animated: true) { [weak self] in
      self?.finish(value: nil)
    }
  }

  public func imagePickerController(_ picker: UIImagePickerController,
                                    didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
    let image = (info[.originalImage] as? UIImage)
    picker.dismiss(animated: true) { [weak self] in
      self?.finish(value: image.flatMap { Self.decode(image: $0) })
    }
  }
}

final class ScanViewController: UIViewController, AVCaptureVideoDataOutputSampleBufferDelegate {
  private let onResult: (String?, String?) -> Void
  private let session = AVCaptureSession()
  private var didFinish = false
  private let sessionQueue = DispatchQueue(label: "com.shinow.qrscan.session")
  private var previewLayer: AVCaptureVideoPreviewLayer?
  private var videoConnection: AVCaptureConnection?

  init(onResult: @escaping (String?, String?) -> Void) {
    self.onResult = onResult
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  override var shouldAutorotate: Bool { true }

  override var supportedInterfaceOrientations: UIInterfaceOrientationMask { .all }

  override func viewDidLoad() {
    super.viewDidLoad()
    view.backgroundColor = .black
    configureSession()
    addChrome()
  }

  override func viewWillAppear(_ animated: Bool) {
    super.viewWillAppear(animated)
    updateVideoOrientation()
    sessionQueue.async { [weak self] in
      self?.session.startRunning()
    }
  }

  override func viewWillDisappear(_ animated: Bool) {
    sessionQueue.async { [weak self] in
      self?.session.stopRunning()
    }
    super.viewWillDisappear(animated)
  }

  override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    previewLayer?.frame = view.bounds
    updateVideoOrientation()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: { _ in
      self.previewLayer?.frame = CGRect(origin: .zero, size: size)
      self.updateVideoOrientation()
    })
  }

  private func configureSession() {
    session.beginConfiguration()
    session.sessionPreset = .high
    guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
          let input = try? AVCaptureDeviceInput(device: device),
          session.canAddInput(input) else {
      session.commitConfiguration()
      DispatchQueue.main.async { [weak self] in
        self?.complete(nil, error: "CAMERA_START_FAILED")
      }
      return
    }
    session.addInput(input)
    let output = AVCaptureVideoDataOutput()
    output.setSampleBufferDelegate(self, queue: DispatchQueue(label: "com.shinow.qrscan.frames"))
    if session.canAddOutput(output) {
      session.addOutput(output)
    }
    videoConnection = output.connection(with: .video)
    session.commitConfiguration()

    let preview = AVCaptureVideoPreviewLayer(session: session)
    preview.videoGravity = .resizeAspectFill
    preview.frame = view.bounds
    view.layer.insertSublayer(preview, at: 0)
    previewLayer = preview
    updateVideoOrientation()
  }

  private func updateVideoOrientation() {
    let orientation = Self.captureOrientation()
    if let connection = previewLayer?.connection, connection.isVideoOrientationSupported {
      connection.videoOrientation = orientation
    }
    if let connection = videoConnection, connection.isVideoOrientationSupported {
      connection.videoOrientation = orientation
    }
  }

  /// UIInterfaceOrientation landscapeLeft/Right is inverted vs AVCaptureVideoOrientation.
  private static func captureOrientation() -> AVCaptureVideoOrientation {
    let interface: UIInterfaceOrientation
    if let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first {
      interface = scene.interfaceOrientation
    } else {
      interface = .portrait
    }
    switch interface {
    case .landscapeLeft:
      return .landscapeRight
    case .landscapeRight:
      return .landscapeLeft
    case .portraitUpsideDown:
      return .portraitUpsideDown
    default:
      return .portrait
    }
  }

  private func addChrome() {
    let close = UIButton(type: .system)
    close.setTitle("Close", for: .normal)
    close.setTitleColor(.white, for: .normal)
    close.titleLabel?.font = UIFont.systemFont(ofSize: 17, weight: .medium)
    close.addTarget(self, action: #selector(cancel), for: .touchUpInside)
    close.translatesAutoresizingMaskIntoConstraints = false
    view.addSubview(close)
    NSLayoutConstraint.activate([
      close.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 16),
      close.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 8)
    ])
  }

  @objc private func cancel() {
    complete(nil, error: nil)
  }

  func captureOutput(_ output: AVCaptureOutput,
                     didOutput sampleBuffer: CMSampleBuffer,
                     from connection: AVCaptureConnection) {
    if didFinish { return }
    guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
    if let text = decode(pixelBuffer: pixelBuffer, symbologies: SwiftQrscanPlugin.twoD) {
      complete(text, error: nil)
      return
    }
    if let text = decode(pixelBuffer: pixelBuffer, symbologies: SwiftQrscanPlugin.oneD) {
      complete(text, error: nil)
    }
  }

  private func decode(pixelBuffer: CVPixelBuffer, symbologies: [VNBarcodeSymbology]) -> String? {
    let request = VNDetectBarcodesRequest()
    request.symbologies = symbologies
    try? VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .up, options: [:]).perform([request])
    return request.results?.first?.payloadStringValue
  }

  private func complete(_ value: String?, error: String?) {
    guard !didFinish else { return }
    didFinish = true
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }
      self.dismiss(animated: true) {
        self.onResult(value, error)
      }
    }
  }
}
