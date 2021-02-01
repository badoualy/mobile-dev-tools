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

Use the following function

`fun List<File>.getStitchedImage(startY: Int = 0, endY: Int = Integer.MAX_VALUE): ImmutableImage`

Where

* `startY` scrolling view top position
* `endY` scrolling view bottom position

Those values are optional and serve as optimisation to avoid checking unnecessary parts of the images
