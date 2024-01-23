// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let binaryTarget: Target
let maplibreSwiftUIDSLPackage: Package.Dependency
let useLocalFramework = false
let useLocalMapLibreSwiftUIDSL = false

if useLocalFramework {
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        // IMPORTANT: Swift packages importing this locally will not be able to
        // import Ferrostar core unless you specify this as a relative path!
        path: "./common/target/ios/libferrostar-rs.xcframework"
    )
} else {
    let releaseTag = "0.0.17"
    let releaseChecksum = "bd4f11f91deb132b8cfce679ef3bc238f761800bed9a3071d147b53c9a57ce59"
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        url: "https://github.com/stadiamaps/ferrostar/releases/download/\(releaseTag)/libferrostar-rs.xcframework.zip",
        checksum: releaseChecksum
    )
}

if useLocalMapLibreSwiftUIDSL {
    maplibreSwiftUIDSLPackage = .package(path: "../maplibre-swiftui-dsl-playground")
} else {
    maplibreSwiftUIDSLPackage = .package(url: "https://github.com/stadiamaps/maplibre-swiftui-dsl-playground", branch: "main")
}

let package = Package(
    name: "FerrostarCore",
    platforms: [
        .iOS(.v15),
    ],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "FerrostarCore",
            targets: ["FerrostarCore"]
        ),
        .library(
            name: "FerrostarMapLibreUI",
            targets: ["FerrostarMapLibreUI"]
        ),
    ],
    dependencies: [
        maplibreSwiftUIDSLPackage,
        .package(
            url: "https://github.com/pointfreeco/swift-snapshot-testing",
            from: "1.15.0"
        ),
    ],
    targets: [
        binaryTarget,
        .target(
            name: "FerrostarCore",
            dependencies: [.target(name: "UniFFI")],
            path: "apple/Sources/FerrostarCore"
        ),
        .target(
            name: "FerrostarMapLibreUI",
            dependencies: [
                .target(name: "FerrostarCore"),
                .product(name: "MapLibreSwiftDSL", package: "maplibre-swiftui-dsl-playground"),
                .product(name: "MapLibreSwiftUI", package: "maplibre-swiftui-dsl-playground"),
            ],
            path: "apple/Sources/FerrostarMapLibreUI"
        ),
        .target(
            name: "UniFFI",
            dependencies: [.target(name: "FerrostarCoreRS")],
            path: "apple/Sources/UniFFI"
        ),
        .testTarget(
            name: "FerrostarCoreTests",
            dependencies: [
                "FerrostarCore",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "apple/Tests/FerrostarCoreTests"
        ),
    ]
)
