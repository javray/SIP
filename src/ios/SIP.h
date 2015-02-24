#import <Cordova/CDV.h>

@interface MyPlugin : CDVPlugin

- (void)connect:(CDVInvokedUrlCommand*)command;
- (void)makecall:(CDVInvokedUrlCommand*)command;
- (void)endcall:(CDVInvokedUrlCommand*)command;
- (void)disconnect:(CDVInvokedUrlCommand*)command;

@end
