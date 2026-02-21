import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(ArVrPluginPlugin)
public class ArVrPluginPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "ArVrPluginPlugin"
    public let jsName = "ArVrPlugin"
    public let pluginMethods: [CAPPluginMethod] = [
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
