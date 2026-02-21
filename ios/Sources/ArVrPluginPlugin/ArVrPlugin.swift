import Foundation
import ARKit
import SceneKit
import Capacitor
import CoreLocation
import UIKit

@objc public class ArVrPlugin: NSObject, ARSCNViewDelegate, ARSessionDelegate, CLLocationManagerDelegate {

    // Callback for event emission (set by ArVrPluginPlugin)
    public var onObjectSelected: ((_ data: JSObject) -> Void)?

    private weak var bridge: CAPBridgeProtocol?
    private var sceneView: ARSCNView?
    private var vrLeftView: ARSCNView?
    private var vrRightView: ARSCNView?
    private var isVrMode = false
    private var pois: [[String: Any]] = []

    private let locationManager = CLLocationManager()
    private var userLocation: CLLocation?

    init(bridge: CAPBridgeProtocol) {
        self.bridge = bridge
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    // MARK: - Public API

    @objc public func startSession(pois: [JSObject]) {
        self.pois = pois.map { $0 as [String: Any] }

        // Request location
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()

        DispatchQueue.main.async {
            self.setupAR()
            self.makeWebViewTransparent()

            // Delay anchor placement slightly to allow GPS fix
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                self.addAnchors()
            }
        }
    }

    @objc public func stopSession() {
        DispatchQueue.main.async {
            self.sceneView?.session.pause()
            self.sceneView?.removeFromSuperview()
            self.sceneView = nil
            self.disableVR()
            self.restoreWebView()
            self.locationManager.stopUpdatingLocation()
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

    // MARK: - CLLocationManagerDelegate

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let loc = locations.last {
            userLocation = loc
        }
    }

    // MARK: - AR Setup

    private func setupAR() {
        guard let viewController = bridge?.viewController else { return }

        sceneView = ARSCNView(frame: viewController.view.bounds)
        sceneView?.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        sceneView?.delegate = self
        sceneView?.session.delegate = self
        sceneView?.automaticallyUpdatesLighting = true

        // Insert behind webview
        viewController.view.insertSubview(sceneView!, at: 0)

        let configuration = ARWorldTrackingConfiguration()
        configuration.worldAlignment = .gravityAndHeading // Align with True North
        sceneView?.session.run(configuration)

        // Tap gesture for hit testing
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

    // MARK: - POI Placement with GPS Math

    private func addAnchors() {
        guard let sceneView = sceneView else { return }

        for poi in pois {
            guard let id = poi["id"] as? String,
                  let lat = poi["lat"] as? Double,
                  let lng = poi["lng"] as? Double else { continue }

            let label = poi["label"] as? String ?? "POI"
            let icon = poi["icon"] as? String ?? "📍"
            let rating = poi["rating"] as? Double ?? 4.0
            let votes = poi["votes"] as? Int ?? 0

            // Compute local AR offset from GPS
            let offset = gpsToLocal(poiLat: lat, poiLng: lng)
            let distMeters = sqrt(offset.x * offset.x + offset.z * offset.z)

            // Create the info card node
            let cardNode = createInfoCardNode(
                id: id,
                label: label,
                icon: icon,
                distance: distMeters,
                rating: rating,
                votes: votes
            )

            cardNode.name = id
            cardNode.position = SCNVector3(offset.x, 1.5, offset.z)

            // Billboard constraint: always face the camera
            let billboardConstraint = SCNBillboardConstraint()
            billboardConstraint.freeAxes = .Y
            cardNode.constraints = [billboardConstraint]

            sceneView.scene.rootNode.addChildNode(cardNode)
        }
    }

    // MARK: - Info Card Node (UIView → SCNPlane texture)

    private func createInfoCardNode(id: String, label: String, icon: String,
                                     distance: Float, rating: Double, votes: Int) -> SCNNode {
        let cardWidth: CGFloat = 320
        let cardHeight: CGFloat = 80

        // Render the card as a UIView
        let cardView = createCardView(
            width: cardWidth,
            height: cardHeight,
            label: label,
            icon: icon,
            distance: distance,
            rating: rating,
            votes: votes
        )

        // Convert UIView to image
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: cardWidth, height: cardHeight))
        let image = renderer.image { ctx in
            cardView.drawHierarchy(in: cardView.bounds, afterScreenUpdates: true)
        }

        // Apply image as texture on a SCNPlane
        let plane = SCNPlane(width: 0.5, height: 0.125)  // meters in AR space
        plane.firstMaterial?.diffuse.contents = image
        plane.firstMaterial?.isDoubleSided = true
        plane.firstMaterial?.lightingModel = .constant

        let node = SCNNode(geometry: plane)
        return node
    }

