// swift-tools-version: 5.8
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "FerrostarCore",
    platforms: [
        .iOS(.v14),
    ],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "FerrostarCore",
            targets: ["FerrostarCore"]
        ),
    ],
    dependencies: [
    ],
    targets: [
        // Targets are the basic building blocks of a package. A target can define a module or a test suite.
        // Targets can depend on other targets in this package, and on products in packages this package depends on.
        // TODO: This needs to point at an artifact on GitHub or something
        .binaryTarget(
            name: "FerrostarCoreRS",
            path: "common/target/ios/ferrostar-rs.xcframework.zip"
        ),
        .target(
            name: "FerrostarCore",
            dependencies: [.target(name: "UniFFI")],
            path: "SwiftCore/Sources/FerrostarCore"
        ),
        .target(
            name: "UniFFI",
            dependencies: [.target(name: "FerrostarCoreRS")],
            path: "SwiftCore/Sources/UniFFI"
        ),
        .testTarget(
            name: "FerrostarCoreTests",
            dependencies: ["FerrostarCore"],
            path: "SwiftCore/Tests/FerrostarCoreTests"
        ),
    ]
)
