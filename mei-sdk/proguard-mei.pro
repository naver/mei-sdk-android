# Proguard settings for mei sdk

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

-keep class com.naver.mei.sdk.core.gif.encoder.MapResult { *; }
-keep class wseemann.media.**{*; }

# Keep 3rd-party classes
-keep public class org.apache.commons.io.**
