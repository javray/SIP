var exec = require('cordova/exec');

function SIP() {
}

SIP.prototype.connect = function(arg0, arg1, arg2, success, error) {
    exec(success, error, "SIP", "connect", [arg0, arg1, arg2]);
};

SIP.prototype.call = function(arg0, success, error) {
    exec(success, error, "SIP", "call", [arg0]);
};

SIP.prototype.callend = function(success, error) {
    exec(success, error, "SIP", "callend", []);
};

module.exports = new SIP();
