// swift-tools-version: 5.8
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
        path: "./common/target/ios/ferrostar-rs.xcframework"
    )
} else {
    let releaseTag = "0.1.0"
    let releaseChecksum = "24d0efe06948dce7d0b2ffc5ebeb3dcb014a20b71b59642a5bdf2764b6fbc910"
    binaryTarget = .binaryTarget(
        name: "FerrostarCoreRS",
        url: "https://github.com/stadiamaps/ferrostar/releases/download/\(releaseTag)/ferrostar-rs.xcframework.zip",
        checksum: releaseChecksum
    )
}


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
        binaryTarget,
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
