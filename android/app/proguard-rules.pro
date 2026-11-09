# Shizuku starts the file service by class name in a separate process.
-keep class com.filyar.app.FileService { *; }
-keep class com.filyar.app.IFileService { *; }
-keep class com.filyar.app.IFileService$* { *; }
-keep class com.filyar.app.RemoteFile { *; }
-keep class com.filyar.app.core.FileCore { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
