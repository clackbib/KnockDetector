# KnockDetector

An Rx Knock detection tool.

<b>How to add to your project:</b>
```gradle
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}

dependencies {
	compile 'com.github.clackbib:KnockDetector:-SNAPSHOT'
}
```
<b>Usage:</b>


Required permissions

```xml

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

```

```kotlin
  disposable = KnockDetector.create(this)
                // Make sure to specify which thread to run your logic on, as this isn't internally restricted
                .subscribeOn(Schedulers.computation()) 
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                  when(it)
                    //Single knocks are ignored internally
                    2 -> // Action for 2 knocks
                    3 -> //Action for 3 knocks
                  }
                
                }, {
                  Log.e("Error", it.message, it) // Be sure to debug for missing permissions.
                })
                
                
  ....
  
  disposable.dispose() //Do not forget to dispose of the subscription when not needed, so resources can be release.

```
TODO:
====
- Provide a calibration API
- Provide additional configuration options
- Provide handling for situations where internal microphone is not accessible (Headsets / External Mics)
- Unit Tests.
