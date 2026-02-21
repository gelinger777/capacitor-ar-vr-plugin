package com.example.arvr;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ArVrPlugin")
public class ArVrPluginPlugin extends Plugin {

    private ArVrPlugin implementation;

    @Override
    public void load() {
        implementation = new ArVrPlugin(getContext(), getBridge());
    }

    @PluginMethod
    public void startSession(PluginCall call) {
        implementation.startSession(call.getArray("pois"));
        call.resolve();
    }

    @PluginMethod
    public void stopSession(PluginCall call) {
        implementation.stopSession();
        call.resolve();
    }

    @PluginMethod
    public void toggleVRMode(PluginCall call) {
        implementation.toggleVRMode(call.getBoolean("enable", false));
        call.resolve();
    }
}
