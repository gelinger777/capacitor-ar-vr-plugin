package com.example.arvr;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;

import android.Manifest;

@CapacitorPlugin(
    name = "ArVrPlugin",
    permissions = {
        @Permission(
            alias = "camera",
            strings = { Manifest.permission.CAMERA }
        ),
        @Permission(
            alias = "location",
            strings = { Manifest.permission.ACCESS_FINE_LOCATION }
        )
    }
)
public class ArVrPluginPlugin extends Plugin {

    private ArVrPlugin implementation;

    @Override
    public void load() {
        implementation = new ArVrPlugin(getContext(), this);
    }

    @PluginMethod
    public void startSession(PluginCall call) {
        // Check permissions before starting
        if (getPermissionState("camera") != PermissionState.GRANTED ||
            getPermissionState("location") != PermissionState.GRANTED) {
            savedCall = call;
            requestAllPermissions(call, "permissionCallback");
            return;
        }

        implementation.startSession(call.getArray("pois"));
        call.resolve();
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            implementation.startSession(call.getArray("pois"));
            call.resolve();
        } else {
            call.reject("Camera permission is required for AR");
        }
    }

    private PluginCall savedCall;

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

    public void notifyObjectSelected(JSObject data) {
        notifyListeners("onObjectSelected", data);
    }
}
