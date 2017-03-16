var exec = require('cordova/exec');

var Downloader = {
    get: function (message, win, fail) {
        exec(win, fail, "DownloadPlugin", "get", [message]);
    }
};

module.exports = Downloader;