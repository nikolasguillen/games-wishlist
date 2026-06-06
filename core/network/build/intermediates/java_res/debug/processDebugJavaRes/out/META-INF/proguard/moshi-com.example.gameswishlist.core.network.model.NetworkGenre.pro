-keepnames class com.example.gameswishlist.core.network.model.NetworkGenre
-if class com.example.gameswishlist.core.network.model.NetworkGenre
-keep class com.example.gameswishlist.core.network.model.NetworkGenreJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
