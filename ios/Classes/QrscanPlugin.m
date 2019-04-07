#import "QrscanPlugin.h"
#import <qrscan/qrscan-Swift.h>

@implementation QrscanPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftQrscanPlugin registerWithRegistrar:registrar];
}
@end
