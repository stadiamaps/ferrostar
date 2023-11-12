# Cutting a release

When cutting a release, follow this checklist:

1. Ensure that all version strings are up to date.
   Currently this means checking crate and Swift package versions.
2. Generate the iOS framework using `build-ios.sh --release`.
   This updates Package.swift and generates a distributable zipped framework under `common/target/ios`.
3. Push the updated Package.swift.
3. Create a GitHub release targeting the last commit.
   Upload the zipped XCFramework (from step 2) along with the GitHub release! 
