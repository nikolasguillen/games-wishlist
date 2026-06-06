-keepnames class com.example.gameswishlist.core.network.model.NetworkPublisher
-if class com.example.gameswishlist.core.network.model.NetworkPublisher
-keep class com.example.gameswishlist.core.network.model.NetworkPublisherJsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
}
