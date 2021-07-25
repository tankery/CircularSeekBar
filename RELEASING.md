# Releasing

## I. Bump version

1. Update version to `X.Y.Z` in `gradle.properties`.
2. Update the `CHANGELOG.md` for the impending release.
3. Update version name in `README.md`
4. Execute `git commit -m "Upgrade to vX.Y.Z"` (where X.Y.Z is the new version).
5. Create a PR by execute `git push origin master:release/X.Y.Z`.
6. Review PR and merge branch to master.

## II. Publishing

1. Execute `git tag vX.Y.Z` (where X.Y.Z is the new version)
2. Execute `git push --tags`
3. The GitHub workflow will automatically create a new release for this version, and upload to Sonatype.
4. After published, visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.

