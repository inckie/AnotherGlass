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
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable { *; }

# GDK (Glass Development Kit) classes are provided by the Glass system at runtime.
# Do not rename or remove any references to them.
-keep class com.google.android.glass.** { *; }

# Shared RPC protocol classes are looked up by full class name at runtime via
# Class.forName() in JsonMessageSerializer, and their fields/enum constants are
# accessed by name by Gson. Renaming any of them breaks deserialization.
-keep class com.damn.anotherglass.shared.** { *; }

# This is generated automatically by the Android Gradle plugin.
-dontwarn java.lang.invoke.MethodHandleInfo
-dontwarn javax.annotation.Nullable
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.OpenSSLProvider