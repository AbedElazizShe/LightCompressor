[![JitPack](https://jitpack.io/v/AbedElazizShe/LightCompressor.svg)](https://jitpack.io/#AbedElazizShe/LightCompressor)


# LightCompressor

LightCompressor can now be used in Flutter through [light_compressor](https://pub.dev/packages/light_compressor) plugin.

A powerful and easy-to-use video compression library for android uses [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec) API. This library generates a compressed MP4 video with a modified width, height, and bitrate (the number of bits per
seconds that determines the video and audio files’ size and quality). It is based on Telegram for Android source code.

The general idea of how the library works is that, extreme high bitrate is reduced while maintaining a good video quality resulting in a smaller size.

I would like to mention that the set attributes for size and quality worked just great in my projects and met the expectations. It may or may not meet yours. I’d appreciate your feedback so I can enhance the compression process.

**LightCompressor is now available in iOS**, have a look at [LightCompressor_iOS](https://github.com/AbedElazizShe/LightCompressor_iOS).


## How it works
When the video file is called to be compressed, the library checks if the user wants to set a min bitrate to avoid compressing low resolution videos. This becomes handy if you don’t want the video to be compressed every time it is to be processed to avoid having very bad quality after multiple rounds of compression. The minimum is;
* Bitrate: 2MB

You can pass one of a 5 video qualities; VERY_HIGH, HIGH, MEDIUM, LOW, OR VERY_LOW and the library will handle generating the right bitrate value for the output video
```kotlin
return when (quality) {
    VideoQuality.VERY_LOW -> (bitrate * 0.08).roundToInt()
    VideoQuality.LOW -> (bitrate * 0.1).roundToInt()
    VideoQuality.MEDIUM -> (bitrate * 0.2).roundToInt()
    VideoQuality.HIGH -> (bitrate * 0.3).roundToInt()
    VideoQuality.VERY_HIGH -> (bitrate * 0.5).roundToInt()
}

when {
   width >= 1920 || height >= 1920 -> {
      newWidth = (((width * 0.5) / 16).roundToInt() * 16)
      newHeight = (((height * 0.5) / 16f).roundToInt() * 16)
   }
   width >= 1280 || height >= 1280 -> {
      newWidth = (((width * 0.75) / 16).roundToInt() * 16)
      newHeight = (((height * 0.75) / 16).roundToInt() * 16)
   }
   width >= 960 || height >= 960 -> {
      newWidth = (((MIN_HEIGHT * 0.95) / 16).roundToInt() * 16)
      newHeight = (((MIN_WIDTH * 0.95) / 16).roundToInt() * 16)
   }
   else -> {
      newWidth = (((width * 0.9) / 16).roundToInt() * 16)
      newHeight = (((height * 0.9) / 16).roundToInt() * 16)
   }
}
```

You can as well pass custom videoHeight, videoWidth, and videoBitrate values if you don't want the library to auto-generate the values for you. **The compression will fail if height or width is specified without the other, so ensure you pass both values**.

These values were tested on a huge set of videos and worked fine and fast with them. They might be changed based on the project needs and expectations.

## Demo
![Demo](/pictures/demo.gif)

Usage
--------
To use this library, you must add the following permission to allow read and write to external storage.

**API < 29**

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
android:maxSdkVersion="28"/>
```

**API >= 29**

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

And import the following dependencies to use kotlin coroutines

### Groovy

```groovy
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"
```

### DSL

```dsl
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3")
```

Then just call [VideoCompressor.start()] and pass both source and destination file paths.

**NOTE**: The source video can be provided as a string path or a content uri. If both [srcPath] and
[srcUri] are provided, [srcUri] will be ignored. Passing [srcUri] requires [context].

The method has a callback for 5 functions;
1) OnStart - called when compression started
2) OnSuccess - called when compression completed with no errors/exceptions
3) OnFailure - called when an exception occurred or video bitrate and size are below the minimum required for compression.
4) OnProgress - called with progress new value
5) OnCancelled - called when the job is cancelled

You can pass the optional video quality (default is medium) and if to enable checking for min bitrate (default is true), in addition, if you wish to keep the
original video width and height from being changed during compression, you can pass true or false for keepOriginalResolution where default is false.

To cancel the compression job, just call [VideoCompressor.cancel()]

### Kotlin

```kotlin
VideoCompressor.start(
   context = applicationContext, // => This is required if srcUri is provided. If not, it can be ignored or null.
   srcUri = uri, // => Source can be provided as content uri, it requires context.
   srcPath = path, // => This could be ignored or null if srcUri and context are provided.
   destPath = desFile.path,
   listener = object : CompressionListener {
       override fun onProgress(percent: Float) {
          // Update UI with progress value
          runOnUiThread {
             // update a text view
             progress.text = "${percent.toLong()}%"
             // update a progress bar
             progressBar.progress = percent.toInt()
          }
       }

       override fun onStart() {
          // Compression start
       }

       override fun onSuccess() {
         // On Compression success
       }

       override fun onFailure(failureMessage: String) {
         // On Failure
       }

       override fun onCancelled() {
         // On Cancelled
       }

   },
   configureWith = Configuration(
      quality = VideoQuality.MEDIUM,
      isMinBitRateEnabled = true,
      keepOriginalResolution = false,
      videoHeight = 320.0 /*Double, ignore, or null*/,
      videoWidth = 320.0 /*Double, ignore, or null*/,
      videoBitrate = 3677198 /*Int, ignore, or null*/
   )
)
```
### Java

```java
 VideoCompressor.start(
    applicationContext, // => This is required if srcUri is provided. If not, pass null.
    uri, // => Source can be provided as content uri, it requires context.
    path, // => This could be null if srcUri and context are provided.
    desFile.path,
    new CompressionListener() {
       @Override
       public void onStart() {
         // Compression start
       }

       @Override
       public void onSuccess() {
         // On Compression success
       }

       @Override
       public void onFailure(String failureMessage) {
         // On Failure
       }

       @Override
       public void onProgress(float v) {
         // Update UI with progress value
         runOnUiThread(new Runnable() {
            public void run() {
                progress.setText(progressPercent + "%");
                progressBar.setProgress((int) progressPercent);
           }
         });
       }

       @Override
       public void onCancelled() {
         // On Cancelled
       }
    }, new Configuration(
        VideoQuality.MEDIUM,
        false,
        false,
        null /*videoHeight: double, or null*/,
        null /*videoWidth: double, or null*/,
        null /*videoBitrate: int, or null*/
    )
);
```

## Common issues
You cannot call Toast.makeText() and other functions dealing with the UI directly in onProgress() which is a worker thread. They need to be called
from within the main thread. Have a look at the example code above for more information.

## Reporting issues
To report an issue, please specify the following:
- Device name
- Android version
- If the bug/issue exists on the sample app (version 0.9.1) of the library that could be downloaded at this [link](https://drive.google.com/file/d/1u_7uXUD8gXzbs_5Lh_PozZbHmP9lzPV7/view?usp=sharing).

## Compatibility
Minimum Android SDK: LightCompressor requires a minimum API level of 21.

## Performance
This method was tested on Pixel, Huawei, Xiaomi, Samsung and Nokia phones and more than 150 videos.
Here’s some results from pixel 2 XL (medium quality);
* 94.3MB compressed to 9.2MB in 11 seconds
* 151.2MB compressed to 14.7MB in 18 seconds
* 65.7MB compressed to 6.4MB in 8 seconds

## How to add to your project?
#### Gradle

Ensure Kotlin version is `1.5.10`

Include this in your Project-level build.gradle file:

### Groovy

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

### DSL

```dsl
allprojects {
    repositories {
        .
        .
        maven( url = "https://jitpack.io" )
    }
}

```

Include this in your Module-level build.gradle file:

### Groovy

```groovy
implementation 'com.github.AbedElazizShe:LightCompressor:0.9.1'
```

### DSL

```dsl
implementation("com.github.AbedElazizShe:LightCompressor:0.9.1")
```

## Getting help
For questions, suggestions, or anything else, email elaziz.shehadeh(at)gmail.com

## Credits
[Telegram](https://github.com/DrKLO/Telegram) for Android.
