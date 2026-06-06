-keepnames class com.example.gameswishlist.core.network.model.NetworkDeveloper
-if class com.example.gameswishlist.core.network.model.NetworkDeveloper
-keep class com.example.gameswishlist.core.network.model.NetworkDeveloperJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
