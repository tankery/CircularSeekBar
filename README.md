# CircularSeekBar
Custom circular SeekBar (Circle, Semi-circle, and Ellipse) for Android.

This library is based on [CircularSeekBar of Matt Joseph (devadvance)](https://github.com/devadvance/circularseekbar).

The reason for create a repositive instead of fork it from Matt's, is that the project structure is different.

I make this library run on Android Studio (Gradle), and fix & add more features to the library.

<img src="/art/capture.jpg" alt="CircularSeekBar Screenshot" width="320" height="auto">

## The features I add

1. Support float progress, instead of integer only.
2. Refactor the name of the attributes, to avoid conflicts.
3. Disable the seek pointer, make it work like a circular progress bar.
4. Some other bug fixes for Matt's CircularSeekBar.
5. Customize the shape of progress end (butt, round, square)
6. Use a arc to represent the pointer, you can custom the arc angle.
7. Negative progress support.

## How to use

### Gradle
``` Gradle
dependencies {
    compile 'me.tankery.lib:circularSeekBar:1.1.4'
}
```

### Source
Copy sources and `attrs.xml` in module `circularSeekBar` to your project.

CircularSeekBar support following attributes:
``` xml
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
```

