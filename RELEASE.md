# Cutting a release

When cutting a release, follow this checklist:

1. Run `./update-release-version.sh X.Y.Z` with the new version string. Note that this is currently macOS-specific given the vagaries of GNU sed.
2. Commit staged changes and push.
3. Create a GitHub release and use the new version as the tag name (not that it must be in X.Y.Z format to please SPM).
4. Sit back and watch. GitHub actions take care of the rest.
   Note that iOS CI ends up adding a commit due to the way binary checksumming works.

## GPG

Maven Central requires all packages to be signed.
This necessarily means the headache of key management.
Fortunately keys don't need to be regenerated very often,
but here are some notes for whenever it's required again.

Sonatype has good [docs](https://central.sonatype.org/publish/requirements/gpg/) on generating a key.
To export the private key for use in CI,
you can run the following command.

```shell
gpg --armor --export-secret-key you@example.com | grep -v '\-\-' | grep -v '^=.' | tr -d '\n'
```
