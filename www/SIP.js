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

module.exports = new SIP();
