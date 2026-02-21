// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorArVrPlugin",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorArVrPlugin",
            targets: ["ArVrPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "ArVrPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/ArVrPluginPlugin"),
        .testTarget(
            name: "ArVrPluginPluginTests",
            dependencies: ["ArVrPluginPlugin"],
            path: "ios/Tests/ArVrPluginPluginTests")
    ]
)