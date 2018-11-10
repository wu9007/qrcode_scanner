#import "QrcodeScanPlugin.h"
#import <qrcode_scan/qrcode_scan-Swift.h>

@implementation QrcodeScanPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftQrcodeScanPlugin registerWithRegistrar:registrar];
}
@end
