package com.example.gameswishlist.core.network.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("javax.inject.Named")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class NetworkModule_ProvideRawgApiKeyFactory implements Factory<String> {
  @Override
  public String get() {
    return provideRawgApiKey();
  }

  public static NetworkModule_ProvideRawgApiKeyFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static String provideRawgApiKey() {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideRawgApiKey());
  }

  private static final class InstanceHolder {
    static final NetworkModule_ProvideRawgApiKeyFactory INSTANCE = new NetworkModule_ProvideRawgApiKeyFactory();
  }
}
