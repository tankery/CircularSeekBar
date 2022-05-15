# CircularSeekBar

[![Build Status](https://github.com/tankery/CircularSeekBar/actions/workflows/ci-check.yml/badge.svg?event=push&branch=master)](https://github.com/tankery/CircularSeekBar/actions/workflows/ci-check.yml)
[![GitHub release](https://img.shields.io/github/release/tankery/CircularSeekBar.svg?label=demo)](https://github.com/tankery/CircularSeekBar/releases)
[![Maven Central](https://img.shields.io/maven-central/v/me.tankery.lib/circularSeekBar)](https://search.maven.org/artifact/me.tankery.lib/circularSeekBar)

Rich feature Circular SeekBar (Circle, Semi-circle, and Ellipse) for Android.

This library is based on [CircularSeekBar of Matt Joseph (devadvance)](https://github.com/devadvance/circularseekbar).
But the original author seems to have stopped maintaining (last commit was in 2016), so I decide to take it up by myself.

I made it build on Android Studio (Gradle), then fix & add more features to the library.

<img src="/art/capture.jpg" alt="CircularSeekBar Screenshot" width="320" height="auto">

## The features I add

1. Support float progress, instead of integer only.
2. Refactor the name of the attributes, to avoid conflicts.
3. Disable the seek pointer, make it work like a circular progress bar.
4. Some other bug fixes for Matt's CircularSeekBar.
5. Customize the shape of progress end (butt, round, square)
6. Use a arc to represent the pointer, you can custom the arc angle.
7. Negative progress support.
8. Other small features.

## Setup

### Gradle

``` Gradle
dependencies {
    implementation 'me.tankery.lib:circularSeekBar:1.3.2'
}
```

### Source

Copy sources and `attrs.xml` in module `circularSeekBar` to your project.

## Usage

CircularSeekBar support following attributes:
```
app:cs_circle_style = "butt|round|square"
app:cs_progress = "integer"
app:cs_max = "integer"
app:cs_negative_enabled = "boolean"
app:cs_move_outside_circle = "boolean"
app:cs_maintain_equal_circle = "boolean"
app:cs_use_custom_radii = "boolean"
app:cs_lock_enabled = "boolean"
app:cs_circle_x_radius = "dimension"
app:cs_circle_y_radius = "dimension"
app:cs_circle_stroke_width = "dimension"
app:cs_disable_pointer = "boolean"
app:cs_pointer_stroke_width = "dimension"
app:cs_pointer_halo_width = "dimension"
app:cs_pointer_halo_border_width = "dimension"
app:cs_circle_fill = "color"
app:cs_circle_color = "color"
app:cs_circle_progress_color = "color"
app:cs_pointer_color = "color"
app:cs_pointer_halo_color = "color"
app:cs_pointer_halo_color_ontouch = "color"
app:cs_pointer_alpha_ontouch = "integer"
app:cs_pointer_angle = "float"
app:cs_start_angle = "float"
app:cs_end_angle = "float"
app:cs_disable_progress_glow = "boolean"
app:cs_hide_progress_when_empty = "boolean"
```

## Appreciation

This library is based on [CircularSeekBar of Matt Joseph (devadvance)](https://github.com/devadvance/circularseekbar).
But the original author seems to have stopped maintaining (last commit was in 2016), so I decide to take it up by myself. Thanks to Matt for the work!

### Automation

This part provides general solution to any types of libraries:

- GitHub Actions: [Quick Start](https://docs.github.com/en/actions/quickstart), [Workflow commands](https://docs.github.com/en/actions/reference/workflow-commands-for-github-actions) and [Upload Release Asset repo](https://github.com/actions/upload-release-asset) are official documents I use to write the CI check and releasing workflows.
- [Maven Publish Plugin](https://docs.gradle.org/current/userguide/publishing_maven.html) is the official doc for the new maven publish plugin. For Android, the config can be a little different, could also checkout [Android library distribution with maven-publish](https://proandroiddev.com/android-library-distribution-with-maven-publish-28ac59b8ecb8) for a reference.
   - Legacy Maven Publish docs: [Márton B.: Publishing Android libraries to MavenCentral in 2021](https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/): It is a great learning material for how to upload library to sonatype maven central. Another post ([LINK](https://proandroiddev.com/publishing-your-first-android-library-to-mavencentral-be2c51330b88)) also could used as a reference.
- Official Maven Central document [Gradle](https://central.sonatype.org/publish/publish-gradle/) to learn how to publish artifacts.
- Signature: Maven Central requires signature for library to release. [GPG](https://central.sonatype.org/publish/requirements/gpg/#distributing-your-public-key) and [The Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html) are two official document for how to sign the library, this will correct some error or outdated information in Márton's article.
- Also [Timber](https://github.com/JakeWharton/timber) of Jake Wharton is a great library to learn how to use a generic solution for public libraries. My [mvn-push.gradle](https://github.com/tankery/CircularSeekBar/blob/master/gradle/mvn-push.gradle) is actually forked from Timber repo, and it's really universal that could be used on many different types of libraries.

