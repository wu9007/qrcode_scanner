import AVFoundation
import Flutter
import UIKit
import Vision

public class SwiftQrscanPlugin: NSObject, FlutterPlugin {
  private var pendingResult: FlutterResult?

  fileprivate static let qrOnly: [VNBarcodeSymbology] = [.qr]
  fileprivate static let twoDRest: [VNBarcodeSymbology] = [.aztec, .dataMatrix, .pdf417]
  fileprivate static var oneD: [VNBarcodeSymbology] {
    var list: [VNBarcodeSymbology] = [
      .code128, .code39, .code93, .ean8, .ean13, .upce, .itf14, .i2of5
    ]
    if #available(iOS 15.0, *) {
      list.append(.codabar)
    }
    return list
  }

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
      guard let code = args?["code"] as? String, !code.isEmpty else {
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
        let message = error == "CAMERA_IN_USE" ? "Camera in use" : "Camera start failed"
        self?.finish(errorCode: error, message: message)
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
    let window: UIWindow?
    if #available(iOS 13.0, *) {
      window = UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap { $0.windows }
        .first { $0.isKeyWindow }
    } else {
      window = UIApplication.shared.keyWindow
    }
    var top = window?.rootViewController
    while let presented = top?.presentedViewController {
      top = presented
    }
    return top
  }

  static func decode(path: String?) -> String? {
    guard let path, !path.isEmpty, let image = UIImage(contentsOfFile: path) else { return nil }
    return decode(image: image)
  }

  static func decode(data: Data?) -> String? {
    guard let data, let image = UIImage(data: data) else { return nil }
    return decode(image: image)
  }

  /// Album / path / bytes: QR first, then other 2D, then 1D.
  /// Honor EXIF orientation, then invert, then 90° steps — same idea as Android QrDecoder.
  static func decode(image: UIImage) -> String? {
    guard let cgImage = image.cgImage else { return nil }
    let first = CGImagePropertyOrientation(image.imageOrientation)
    var tried: Set<UInt32> = []
    let order: [CGImagePropertyOrientation] = [first, .up, .right, .down, .left]
    for orientation in order {
      if tried.contains(orientation.rawValue) { continue }
      tried.insert(orientation.rawValue)
      if let text = decode(cgImage: cgImage, orientation: orientation) {
        return text
      }
    }
    if let inverted = invert(cgImage) {
      tried.removeAll()
      for orientation in order {
        if tried.contains(orientation.rawValue) { continue }
        tried.insert(orientation.rawValue)
        if let text = decode(cgImage: inverted, orientation: orientation) {
          return text
        }
      }
    }
    return nil
  }

  static func decode(cgImage: CGImage, orientation: CGImagePropertyOrientation) -> String? {
    let handler = VNImageRequestHandler(cgImage: cgImage, orientation: orientation, options: [:])
    if let text = decode(handler: handler, symbologies: qrOnly) { return text }
    if let text = decode(handler: handler, symbologies: twoDRest) { return text }
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

  static func invert(_ image: CGImage) -> CGImage? {
    let ci = CIImage(cgImage: image)
    guard let filter = CIFilter(name: "CIColorInvert", parameters: [kCIInputImageKey: ci]),
          let output = filter.outputImage else { return nil }
    return CIContext().createCGImage(output, from: output.extent)
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

extension CGImagePropertyOrientation {
  init(_ ui: UIImage.Orientation) {
    switch ui {
    case .up: self = .up
    case .down: self = .down
    case .left: self = .left
    case .right: self = .right
    case .upMirrored: self = .upMirrored
    case .downMirrored: self = .downMirrored
    case .leftMirrored: self = .leftMirrored
    case .rightMirrored: self = .rightMirrored
    @unknown default: self = .up
    }
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
  private var cameraDevice: AVCaptureDevice?
  private let overlay = ScanOverlayView()
  private var torchButton: UIButton?

  init(onResult: @escaping (String?, String?) -> Void) {
    self.onResult = onResult
    super.init(nibName: nil, bundle: nil)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) not used")
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
    overlay.start()
    sessionQueue.async { [weak self] in
      self?.session.startRunning()
    }
  }

  override func viewWillDisappear(_ animated: Bool) {
    overlay.stop()
    setTorch(false)
    sessionQueue.async { [weak self] in
      self?.session.stopRunning()
    }
    super.viewWillDisappear(animated)
  }

  override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()
    previewLayer?.frame = view.bounds
    overlay.frame = view.bounds
    updateVideoOrientation()
  }

  override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
    super.viewWillTransition(to: size, with: coordinator)
    coordinator.animate(alongsideTransition: { _ in
      self.previewLayer?.frame = CGRect(origin: .zero, size: size)
      self.overlay.frame = CGRect(origin: .zero, size: size)
      self.overlay.setNeedsLayout()
      self.updateVideoOrientation()
    })
  }

  private func configureSession() {
    session.beginConfiguration()
    session.sessionPreset = .high
    guard let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
      session.commitConfiguration()
      DispatchQueue.main.async { [weak self] in
        self?.complete(nil, error: "CAMERA_START_FAILED")
      }
      return
    }
    cameraDevice = device
    let input: AVCaptureDeviceInput
    do {
      input = try AVCaptureDeviceInput(device: device)
    } catch {
      session.commitConfiguration()
      let ns = error as NSError
      let inUse = ns.code == AVError.deviceInUseByAnotherApplication.rawValue
      DispatchQueue.main.async { [weak self] in
        self?.complete(nil, error: inUse ? "CAMERA_IN_USE" : "CAMERA_START_FAILED")
      }
      return
    }
    guard session.canAddInput(input) else {
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

  private static func captureOrientation() -> AVCaptureVideoOrientation {
    let interface: UIInterfaceOrientation
    if #available(iOS 13.0, *) {
      if let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first {
        interface = scene.interfaceOrientation
      } else {
        interface = .portrait
      }
    } else {
      interface = UIApplication.shared.statusBarOrientation
    }
    switch interface {
    case .landscapeLeft: return .landscapeRight
    case .landscapeRight: return .landscapeLeft
    case .portraitUpsideDown: return .portraitUpsideDown
    default: return .portrait
    }
  }

  private func addChrome() {
    overlay.translatesAutoresizingMaskIntoConstraints = false
    view.addSubview(overlay)
    NSLayoutConstraint.activate([
      overlay.leadingAnchor.constraint(equalTo: view.leadingAnchor),
      overlay.trailingAnchor.constraint(equalTo: view.trailingAnchor),
      overlay.topAnchor.constraint(equalTo: view.topAnchor),
      overlay.bottomAnchor.constraint(equalTo: view.bottomAnchor)
    ])

    let cancel = UIButton(type: .system)
    cancel.setTitle("取消", for: .normal)
    cancel.setTitleColor(.white, for: .normal)
    cancel.titleLabel?.font = UIFont.systemFont(ofSize: 17, weight: .medium)
    cancel.addTarget(self, action: #selector(cancelTapped), for: .touchUpInside)
    cancel.translatesAutoresizingMaskIntoConstraints = false
    view.addSubview(cancel)
    NSLayoutConstraint.activate([
      cancel.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 16),
      cancel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 8)
    ])

    if cameraDevice?.hasTorch == true {
      let torch = UIButton(type: .system)
      torch.setTitle("手电", for: .normal)
      torch.setTitleColor(.white, for: .normal)
      torch.titleLabel?.font = UIFont.systemFont(ofSize: 16, weight: .medium)
      torch.addTarget(self, action: #selector(toggleTorch), for: .touchUpInside)
      torch.translatesAutoresizingMaskIntoConstraints = false
      view.addSubview(torch)
      NSLayoutConstraint.activate([
        torch.centerXAnchor.constraint(equalTo: view.centerXAnchor),
        torch.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -28)
      ])
      torchButton = torch
    }
  }

  @objc private func cancelTapped() {
    complete(nil, error: nil)
  }

  @objc private func toggleTorch() {
    guard let device = cameraDevice, device.hasTorch else { return }
    do {
      try device.lockForConfiguration()
      device.torchMode = device.torchMode == .on ? .off : .on
      device.unlockForConfiguration()
    } catch {
      // no flash / in use
    }
  }

  private func setTorch(_ on: Bool) {
    guard let device = cameraDevice, device.hasTorch else { return }
    try? device.lockForConfiguration()
    device.torchMode = on ? .on : .off
    device.unlockForConfiguration()
  }

  func captureOutput(_ output: AVCaptureOutput,
                     didOutput sampleBuffer: CMSampleBuffer,
                     from connection: AVCaptureConnection) {
    if didFinish { return }
    guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }
    if let text = decode(pixelBuffer: pixelBuffer, symbologies: SwiftQrscanPlugin.qrOnly) {
      complete(text, error: nil)
      return
    }
    if let text = decode(pixelBuffer: pixelBuffer, symbologies: SwiftQrscanPlugin.twoDRest) {
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

final class ScanOverlayView: UIView {
  private let line = UIView()
  private var hole = CGRect.zero
  private var running = false

  override init(frame: CGRect) {
    super.init(frame: frame)
    isOpaque = false
    backgroundColor = .clear
    isUserInteractionEnabled = false
    line.backgroundColor = UIColor(red: 0.07, green: 0.77, blue: 1, alpha: 0.9)
    addSubview(line)
  }

  required init?(coder: NSCoder) { fatalError("init(coder:) not used") }

  override func layoutSubviews() {
    super.layoutSubviews()
    let topBar: CGFloat = 56
    let bottom: CGFloat = 96
    let available = max(120, bounds.height - topBar - bottom)
    let size = min(bounds.width * 0.72, available * 0.92)
    let left = (bounds.width - size) / 2
    let top = topBar + max(0, (available - size) / 2)
    hole = CGRect(x: left, y: top, width: size, height: size)
    line.frame = CGRect(x: hole.minX + 8, y: hole.minY + 8, width: hole.width - 16, height: 2)
    setNeedsDisplay()
    if running { start() }
  }

  override func draw(_ rect: CGRect) {
    guard let ctx = UIGraphicsGetCurrentContext(), hole.width > 0 else { return }
    ctx.setFillColor(UIColor(white: 0, alpha: 0.55).cgColor)
    ctx.fill(CGRect(x: 0, y: 0, width: bounds.width, height: hole.minY))
    ctx.fill(CGRect(x: 0, y: hole.maxY, width: bounds.width, height: bounds.height - hole.maxY))
    ctx.fill(CGRect(x: 0, y: hole.minY, width: hole.minX, height: hole.height))
    ctx.fill(CGRect(x: hole.maxX, y: hole.minY, width: bounds.width - hole.maxX, height: hole.height))
    ctx.setStrokeColor(UIColor(red: 0.07, green: 0.77, blue: 1, alpha: 1).cgColor)
    ctx.setLineWidth(3)
    let len: CGFloat = 22
    let t = hole.minY, b = hole.maxY, l = hole.minX, r = hole.maxX
    ctx.move(to: CGPoint(x: l, y: t + len)); ctx.addLine(to: CGPoint(x: l, y: t)); ctx.addLine(to: CGPoint(x: l + len, y: t))
    ctx.move(to: CGPoint(x: r - len, y: t)); ctx.addLine(to: CGPoint(x: r, y: t)); ctx.addLine(to: CGPoint(x: r, y: t + len))
    ctx.move(to: CGPoint(x: l, y: b - len)); ctx.addLine(to: CGPoint(x: l, y: b)); ctx.addLine(to: CGPoint(x: l + len, y: b))
    ctx.move(to: CGPoint(x: r - len, y: b)); ctx.addLine(to: CGPoint(x: r, y: b)); ctx.addLine(to: CGPoint(x: r, y: b - len))
    ctx.strokePath()
  }

  func start() {
    running = true
    line.layer.removeAllAnimations()
    let startY = hole.minY + 8
    let endY = hole.maxY - 10
    guard endY > startY else { return }
    line.frame.origin.y = startY
    UIView.animate(withDuration: 1.6, delay: 0, options: [.repeat, .autoreverse, .curveLinear], animations: {
      self.line.frame.origin.y = endY
    })
  }

  func stop() {
    running = false
    line.layer.removeAllAnimations()
  }
}
