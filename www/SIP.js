var exec = require('cordova/exec');

function SIP() {
}

SIP.prototype.connect = function(arg0, arg1, arg2, success, error) {
    exec(success, error, "SIP", "connect", [arg0, arg1, arg2]);
};

SIP.prototype.makeCall = function(arg0, success, error) {
    exec(success, error, "SIP", "makecall", [arg0]);
};

SIP.prototype.endCall = function(success, error) {
    exec(success, error, "SIP", "endcall", []);
};

SIP.prototype.disconnect = function(success, error) {
    exec(success, error, "SIP", "disconnect", []);
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

module.exports = new SIP();
