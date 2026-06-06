package com.example.gameswishlist.core.database.di;

import com.example.gameswishlist.core.database.GamesWishlistDatabase;
import com.example.gameswishlist.core.database.dao.ListDao;
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
public final class DatabaseModule_ProvideListDaoFactory implements Factory<ListDao> {
  private final Provider<GamesWishlistDatabase> databaseProvider;

  private DatabaseModule_ProvideListDaoFactory(Provider<GamesWishlistDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ListDao get() {
    return provideListDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideListDaoFactory create(
      Provider<GamesWishlistDatabase> databaseProvider) {
    return new DatabaseModule_ProvideListDaoFactory(databaseProvider);
  }

  public static ListDao provideListDao(GamesWishlistDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideListDao(database));
  }
}
