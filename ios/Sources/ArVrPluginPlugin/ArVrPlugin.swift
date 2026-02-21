import Foundation
import ARKit
import SceneKit
import Capacitor
import CoreLocation

@objc public class ArVrPlugin: NSObject, ARSCNViewDelegate, ARSessionDelegate {
    private weak var bridge: CAPBridgeProtocol?
    private var sceneView: ARSCNView?
    private var vrLeftView: ARSCNView?
    private var vrRightView: ARSCNView?
    private var isVrMode = false
    private var pois: [JSObject] = []
    
    init(bridge: CAPBridgeProtocol) {
        self.bridge = bridge
        super.init()
    }

    @objc public func startSession(pois: [JSObject]) {
        self.pois = pois
        DispatchQueue.main.async {
            self.setupAR()
            self.makeWebViewTransparent()
            self.addAnchors()
        }
    }

    @objc public func stopSession() {
        DispatchQueue.main.async {
            self.sceneView?.session.pause()
            self.sceneView?.removeFromSuperview()
            self.sceneView = nil
            self.restoreWebView()
        }
    }

    @objc public func toggleVRMode(enable: Bool) {
        self.isVrMode = enable
        DispatchQueue.main.async {
            if enable {
                self.enableVR()
            } else {
                self.disableVR()
            }
        }
    }

    private func setupAR() {
        guard let viewController = bridge?.viewController else { return }
        
        sceneView = ARSCNView(frame: viewController.view.bounds)
        sceneView?.delegate = self
        sceneView?.session.delegate = self
        sceneView?.automaticallyUpdatesLighting = true
        
        // Add behind webview
        viewController.view.insertSubview(sceneView!, at: 0)
        
        let configuration = ARWorldTrackingConfiguration()
        configuration.worldAlignment = .gravityAndHeading // Align with True North
        sceneView?.session.run(configuration)
        
        // Setup tap gesture for hit testing
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(handleTap(_:)))
        sceneView?.addGestureRecognizer(tapGesture)
    }

    private func makeWebViewTransparent() {
        bridge?.webView?.backgroundColor = .clear
        bridge?.webView?.isOpaque = false
        bridge?.webView?.scrollView.backgroundColor = .clear
    }

    private func restoreWebView() {
        bridge?.webView?.backgroundColor = .white
        bridge?.webView?.isOpaque = true
    }

    private func addAnchors() {
        // Here we would typically use CoreLocation to calculate distance/bearing
        // For this demo, we'll place them at relative world coordinates based on a simplified "flat earth" projection
        // Real implementation would use ARGeoAnchor (available in iOS 14+ in specific areas) or custom math.
        
        for poi in pois {
            guard let id = poi["id"] as? String,
                  let lat = poi["lat"] as? Double,
                  let lng = poi["lng"] as? Double else { continue }
            
            // Simplified placement: 10m north, 5m east of start for demo if coords are close
            // In reality, calculate X/Z offsets from user start location
            let node = SCNNode(geometry: SCNBox(width: 1, height: 1, length: 1, chamferRadius: 0.1))
            node.geometry?.firstMaterial?.diffuse.contents = UIColor.red
            node.name = id
            
            // Mock placement logic (replace with GPS to Local offset math)
            // Z is north-south, X is east-west
            node.position = SCNVector3(Float(lng) * 100, 0, -Float(lat) * 100) 
            
            sceneView?.scene.rootNode.addChildNode(node)
        }
    }

    @objc func handleTap(_ gesture: UITapGestureRecognizer) {
        guard let sceneView = sceneView else { return }
        let location = gesture.location(in: sceneView)
        
        let hitResults = sceneView.hitTest(location, options: nil)
        if let result = hitResults.first {
            let node = result.node
            if let id = node.name {
                notifyObjectSelected(id: id)
            }
        }
    }

    private func notifyObjectSelected(id: String) {
        if let poi = pois.first(where: { ($0["id"] as? String) == id }),
           let url = poi["url"] as? String {
            bridge?.triggerWindowListener(handlerName: "onObjectSelected", data: "{ \"id\": \"\(id)\", \"url\": \"\(url)\" }")
        }
    }

    private func enableVR() {
        guard let viewController = bridge?.viewController, let originalView = sceneView else { return }
        originalView.isHidden = true
        
        let width = viewController.view.bounds.width / 2
        let height = viewController.view.bounds.height
        
        vrLeftView = ARSCNView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        vrRightView = ARSCNView(frame: CGRect(x: width, y: 0, width: width, height: height))
        
        [vrLeftView, vrRightView].forEach { view in
            view?.scene = originalView.scene
            view?.session = originalView.session
            view?.delegate = self
            viewController.view.insertSubview(view!, at: 0)
        }
        
        // Offset cameras for interpupillary distance (IPD)
        // This is simplified; real VR needs custom camera nodes for each view
    }

    private func disableVR() {
        vrLeftView?.removeFromSuperview()
        vrRightView?.removeFromSuperview()
        sceneView?.isHidden = false
    }
}
