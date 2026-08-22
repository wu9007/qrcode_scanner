Pod::Spec.new do |s|
  s.name             = 'qrscan'
  s.version          = '1.1.2'
  s.summary          = 'Flutter QR / barcode scanner plugin.'
  s.description      = <<-DESC
Scan QR codes and barcodes, and generate QR images on Android and iOS.
                       DESC
  s.homepage         = 'https://github.com/wu9007/qrcode_scanner'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Shusheng' => 'wu9007' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.ios.deployment_target = '12.0'
  s.swift_version = '5.0'
  s.frameworks = 'AVFoundation', 'Vision', 'UIKit', 'CoreImage'
end
