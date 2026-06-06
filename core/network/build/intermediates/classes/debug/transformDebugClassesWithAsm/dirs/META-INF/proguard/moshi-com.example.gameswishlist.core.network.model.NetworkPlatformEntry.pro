-keepnames class com.example.gameswishlist.core.network.model.NetworkPlatformEntry
-if class com.example.gameswishlist.core.network.model.NetworkPlatformEntry
-keep class com.example.gameswishlist.core.network.model.NetworkPlatformEntryJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
