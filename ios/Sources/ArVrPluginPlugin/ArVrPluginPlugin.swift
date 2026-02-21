import Foundation
import Capacitor
import ARKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(ArVrPluginPlugin)
public class ArVrPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ArVrPluginPlugin"
    public let jsName = "ArVrPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "checkAvailability", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopSession", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "toggleVRMode", returnType: CAPPluginReturnPromise)
    ]
    private var implementation: ArVrPlugin?

    override public func load() {
        guard let bridge = self.bridge else { return }
        implementation = ArVrPlugin(bridge: bridge)

        // Wire the event callback to Capacitor's notifyListeners
        implementation?.onObjectSelected = { [weak self] data in
            self?.notifyListeners("onObjectSelected", data: data)
        }
    }

    @objc func checkAvailability(_ call: CAPPluginCall) {
        var result = JSObject()
        if ARWorldTrackingConfiguration.isSupported {
            result["available"] = true
            result["status"] = "supported"
            result["message"] = "ARKit is ready to use."
        } else {
            result["available"] = false
            result["status"] = "unsupported_device"
            result["message"] = "This device does not support ARKit. AR features require an A9 chip or later."
        }
        call.resolve(result)
    }

    @objc func startSession(_ call: CAPPluginCall) {
        let pois = call.getArray("pois", JSObject.self) ?? []
        implementation?.startSession(pois: pois)
        call.resolve()
    }

    @objc func stopSession(_ call: CAPPluginCall) {
        implementation?.stopSession()
        call.resolve()
    }

    @objc func toggleVRMode(_ call: CAPPluginCall) {
        let enable = call.getBool("enable") ?? false
        implementation?.toggleVRMode(enable: enable)
        call.resolve()
    }
}
