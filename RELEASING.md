# Releasing

1. Update `version` to the new version `X.Y.Z` in `circularSeekBar/build.gradle`.
2. Add change log of this version in `circularSeekBar/build.gradle`.
3. Update version name in `README.md`
4. Execute `git commit -m "Upgrade to vX.Y.Z"` (where X.Y.Z is the new version).
5. Execute `./gradlew clean build install bintrayUpload`.
6. Execute `git push origin master:release/X.Y.Z`.
7. Review and merge branch to master.
8. Execute `git tag vX.Y.Z` (where X.Y.Z is the new version)
9. Execute `git push --tags`

