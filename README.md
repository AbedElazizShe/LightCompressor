[![JitPack](https://jitpack.io/v/AbedElazizShe/LightCompressor.svg)](https://jitpack.io/#AbedElazizShe/LightCompressor)


# LightCompressor
A powerful and easy-to-use video compression library for android uses [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec) API. This library generates a compressed MP4 video with a modified width, height, and bitrate (the number of bits per
seconds that determines the video and audio files’ size and quality). It is based on Telegram for Android source code.

The general idea of how the library works is that, extreme high bitrate is reduced while maintaining a good video quality resulting in a smaller size.

I would like to mention that the set attributes for size and quality worked just great in my projects and met the expectations. It may or may not meet yours. I’d appreciate your feedback so I can enhance the compression process.

## How it works
When the video file is called to be compressed, the library checks for minimum size and bitrate to determines whether the video needs to be compressed or not. This becomes handy if you don’t want the video to be compressed every time it is to be processed to avoid having very bad quality after multiple rounds of compression. The minimum values set here are;
* Bitrate: 2MB
* Height: 640
* Width: 360

If the file has higher values than that, the library generates new size and bitrate for the output file as follows;
```kotlin
when {
   bitrate >= 15000000 -> 2000000 // > 15MB becomes 2MB
   bitrate >= 8000000 -> 1500000 // > 8MB becomes 1.5MB
   bitrate >= 4000000 -> 1000000 // > 4MB becomes 1MB
   else -> 750000 // other values become 750KB
}

when {
   width >= 1920 || height >= 1920 -> {
       newWidth = (width * 0.5).toInt()
       newHeight = (height * 0.5).toInt()
   }
   width >= 1280 || height >= 1280 -> {
       newWidth = (width * 0.75).toInt()
       newHeight = (height * 0.75).toInt()
   }
   width >= 960 || height >= 960 -> {
       newWidth = MIN_HEIGHT
       newHeight = MIN_WIDTH
   }
   else -> {
       newWidth = width
       newHeight = height
   }
}
```
These values were tested on a huge set of videos and worked fine and fast with them. They might be changed based on the project needs and expectations.

## Demo
![Demo](/pictures/demo.gif)

Usage
--------
To use this library, you must add the following permissions to allow read and write to external storage.
```java
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```
And import the following dependencies to use kotlin coroutines

```groovy
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"
```

Then just call [doVideoCompression] and pass both source and destination file paths. The method has a callback for 4 functions;
1) OnStart - called when compression started
2) OnSuccess - called when compression completed with no errors/exceptions
3) OnFailure - called when an exception occured or video bitrate and size are below the minimum required for compression.
4) OnProgress - called with progress new value

```kotlin
VideoCompressor.doVideoCompression(
   path,
   desFile.path,
   object : CompressionListener {
       override fun onProgress(percent: Float) {
        // Update UI with progress value
       }

       override fun onStart() {
          // Compression start
       }

       override fun onSuccess() {
         // On Compression success
       }

       override fun onFailure() {
       // On Failure
       }

   })
```

## Compatibility
Minimum Android SDK: LightCompressor requires a minimum API level of 21.

## Performance
This method was tested on Pixel, Huawei, Xiaomi, Samsung and Nokia phones and more than 150 videos.
Here’s some results from pixel 2 XL;
* 94.3MB compressed to 9.2MB in 11 seconds
* 151.2MB compressed to 14.7MB in 18 seconds
* 65.7MB compressed to 6.4MB in 8 seconds

## How to add to your project?
#### Gradle

Include this in your Project-level build.gradle file:
```groovy
allprojects {
    repositories {
        .
        .
        .
        maven { url 'https://jitpack.io' }
    }
}
```

Include this in your Module-level build.gradle file:

```groovy
implementation 'com.github.AbedElazizShe:LightCompressor:0.1.0'
```

## Getting help
For questions, suggestions, or anything else, email elaziz.shehadeh(at)gmail.com

## Credits
[Telegram](https://github.com/DrKLO/Telegram) for Android

## License
Copyright 2020 AbedElaziz Shehadeh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.