#import "SIP.h"
#import <Cordova/CDV.h>

@implementation SIP

- (void)connect:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Hello - that's your plugin :)"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)makecall:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Hello - that's your plugin :)"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)endcall:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Hello - that's your plugin :)"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)disconnect:(CDVInvokedUrlCommand*)command
{
  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Hello - that's your plugin :)"];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
