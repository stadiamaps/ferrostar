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
    let releaseTag = "0.37.0"
    let releaseChecksum = "9b251cbc0509e69b6ca0d8da1d7f89f6964ca314785c8a2699a83c9c9e940686"
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        url:
        "https://github.com/stadiamaps/ferrostar/releases/download/\(releaseTag)/libferrostar-rs.xcframework.zip",
        checksum: releaseChecksum
    )
}

if useLocalMapLibreSwiftUIDSL {
    maplibreSwiftUIDSLPackage = .package(path: "../swiftui-dsl")
} else {
    maplibreSwiftUIDSLPackage = .package(
        url: "https://github.com/maplibre/swiftui-dsl",
        from: "0.12.0"
    )
}

let package = Package(
    name: "FerrostarCore",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v16),
    ],
    products: [
        // Products define the executables and libraries a package produces, and make them visible to other packages.
        .library(
            name: "FerrostarCore",
            targets: ["FerrostarCore", "FerrostarCoreFFI"]
        ),
        .library(
            name: "FerrostarMapLibreUI",
            targets: [
                "FerrostarMapLibreUI",
                "FerrostarSwiftUI",
                "FerrostarCarPlayUI",
            ] // TODO: Remove FerrostarSwiftUI from FerrostarMapLibreUI once we can fix the demo app swift package config (broken in Xcode 15.3)
        ),
        .library(
            name: "FerrostarSwiftUI",
            targets: ["FerrostarSwiftUI"]
        ),
        .library(
            name: "FerrostarCarPlayUI",
            targets: ["FerrostarCarPlayUI"]
        ),
    ],
    dependencies: [
        maplibreSwiftUIDSLPackage,
        .package(
            url: "https://github.com/pointfreeco/swift-snapshot-testing",
            from: "1.18.3"
        ),
    ],
    targets: [
        binaryTarget,
        .target(
            name: "FerrostarCarPlayUI",
            dependencies: [
                .target(name: "FerrostarCore"),
                .target(name: "FerrostarSwiftUI"),
                .target(name: "FerrostarMapLibreUI"),
                .product(name: "MapLibreSwiftDSL", package: "swiftui-dsl"),
                .product(name: "MapLibreSwiftUI", package: "swiftui-dsl"),
            ],
            path: "apple/Sources/FerrostarCarPlayUI"
        ),
        .target(
            name: "FerrostarCore",
            dependencies: [.target(name: "FerrostarCoreFFI")],
            path: "apple/Sources/FerrostarCore"
        ),
        .target(
            name: "FerrostarMapLibreUI",
            dependencies: [
                .target(name: "FerrostarCore"),
                .target(name: "FerrostarSwiftUI"),
                .product(name: "MapLibreSwiftDSL", package: "swiftui-dsl"),
                .product(name: "MapLibreSwiftUI", package: "swiftui-dsl"),
            ],
            path: "apple/Sources/FerrostarMapLibreUI"
        ),
        .target(
            name: "FerrostarSwiftUI",
            dependencies: [
                .target(name: "FerrostarCore"),
            ],
            path: "apple/Sources/FerrostarSwiftUI",
            resources: [
                .process("Resources"),
            ]
        ),
        .target(
            name: "FerrostarCoreFFI",
            dependencies: [.target(name: "FerrostarCoreRS")],
            path: "apple/Sources/UniFFI"
        ),

        // MARK: Testing

        .testTarget(
            name: "FerrostarCarPlayUITests",
            dependencies: [
                "FerrostarCore",
                "FerrostarSwiftUI",
                "FerrostarCarPlayUI",
                "TestSupport",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "apple/Tests/FerrostarCarPlayUITests"
        ),
        .testTarget(
            name: "FerrostarCoreTests",
            dependencies: [
                "FerrostarCore",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "apple/Tests/FerrostarCoreTests"
        ),
        .testTarget(
            name: "FerrostarMapLibreUITests",
            dependencies: [
                "FerrostarCore",
                "FerrostarSwiftUI",
                "FerrostarMapLibreUI",
                "TestSupport",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "apple/Tests/FerrostarMapLibreUITests"
        ),
        .testTarget(
            name: "FerrostarSwiftUITests",
            dependencies: [
                "FerrostarCore",
                "FerrostarSwiftUI",
                "TestSupport",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "apple/Tests/FerrostarSwiftUITests"
        ),

        // MARK: Test Support

        .target(
            name: "TestSupport",
            dependencies: [
                "FerrostarCore",
                "FerrostarSwiftUI",
                .product(name: "SnapshotTesting", package: "swift-snapshot-testing"),
            ],
            path: "apple/Tests/TestSupport"
        ),
    ]
)
