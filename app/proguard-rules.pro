# Keep WebView interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
