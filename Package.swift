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
    let releaseTag = "0.0.10"
    let releaseChecksum = "fedb1818ae0da6cfac9d0004ad4fc32f77673e831a1c84a18a7233accf15427c"
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        url: "https://github.com/stadiamaps/ferrostar/releases/download/\(releaseTag)/libferrostar_core-rs.xcframework.zip",
        checksum: releaseChecksum
    )
}


let package = Package(
    name: "FerrostarCore",
    platforms: [
        .iOS(.v17),
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
//        .package(url: "https://github.com/maplibre/maplibre-gl-native-distribution", .upToNextMajor(from: "5.13.0")),
        .package(url: "https://github.com/stadiamaps/maplibre-swiftui-dsl-playground", branch: "main"),
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
            dependencies: ["FerrostarCore"],
            path: "apple/Tests/FerrostarCoreTests"
        ),
    ]
)
