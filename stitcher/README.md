### Stitcher

Stitch multiple screenshots together:

Input:

<img src="https://github.com/badoualy/mobile-dev-tools/blob/main/ART/stitch1.png" width="300"><img src="https://github.com/badoualy/mobile-dev-tools/blob/main/ART/stitch2.png" width="300"><img src="https://github.com/badoualy/mobile-dev-tools/blob/main/ART/stitch3.png" width="300">

Result:

<img src="https://github.com/badoualy/mobile-dev-tools/blob/main/ART/stitched.png" width="300">

### Setup

Available on jitpack. In the root build.gradle:

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

In your module build.gradle

```
dependencies {
  implementation 'com.github.badoualy.mobile-dev-tools:stitcher:<version>'
}
```

### Usage

`./gradlew runAnnotator --args="<options>"`

options:

- `--input <dir>` input directory
- `--timeout <value>` screenshot matching timeout value in ms (applied on a couple of images, not a global timeout)
- `--debug true` will draw bounds of each chunk in a different color on the result

The result will be exported into `result.png`

Use the following function

```kotlin
suspend fun List<File>.getStitchedImage(
    startY: Int = 0,
    endY: Int = Integer.MAX_VALUE,
    threshold: Int = 1,
    timeout: Long = 2 * 60 * 1000L
): StitchedImage
```

Where

* `startY` scrolling view top position
* `endY` scrolling view bottom position
* `threshold` number of successive lines that must match to consider a positive result
* `timeout` screenshot matching timeout value in ms (applied on a couple of images, not a global timeout)

Those values are optional and serve as optimisation to avoid checking unnecessary parts of the images. However, it is
strongly recommended specifying them, or the algorithm might find a false positive outside the scrolling view. 
