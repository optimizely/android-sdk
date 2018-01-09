# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jdeffibaugh/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
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

## Below are the suggested rules from the developer documentation:
## https://developers.optimizely.com/x/solutions/sdks/reference/index.html?language=android&platform=mobile#installation

# Optimizely
#-keep class com.optimizely.ab.** { *; }

# Gson
#-keepnames class com.google.gson.Gson

# Safely ignore warnings about other libraries since we are using Gson
#-dontwarn com.fasterxml.jackson.**
-keep class com.fasterxml.jackson.** {*;}
-dontwarn com.google.gson.**
-dontwarn com.optimizely.ab.config.parser.**
#-dontwarn org.json.**

# Annotations
#-dontwarn javax.annotation.**

# Findbugs
#-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings

# slf4j
#-dontwarn org.slf4j.**
-keep class org.slf4j.** {*;}

# Android Logger
#-keep class com.noveogroup.android.log.** { *; }
