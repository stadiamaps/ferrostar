# Cutting a release

When cutting a release, follow this checklist:

1. Ensure that all version strings are up to date.
   Currently this means checking `common/ferrostar/Cargo.toml`, `Package.swift`, and `android/build.gradle`.
2. Create a GitHub release and use the new version as the tag name (not that it must be in X.Y.Z format to please SPM).
3. Sit back and watch. GitHub actions take care of the rest.
   Note that iOS CI ends up adding a commit due to the way binary checksumming works.
