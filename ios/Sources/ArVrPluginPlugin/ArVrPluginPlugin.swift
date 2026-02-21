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
        implementation = ArVrPlugin(bridge: self.bridge!)
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
