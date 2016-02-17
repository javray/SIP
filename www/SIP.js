var exec = require('cordova/exec');

function SIP() {
}

SIP.prototype.isSupported = function(success, error) {
    exec(success, error, "SIP", "issupported", []);
};

SIP.prototype.connect = function(arg0, arg1, arg2, arg3, success, error) {
    exec(success, error, "SIP", "connect", [arg0, arg1, arg2, arg3]);
};

SIP.prototype.makeCall = function(arg0, success, error) {
    arg0 = arg0.trim().replace(/\.| /g, '');
    exec(success, error, "SIP", "makecall", [arg0]);
};

SIP.prototype.endCall = function(success, error) {
    exec(success, error, "SIP", "endcall", []);
};

SIP.prototype.disconnect = function(success, error) {
    exec(success, error, "SIP", "disconnect", []);
};

SIP.prototype.isConnected = function(success, error) {
    exec(success, error, "SIP", "isconnected", []);
};

SIP.prototype.muteCall = function(arg0, success, error) {
    exec(success, error, "SIP", "mutecall", [arg0]);
};

SIP.prototype.speakerCall = function(arg0, success, error) {
    exec(success, error, "SIP", "speakercall", [arg0]);
};

SIP.prototype.dtmfCall = function(arg0, success, error) {
    exec(success, error, "SIP", "dtmfcall", [arg0]);
};

SIP.prototype.listenCall = function(success, error) {
    exec(success, error, "SIP", "listen", []);
};

SIP.prototype.stopListenCall = function(success, error) {
    exec(success, error, "SIP", "stoplisten", []);
};

SIP.prototype.incommingCall = function(success, error) {
    exec(success, error, "SIP", "incommingcall", []);
};

SIP.prototype.answerCall = function(success, error) {
    exec(success, error, "SIP", "answercall", []);
};

module.exports = new SIP();
