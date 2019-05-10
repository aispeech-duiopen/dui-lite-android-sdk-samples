# To enable ProGuard in your project, edit project.properties
# to define the proguard.config property as described in that file.
#
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the ProGuard
# include property in project.properties.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes EnclosingMethod
-keepattributes InnerClasses

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

#keep Util
-keep public class com.aispeech.common.Util{public *;}
-keep public class com.aispeech.common.WavFileWriter{public *;}
-keep public class com.aispeech.common.AITimer{public *;}
-keep public class com.aispeech.common.JSONResultParser{public *;}
-keep public class com.aispeech.common.AIConstant{public *;}
-keep public class com.aispeech.common.FileUtil{public *;}
-keep public class com.aispeech.DUILiteSDK{public *;}
-keep public class com.aispeech.fdm.**{public *;}
-keep public class com.aispeech.echo.**{public *;}
-keep public class com.aispeech.speex.**{public *;}
-keep public interface com.aispeech.lite.vad.VadKernelListener{public *;}
-keep public interface com.aispeech.lite.nr.NRKernelListener{public *;}

-keep class com.aispeech.upload.**{*;}
-keep public class com.aispeech.AIAudioRecord{public *;}

-keepclassmembers class com.aispeech.kernel.**{
	public static native <methods>;
}

-keep interface com.aispeech.kernel.**$*{
	public *;
}

-keep class com.aispeech.kernel.**$*{
	public *;
}

-keep interface com.aispeech.DUILiteSDK$*{
    public *;
}
