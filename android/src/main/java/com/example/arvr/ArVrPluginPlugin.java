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

import com.google.ar.core.ArCoreApk;

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
    public void checkAvailability(PluginCall call) {
        JSObject result = new JSObject();
        try {
            ArCoreApk.Availability availability =
                    ArCoreApk.getInstance().checkAvailability(getContext());

            if (availability == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                result.put("available", true);
                result.put("status", "supported");
                result.put("message", "ARCore is ready to use.");
            } else if (availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD ||
                       availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED) {
                result.put("available", false);
                result.put("status", "not_installed");
                result.put("message",
                    "Google Play Services for AR is not installed. " +
                    "Please install it from the Play Store to use AR features.");
            } else if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
                result.put("available", false);
                result.put("status", "unsupported_device");
                result.put("message",
                    "This device does not support ARCore. " +
                    "AR features require an ARCore-compatible device.");
            } else {
                result.put("available", false);
                result.put("status", "unknown");
                result.put("message",
                    "Unable to determine ARCore availability. Status: " +
                    availability.name());
            }
        } catch (Exception e) {
            result.put("available", false);
            result.put("status", "unknown");
            result.put("message", "Error checking AR availability: " + e.getMessage());
        }
        call.resolve(result);
    }

    @PluginMethod
    public void startSession(PluginCall call) {
        if (getPermissionState("camera") != PermissionState.GRANTED ||
            getPermissionState("location") != PermissionState.GRANTED) {
            savedCall = call;
            requestAllPermissions(call, "permissionCallback");
            return;
        }

        launchSession(call);
    }

    private void launchSession(PluginCall call) {
        implementation.startSession(call.getArray("pois"), new ArVrPlugin.SessionCallback() {
            @Override
            public void onSuccess() {
                call.resolve();
            }

            @Override
            public void onError(String message) {
                call.reject(message);
            }
        });
    }

    @PermissionCallback
    private void permissionCallback(PluginCall call) {
        if (getPermissionState("camera") == PermissionState.GRANTED) {
            launchSession(call);
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

    public void notifyObjectSelected(JSObject data) {
        notifyListeners("onObjectSelected", data);
    }
}
