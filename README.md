[![JitPack](https://jitpack.io/v/AbedElazizShe/LightCompressor.svg)](https://jitpack.io/#AbedElazizShe/LightCompressor)


# LightCompressor

LightCompressor can now be used in Flutter through [light_compressor](https://pub.dev/packages/light_compressor) plugin.

A powerful and easy-to-use video compression library for android uses [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec) API. This library generates a compressed MP4 video with a modified width, height, and bitrate (the number of bits per
seconds that determines the video and audio files’ size and quality). It is based on Telegram for Android project.

The general idea of how the library works is that, extreme high bitrate is reduced while maintaining a good video quality resulting in a smaller size.

I would like to mention that the set attributes for size and quality worked just great in my projects and met the expectations. It may or may not meet yours. I’d appreciate your feedback so I can enhance the compression process.

**LightCompressor is now available in iOS**, have a look at [LightCompressor_iOS](https://github.com/AbedElazizShe/LightCompressor_iOS).

# Change Logs

## What's new in 1.2.3

- **Breaking** `StorageConfiguration` was removed.
- **Breaking** `AppSpecificStorageConfiguration` can be passed to store the output video in Android's App Specific Storage.
- **Breaking** `SharedStorageConfiguration` can be passed to store the output video in Android's Primary Storage, it accepts storing at `pictures`, `downloads`, or `movies`.
- Only one of the configurations can be provided at a time, either `AppSpecificStorageConfiguration` or `SharedStorageConfiguration`.

# What's new in 1.2.2

- **Breaking** videoBitrate was renamed to videoBitrateInMbps. It should be int.
- Updated README Usage section
- Updated gradle and target android sdk to 33
- Bugs fixes

## How it works
When the video file is called to be compressed, the library checks if the user wants to set a min bitrate to avoid compressing low resolution videos. This becomes handy if you don’t want the video to be compressed every time it is to be processed to avoid having very bad quality after multiple rounds of compression. The minimum is;
* Bitrate: 2mbps

You can pass one of a 5 video qualities; VERY_HIGH, HIGH, MEDIUM, LOW, OR VERY_LOW and the library will handle generating the right bitrate value for the output video
```kotlin
return when (quality) {
    VideoQuality.VERY_LOW -> (bitrate * 0.1).roundToInt()
    VideoQuality.LOW -> (bitrate * 0.2).roundToInt()
    VideoQuality.MEDIUM -> (bitrate * 0.3).roundToInt()
    VideoQuality.HIGH -> (bitrate * 0.4).roundToInt()
    VideoQuality.VERY_HIGH -> (bitrate * 0.6).roundToInt()
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
      newWidth = (((width * 0.95) / 16).roundToInt() * 16)
      newHeight = (((height * 0.95) / 16).roundToInt() * 16)
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
To use this library, you must add the following permission to allow read and write to external storage. Refer to the sample app for a reference on how to start compression with the right setup.

**API < 29**

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28"
    tools:ignore="ScopedStorage" />
```

**API >= 29**

```xml
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32"/>
```

**API >= 33**

```xml
 <uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>
```

```kotlin

 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
     // request READ_MEDIA_VIDEO run-time permission
 } else {
     // request WRITE_EXTERNAL_STORAGE run-time permission
 }
```

And import the following dependencies to use kotlin coroutines

### Groovy

```groovy
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.coroutines}"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.coroutines}"
```

Then just call [VideoCompressor.start()] and pass **context**, **uris**, **isStreamable**, **configureWith**, and either **sharedStorageConfiguration OR appSpecificStorageConfiguration**.

The method has a callback for 5 functions;
1) OnStart - called when compression started
2) OnSuccess - called when compression completed with no errors/exceptions
3) OnFailure - called when an exception occurred or video bitrate and size are below the minimum required for compression.
4) OnProgress - called with progress new value
5) OnCancelled - called when the job is cancelled

### Important Notes:

- All the callback functions returns an index for the video being compressed in the same order of the urls passed to the library. You can use this index to update the UI
or retrieve information about the original uri/file.
- The source video must be provided as a list of content uris.
- OnSuccess returns the path of the stored video.
- If you want an output video that is optimised to be streamed, ensure you pass [isStreamable] flag is true.

