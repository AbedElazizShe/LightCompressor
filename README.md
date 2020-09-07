[![JitPack](https://jitpack.io/v/AbedElazizShe/LightCompressor.svg)](https://jitpack.io/#AbedElazizShe/LightCompressor)


# LightCompressor
A powerful and easy-to-use video compression library for android uses [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec) API. This library generates a compressed MP4 video with a modified width, height, and bitrate (the number of bits per
seconds that determines the video and audio files’ size and quality). It is based on Telegram for Android source code.

The general idea of how the library works is that, extreme high bitrate is reduced while maintaining a good video quality resulting in a smaller size.

I would like to mention that the set attributes for size and quality worked just great in my projects and met the expectations. It may or may not meet yours. I’d appreciate your feedback so I can enhance the compression process.

**LightCompressor is now available in iOS**, have a look at [LightCompressor_iOS](https://github.com/AbedElazizShe/LightCompressor_iOS).
**Flutter plugin is coming soon**

## How it works
When the video file is called to be compressed, the library checks if the user wants to set a min bitrate to avoid compressing low resolution videos. This becomes handy if you don’t want the video to be compressed every time it is to be processed to avoid having very bad quality after multiple rounds of compression. The minimum is;
* Bitrate: 2MB

You can pass one of a 3 video qualities; High, Medium, or Low and the library will handle generating the right bitrate value for the output video
```kotlin
return when (quality) {
    VideoQuality.LOW -> (bitrate * 0.1).roundToInt()
    VideoQuality.MEDIUM -> (bitrate * 0.2).roundToInt()
    VideoQuality.HIGH -> (bitrate * 0.3).roundToInt()
}

when {
    width >= 1920 || height >= 1920 -> {
        newWidth = (width * 0.5)
        newHeight = (height * 0.5)
    }
    width >= 1280 || height >= 1280 -> {
        newWidth = (width * 0.75)
        newHeight = (height * 0.75)
    }
    width >= 960 || height >= 960 -> {
        newWidth = MIN_HEIGHT * 0.95
        newHeight = MIN_WIDTH * 0.95
    }
    else -> {
        newWidth = width * 0.9
        newHeight = height * 0.9
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

Then just call [VideoCompressor.start()] and pass both source and destination file paths. The method has a callback for 5 functions;
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
   path,
   desFile.path,
   object : CompressionListener {
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

   }, VideoQuality.MEDIUM, isMinBitRateEnabled = false, keepOriginalResolution = false)
```
### Java

```java
 VideoCompressor.start(path, desFile.path, new CompressionListener() {
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
 }, VideoQuality.MEDIUM, false, false);
```

## Common issues
You cannot call Toast.makeText() and other functions dealing with the UI directly in onProgress() which is a worker thread. They need to be called
from within the main thread. Have a look at the example code above for more information.

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
implementation 'com.github.AbedElazizShe:LightCompressor:0.7.2'
```

## Getting help
For questions, suggestions, or anything else, email elaziz.shehadeh(at)gmail.com

## Credits
[Telegram](https://github.com/DrKLO/Telegram) for Android
