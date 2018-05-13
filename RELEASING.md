# Releasing

1. Update `version` to the new version `X.Y.Z` in `circularSeekBar/build.gradle`.
2. Add change log of this version in `circularSeekBar/build.gradle`.
3. Update version name in `README.md`
4. Execute `git commit -m "Upgrade to v_X.Y.Z"` (where X.Y.Z is the new version).
5. Execute `./gradlew clean build install bintrayUpload`.
6. Execute `git tag v_X.Y.Z"` (where X.Y.Z is the new version)
7. Execute `git push && git push --tags`

