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

# Keep line numbers so release crash traces can be de-obfuscated with the
# mapping file in app/build/outputs/mapping/release/, while hiding the original
# source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Moshi, Retrofit and kotlinx-serialization all ship their own consumer rules
# (META-INF/proguard/ inside their artifacts), so the JSON adapters, the
# IgdbApiService interface and the @Serializable NavKey routes are covered
# without repeating those keeps here. Hilt and Room do the same.