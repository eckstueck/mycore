System.register([], function(exports_1, context_1) {
    "use strict";
    var __moduleName = context_1 && context_1.id;
    var Settings;
    return {
        setters:[],
        execute: function() {
            class Settings {
                constructor(hS, cHS, aS, c) {
                    this.historySize = hS;
                    this.comHistorySize = cHS;
                    this.autoscroll = aS;
                    this.continueIfOneFails = c;
                }
            }
            exports_1("Settings", Settings);
        }
    }
});
