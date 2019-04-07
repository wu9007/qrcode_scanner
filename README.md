# qrcode_scan

基于ZXing的二维码扫描插件

library：https://github.com/yipianfengye/android-zxingLibrary

![Screenshot_20181110-160005.jpg](https://upload-images.jianshu.io/upload_images/3646846-c30da8ba73215907.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 权限：
- `<uses-permission android:name="android.permission.CAMERA" />`
- `<uses-permission android:name="android.permission.VIBRATE"/>`


编辑app目录下的build.gradle:
```groovy
buildscript {
    ext.kotlin_version = '1.3.21'
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```
编辑项目目录下的build.gradle:
```groovy
apply plugin: 'kotlin-android'
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version"
}
```

编辑 pubspec.yaml 文件
```
dependencies:
 qrscan: ^0.0.1
```

#### 使用方式
```dart
String barcode = await Qrscan.scan();
```

