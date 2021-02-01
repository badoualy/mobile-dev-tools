### Annotator

Annotate screenshots taken from ui test with elements of the page.

The objectives are:

* Build a visible representation of each test flow with screenshots from each step (screen)
* Reference/document the ids used on the elements in each screen for the ui test (to share between platforms and qa
  devs)
* Add misc info from the code (fragment name, controller name, deeplink, ...) that can help new developers easily find a
  screen in the code

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
  implementation 'com.github.badoualy.mobile-dev-tools:annotator:<version>'
}
```

### Requirements

The directory structure should be the following:

```
<root>/
  ├── flow1/
  │   ├── flow.json
  │   ├── screenshot1.png
  │   ├── screenshot2.png
  │   ├── ...
  │   ├── screenshotN.png
  ├── flow2/
  │   ├── flow.json
  │   ├── screenshot1.png
  │   ├── screenshot2.png
  │   ├── ...
  │   ├── screenshotN.png
  └── ...
```

* The names of the files/directories are not important, the tool will used the first found json file in each
  subdirectory, and each screenshot name (and relative path) is specified in the json as shown below

The json should be of the following format:

```json
{
  "flowName": "onboarding",
  "steps": [
    {
      "files": ["step1.png"],
      "id": "screen.auth.home",
      "deeplink": "/auth",
      "fragmentName": "AuthHomeFragment",
      "controllerName": "AuthHomeController",
      "jsFileName": "screen_auth_home.js",
      "elements": [
        {
          "id": "bt_facebook",
          "x": 50,
          "y": 820,
          "width": 400,
          "height": 168
        },
        {
          "id": "bt_google",
          "x": 50,
          "y": 1040,
          "width": 400,
          "height": 168
        },
        {
          "id": "bt_sign_in",
          "x": 50,
          "y": 1470,
          "width": 400,
          "height": 168
        },
        {
          "id": "bt_sign_up",
          "x": 50,
          "y": 1670,
          "width": 400,
          "height": 168
        }
      ]
    }
  ]
}
```

The `files` attribute is a list of relative path, eg: `"files": ["./screenshots/step1.png"],`

### Usage

`./gradlew runAnnotator --args="<path_to_root>"`

### Result

The result will be exported in a `annotated` directory:

```
<root>/
  ├── flow1/
  │   ├── flow.json
  │   ├── screenshot1.png
  │   ├── screenshot2.png
  │   ├── ...
  │   ├── screenshotN.png
  │   └── annotated/
  │       ├── annotated_screenshot1.png
  │       ├── annotated_screenshot2.png
  │       ├── ...
  │       └── annotated_screenshotN.png
  ├── flow2/
  │   ├── flow.json
  │   ├── screenshot1.png
  │   ├── screenshot2.png
  │   ├── ...
  │   ├── screenshotN.png
  │   └── annotated/
  │       ├── annotated_screenshot1.png
  │       ├── annotated_screenshot2.png
  │       ├── ...
  │       └── annotated_screenshotN.png
  └── ...
```

| Original                                                                                        | Annotated                                                                                                |
|---------------------------------------------------------------------------------------------	|-------------------------------------------------------------------------------------------------------	|
| <img src="https://github.com/badoualy/mobile-tools-dev/blob/main/ART/step1.png" width="300">    | <img src="https://github.com/badoualy/mobile-tools-dev/blob/main/ART/annotated_step1.png" width="300">    |