    private func createCardView(width: CGFloat, height: CGFloat, label: String,
                                 icon: String, distance: Float, rating: Double,
                                 votes: Int) -> UIView {
        let card = UIView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        card.backgroundColor = .white
        card.layer.cornerRadius = 8
        card.clipsToBounds = true

        // Icon circle
        let iconBg = UIView(frame: CGRect(x: 10, y: 15, width: 50, height: 50))
        iconBg.backgroundColor = UIColor(red: 0.56, green: 0.27, blue: 0.68, alpha: 1.0)
        iconBg.layer.cornerRadius = 6

        let iconLabel = UILabel(frame: iconBg.bounds)
        iconLabel.text = icon
        iconLabel.textAlignment = .center
        iconLabel.font = UIFont.systemFont(ofSize: 24)
        iconBg.addSubview(iconLabel)
        card.addSubview(iconBg)

        // Label
        let nameLabel = UILabel(frame: CGRect(x: 70, y: 12, width: 160, height: 20))
        nameLabel.text = label
        nameLabel.font = UIFont.boldSystemFont(ofSize: 16)
        nameLabel.textColor = .black
        card.addSubview(nameLabel)

        // Distance
        let distLabel = UILabel(frame: CGRect(x: 230, y: 12, width: 80, height: 20))
        distLabel.text = formatDistance(distance)
        distLabel.font = UIFont.systemFont(ofSize: 12)
        distLabel.textColor = .gray
        distLabel.textAlignment = .right
        card.addSubview(distLabel)

        // Stars
        let starsLabel = UILabel(frame: CGRect(x: 70, y: 38, width: 100, height: 18))
        starsLabel.text = getStarString(rating: rating)
        starsLabel.font = UIFont.systemFont(ofSize: 14)
        starsLabel.textColor = UIColor(red: 1.0, green: 0.76, blue: 0.03, alpha: 1.0)
        card.addSubview(starsLabel)

        // Votes
        let votesLabel = UILabel(frame: CGRect(x: 170, y: 38, width: 100, height: 18))
        votesLabel.text = "\(votes) votes"
        votesLabel.font = UIFont.systemFont(ofSize: 12)
        votesLabel.textColor = .gray
        card.addSubview(votesLabel)

        return card
    }

    // MARK: - Tap Handling

    @objc func handleTap(_ gesture: UITapGestureRecognizer) {
        guard let sceneView = sceneView else { return }
        let location = gesture.location(in: sceneView)

        let hitResults = sceneView.hitTest(location, options: nil)
        if let result = hitResults.first {
            let node = result.node
            // Walk up to find the named node (the card parent)
            var current: SCNNode? = node
            while current != nil {
                if let id = current?.name, !id.isEmpty {
                    notifyObjectSelected(id: id)
                    return
                }
                current = current?.parent
            }
        }
    }

    private func notifyObjectSelected(id: String) {
        if let poi = pois.first(where: { ($0["id"] as? String) == id }),
           let url = poi["url"] as? String {
            let data = JSObject()
            data["id"] = id
            data["url"] = url
            onObjectSelected?(data)
        }
    }

    // MARK: - GPS to Local Coordinates

    private func gpsToLocal(poiLat: Double, poiLng: Double) -> (x: Float, z: Float) {
        guard let userLoc = userLocation else {
            // Fallback for demo mode
            return (Float(poiLng) * 10, Float(-poiLat) * 10)
        }

        let userLat = userLoc.coordinate.latitude.degreesToRadians
        let userLng = userLoc.coordinate.longitude.degreesToRadians
        let targetLat = poiLat.degreesToRadians
        let targetLng = poiLng.degreesToRadians

        let dLat = targetLat - userLat
        let dLng = targetLng - userLng

        // Haversine distance
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(userLat) * cos(targetLat) *
                sin(dLng / 2) * sin(dLng / 2)
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        let distance = 6371000 * c  // meters

        // Bearing
        let y = sin(dLng) * cos(targetLat)
        let x = cos(userLat) * sin(targetLat) -
                sin(userLat) * cos(targetLat) * cos(dLng)
        let bearing = atan2(y, x)

        // Convert: x = east, z = -north
        let localX = Float(distance * sin(bearing))
        let localZ = Float(-distance * cos(bearing))

        return (localX, localZ)
    }

    // MARK: - VR Mode

    private func enableVR() {
        guard let viewController = bridge?.viewController, let originalView = sceneView else { return }
        originalView.isHidden = true

        let width = viewController.view.bounds.width / 2
        let height = viewController.view.bounds.height

        vrLeftView = ARSCNView(frame: CGRect(x: 0, y: 0, width: width, height: height))
        vrRightView = ARSCNView(frame: CGRect(x: width, y: 0, width: width, height: height))

        [vrLeftView, vrRightView].forEach { view in
            guard let view = view else { return }
            view.scene = originalView.scene
            view.session = originalView.session
            view.delegate = self
            viewController.view.insertSubview(view, at: 0)
        }
    }

    private func disableVR() {
        vrLeftView?.removeFromSuperview()
        vrRightView?.removeFromSuperview()
        vrLeftView = nil
        vrRightView = nil
        sceneView?.isHidden = false
    }

    // MARK: - Helpers

    private func formatDistance(_ meters: Float) -> String {
        if meters < 1000 {
            return String(format: "%.0fm", meters)
        }
        return String(format: "%.1fkm", meters / 1000)
    }

    private func getStarString(rating: Double) -> String {
        let full = Int(floor(rating))
        var stars = ""
        for _ in 0..<full { stars += "★" }
        for _ in full..<5 { stars += "☆" }
        return stars
    }
}

// MARK: - Extension

private extension Double {
    var degreesToRadians: Double { self * .pi / 180 }
}
