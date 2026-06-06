-keepnames class com.example.gameswishlist.core.network.model.NetworkGame
-if class com.example.gameswishlist.core.network.model.NetworkGame
-keep class com.example.gameswishlist.core.network.model.NetworkGameJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
