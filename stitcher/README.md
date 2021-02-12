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

`./gradlew runStitcher --args="<options>"`

options:

- `--input <dir>` input directory
- `--bounds y1:y2` range of rows (Y values) that defines the area to look in
- `--threshold <value>` how many successive row should be identical to be considered a match (default: 50)
- `--timeout <value>` timeout before aborting merge
- `--debug true|false` will draw bounds of each chunk in a different color on the result (default: false)

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
* `timeout` timeout before aborting merge

Those values are optional and serve as optimisation to avoid checking unnecessary parts of the images. However, it is
strongly recommended specifying them, or the algorithm might find a false positive outside the scrolling view. 
