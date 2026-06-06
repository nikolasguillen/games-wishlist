package com.example.gameswishlist.core.database.di;

import com.example.gameswishlist.core.database.GamesWishlistDatabase;
import com.example.gameswishlist.core.database.dao.GameDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class DatabaseModule_ProvideGameDaoFactory implements Factory<GameDao> {
  private final Provider<GamesWishlistDatabase> databaseProvider;

  private DatabaseModule_ProvideGameDaoFactory(Provider<GamesWishlistDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public GameDao get() {
    return provideGameDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideGameDaoFactory create(
      Provider<GamesWishlistDatabase> databaseProvider) {
    return new DatabaseModule_ProvideGameDaoFactory(databaseProvider);
  }

  public static GameDao provideGameDao(GamesWishlistDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGameDao(database));
  }
}
