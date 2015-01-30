var exec = require('cordova/exec');

function SIP() {
}

SIP.prototype.connect = function(arg0, arg1, arg2, success, error) {
    exec(success, error, "SIP", "connect", [arg0, arg1, arg2]);
};

SIP.prototype.disconnect = function(success, error) {
    exec(success, error, "SIP", "disconnect", []);
};

SIP.prototype.login = function(arg0, arg1, success, error) {
    exec(success, error, "SIP", "login", [arg0, arg1]);
};

SIP.prototype.command = function(arg0, success, error) {
    exec(success, error, "SIP", "command", [arg0]);
};

module.exports = new SIP();
