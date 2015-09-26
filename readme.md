# NDK Test Gradle Plugin #

### Description ###

Build NDK Googletest based C++ tests with gradle and run them on an Android device. Can be used in Android studio.

### Requirements ###
It is important to have a compiling NDK configuration in place.

### How to use ###
Simply apply the plugin and you're ready to go.

```java
buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath 'me.barata.gradle:ndktest-plugin:1.0.0'
    }
}

apply plugin: 'me.barata.ndktest'
```

This will add multiple tasks to your project:

1. compile**Variant**NdkTest
2. connected**Variant**NdkTest

Simply running the connected**Variant**NdkTest will compile it and run on the device.

### Configure ###

After configuring your NDK, you may configure NDK test plugin by the following:

```java
android {
	...
	sourceSets {
		test {
			jni.srcDirs = ['src/test/jni']
		}
	}
}

ndktest {
	cFlags = "-std=c++11"
	ldLibs = ["log"]
	abiFilters = ['x86','armeabi-v7a']
}
```

### Known issues ###
1. Currently not compatible with gradle experimental plugin
2. Cannot debug on device using Android studio (some code is in place but I was unable to fix the gdb server)
3. Does not support multiple devices

### Suggestions ###
1. Use abi filters to avoid compiling every architecture every time. Use the architecture of the device under test.
2. Create a gradle task Android studio run configuration
3. Disable jniDebuggable for faster builds and runs

### Contact ###

This gradle plugin was made by Fabio Barata. Contact me anytime.
