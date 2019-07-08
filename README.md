# aar2jar
Gradle plugin which will help you to add Aar dependencies into your java|kotlin modules


How to use: add following lines in your module's `build.gradle` file
```
plugins {
	id 'kotlin'
	id 'com.stepango.aar2jar' version “0.6” // <- this one
}


dependencies {
	implementation Libs.kotlin
	compileOnlyAar "com.android.support:support-annotations:28.0.0" // <- Use any AAR dependencies
	// or
	implementationAar "com.android.support:support-annotations:28.0.0" // <- Use any AAR dependencies
	// or
	testCompileOnlyAar "com.android.support:support-annotations:28.0.0" // <- Use any AAR dependencies
	// or
	testImplementationAar "com.android.support:support-annotations:28.0.0" // <- Use any AAR dependencies
}
```
If using `compileOnlyAar` dependencies - make sure you have this dependency in any of your other modules `build.gradle`

```
implementation "com.android.support:support-annotations:28.0.0"
//or 
runtimeOnly "com.android.support:support-annotations:28.0.0"
```
[Presentation Mobuis 2019 RUS](https://drive.google.com/open?id=1r68gebquy6nSALzrCyP3kpK14tFL51pd)

[Gradle plugin page](https://plugins.gradle.org/plugin/com.stepango.aar2jar)
