package com.example.gameswishlist.core.data.repository;

import com.example.gameswishlist.core.database.dao.GameDao;
import com.example.gameswishlist.core.database.dao.ListDao;
import com.example.gameswishlist.core.network.RawgApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class GameRepositoryImpl_Factory implements Factory<GameRepositoryImpl> {
  private final Provider<RawgApiService> apiServiceProvider;

  private final Provider<GameDao> gameDaoProvider;

  private final Provider<ListDao> listDaoProvider;

  private final Provider<String> apiKeyProvider;

  private GameRepositoryImpl_Factory(Provider<RawgApiService> apiServiceProvider,
      Provider<GameDao> gameDaoProvider, Provider<ListDao> listDaoProvider,
      Provider<String> apiKeyProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.gameDaoProvider = gameDaoProvider;
    this.listDaoProvider = listDaoProvider;
    this.apiKeyProvider = apiKeyProvider;
  }

  @Override
  public GameRepositoryImpl get() {
    return newInstance(apiServiceProvider.get(), gameDaoProvider.get(), listDaoProvider.get(), apiKeyProvider.get());
  }

  public static GameRepositoryImpl_Factory create(Provider<RawgApiService> apiServiceProvider,
      Provider<GameDao> gameDaoProvider, Provider<ListDao> listDaoProvider,
      Provider<String> apiKeyProvider) {
    return new GameRepositoryImpl_Factory(apiServiceProvider, gameDaoProvider, listDaoProvider, apiKeyProvider);
  }

  public static GameRepositoryImpl newInstance(RawgApiService apiService, GameDao gameDao,
      ListDao listDao, String apiKey) {
    return new GameRepositoryImpl(apiService, gameDao, listDao, apiKey);
  }
}
