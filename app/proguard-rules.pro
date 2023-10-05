# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class android.** {
    *;
}

-keep class androidx.** {
    public *;
}
-keepclassmembers class org.cocos2dx.** {
    *;
}

-keep class org.cocos2dx.** { *; }
-dontwarn org.cocos2dx.**


-keep class com.android.vending.** {
    *;
}

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep ADCNative class members unobfuscated
-keepclassmembers class com.adcolony.sdk.ADCNative** {
    *;
}

# For removing warnings due to lack of Multi-Window support
-dontwarn android.app.Activity

-dontwarn com.applovin.**
-keep class com.applovin.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }

# For Google Play Services
-keep public class com.google.android.gms.ads.**{
   public *;
}

# For old ads classes
-keep public class com.google.ads.**{
   public *;
}

# For mediation
-keepattributes *Annotation*

# Other required classes for Google Play Services
# Read more at http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
   protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
   public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
   @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
   public static final ** CREATOR;
}

-keep public class com.ivy.IvySdk {
   public *;
}

-keepattributes Signature
-keep class com.facebook.android.*
-keep class android.webkit.WebViewClient
-keep class * extends android.webkit.WebViewClient
-keepclassmembers class * extends android.webkit.WebViewClient {
    <methods>;
}

-keep class com.chartboost.** { *; }
-dontwarn com.chartboost.**

-keep class com.google.ads.mediation.** { *; }
-dontwarn com.google.ads.mediation.**

-keep class com.ironsource.** { *; }
-dontwarn com.ironsource.**

-keep class com.vungle.** { *; }
-dontwarn com.vungle.**


-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-dontwarn android.support.v7.**
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.androidquery.callback.** {*;}
-keep public interface com.bytedance.sdk.openadsdk.downloadnew.** {*;}
-keep class com.ss.sys.ces.* {*;}

-keep class com.android.client.** { *; }
-keep public class com.ivy.IvySdk {
   public *;
}

-keep class **.R** {
    *;
}

-keepnames class **.R** {
    *;
}

-keepclassmembers class **.R$* {
	*;
}

-keep class **.R$* { *; }

-keep class com.unity3d.player.** { *; }


-keep class com.tencent.** { *; }

-keep class com.chartboost.** { *; }

-keep class okhttp3.** { *; }
-keep class okio.** { *; }

-keep class com.google.android.gms.** {*; }
-keep class com.android.billingclient.** {*; }

-keep class com.appsflyer.** { *; }
-keep class com.parfka.** {*;}