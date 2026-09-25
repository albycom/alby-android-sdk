-keepclassmembers class com.alby.widget.NestedWebView$ScrollBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# NestedWebView flings a host RecyclerView by reflection.
-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
    public boolean fling(int, int);
}