### Configuration values

- VideoQuality: VERY_HIGH (original-bitrate * 0.6) , HIGH (original-bitrate * 0.4), MEDIUM (original-bitrate * 0.3), LOW (original-bitrate * 0.2), OR VERY_LOW (original-bitrate * 0.1)

- isMinBitrateCheckEnabled: this means, don't compress if bitrate is less than 2mbps

- videoBitrateInMbps: any custom bitrate value in Mbps.

- disableAudio: true/false to generate a video without audio. False by default.

- keepOriginalResolution: true/false to tell the library not to change the resolution.

- videoWidth: custom video width.

- videoHeight: custom video height.

### AppSpecificStorageConfiguration Configuration values

- videoName: a custom name for the output video.

- subFolderName: a subfolder name created in app's specific storage. The library won't create the subfolder and will throw an exception if the subfolder does not exist.


### AppSpecificStorageConfiguration Configuration values

- videoName: a custom name for the output video.

- saveAt: the directory where the video should be saved in. Must be one of the following; [SaveLocation.pictures], [SaveLocation.movies], or [SaveLocation.downloads].


To cancel the compression job, just call [VideoCompressor.cancel()]

### Kotlin

```kotlin
VideoCompressor.start(
   context = applicationContext, // => This is required
   uris = List<Uri>, // => Source can be provided as content uris
   isStreamable = false, 
   // THIS STORAGE 
   sharedStorageConfiguration = SharedStorageConfiguration(
       saveAt = SaveLocation.movies, // => default is movies
       videoName = "compressed_video" // => required name
   ),
   // OR AND NOT BOTH
   appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
       videoName = "compressed_video", // => required name
       subFolderName = "my-videos" // => optional and ONLY if exists
   ),   
   configureWith = Configuration(
      quality = VideoQuality.MEDIUM,
      isMinBitrateCheckEnabled = true,
      videoBitrateInMbps = 5, /*Int, ignore, or null*/
      disableAudio = false, /*Boolean, or ignore*/
      keepOriginalResolution = false, /*Boolean, or ignore*/
      videoWidth = 360.0, /*Double, ignore, or null*/
      videoHeight = 480.0 /*Double, ignore, or null*/
   ),
   listener = object : CompressionListener {
       override fun onProgress(index: Int, percent: Float) {
          // Update UI with progress value
          runOnUiThread {
          }
       }

       override fun onStart(index: Int) {
          // Compression start
       }

       override fun onSuccess(index: Int, size: Long, path: String?) {
         // On Compression success
       }

       override fun onFailure(index: Int, failureMessage: String) {
         // On Failure
       }

       override fun onCancelled(index: Int) {
         // On Cancelled
       }

   }
)
```

## Common issues

- Sending the video to whatsapp when disableAudio = false, won't succeed [ at least for now ]. Whatsapp's own compression does not work with
LightCompressor library. You can send the video as document.

- You cannot call Toast.makeText() and other functions dealing with the UI directly in onProgress() which is a worker thread. They need to be called
from within the main thread. Have a look at the example code above for more information.

## Reporting issues
To report an issue, please specify the following:
- Device name
- Android version
- If the bug/issue exists on the sample app (version 1.2.3) of the library that could be downloaded at this [link](https://drive.google.com/file/d/1WZtHN8gG2TaDuuTDKi9wB3B_sT_0SJ4w/view?usp=share_link).

## Compatibility
Minimum Android SDK: LightCompressor requires a minimum API level of 21.

## How to add to your project?
#### Gradle

Ensure Kotlin version is `1.6.0`

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

Include this in your Module-level build.gradle file:

### Groovy

```groovy
implementation 'com.github.AbedElazizShe:LightCompressor:1.2.3'
```

If you're facing problems with the setup, edit settings.gradle by adding this at the beginning of the file:

```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

## Getting help
For questions, suggestions, or anything else, email elaziz.shehadeh(at)gmail.com

## Credits
[Telegram](https://github.com/DrKLO/Telegram) for Android.
