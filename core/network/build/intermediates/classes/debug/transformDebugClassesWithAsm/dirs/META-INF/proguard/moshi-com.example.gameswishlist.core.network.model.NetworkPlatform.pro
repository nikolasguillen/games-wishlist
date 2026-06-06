-keepnames class com.example.gameswishlist.core.network.model.NetworkPlatform
-if class com.example.gameswishlist.core.network.model.NetworkPlatform
-keep class com.example.gameswishlist.core.network.model.NetworkPlatformJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
