# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jdeffibaugh/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# when gson is replaced with jackson-databind json parser
-keep class com.fasterxml.jackson.** {*;}

#-printconfiguration proguard-merged-config.txt
