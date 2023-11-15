// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let binaryTarget: Target
// TODO: Define this via an env variable that doesn't need to be checked in?
let useLocalFramework = false

if useLocalFramework {
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        // IMPORTANT: Swift packages importing this locally will not be able to
        // import Ferrostar core unless you specify this as a relative path!
        path: "./common/target/ios/libferrostar-rs.xcframework"
    )
} else {
    let releaseTag = "0.0.13"
    let releaseChecksum = "041951f5c8aaf44bd60d5a90861d24cba4821a4601d1b091a9b7a64d739e01c4"
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        url: "https://github.com/stadiamaps/ferrostar/releases/download/\(releaseTag)/libferrostar-rs.xcframework.zip",
        checksum: releaseChecksum
    )
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
        .package(url: "https://github.com/stadiamaps/maplibre-swiftui-dsl-playground", branch: "main"),
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
                .product(name: "MapLibre", package: "maplibre-swiftui-dsl-playground"),
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
