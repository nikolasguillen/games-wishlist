-keepnames class com.example.gameswishlist.core.network.model.NetworkGameDetail
-if class com.example.gameswishlist.core.network.model.NetworkGameDetail
-keep class com.example.gameswishlist.core.network.model.NetworkGameDetailJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
