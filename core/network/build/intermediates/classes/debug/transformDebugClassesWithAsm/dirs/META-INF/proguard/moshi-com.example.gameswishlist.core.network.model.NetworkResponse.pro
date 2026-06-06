-keepnames class com.example.gameswishlist.core.network.model.NetworkResponse
-if class com.example.gameswishlist.core.network.model.NetworkResponse
-keep class com.example.gameswishlist.core.network.model.NetworkResponseJsonAdapter {
    public <init>(com.squareup.moshi.Moshi,java.lang.reflect.Type[]);
}
