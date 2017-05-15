# Frodo-Leak-Android

## usage
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

```
dependencies {
  compile 'com.github.wyp0011:Frodo-Leak-Android:1.0.1'
}
```

MainApplication中添加
```
private RefWatcher refWatcher;
protected RefWatcher installLeakCanary() {
  return LeakCanary.install(this, UploadService.class,
          AndroidExcludedRefs.createAndroidDefaults().build());
}
```  
onCreate 中添加
```
super.onCreate();
refWatcher = installLeakCanary();
```
