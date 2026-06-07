package com.example.gameswishlist.feature.wishlist;

import com.example.gameswishlist.core.domain.usecase.list.GetGamesByListUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class WishlistViewModel_Factory implements Factory<WishlistViewModel> {
  private final Provider<GetGamesByListUseCase> getGamesByListUseCaseProvider;

  private WishlistViewModel_Factory(Provider<GetGamesByListUseCase> getGamesByListUseCaseProvider) {
    this.getGamesByListUseCaseProvider = getGamesByListUseCaseProvider;
  }

  @Override
  public WishlistViewModel get() {
    return newInstance(getGamesByListUseCaseProvider.get());
  }

  public static WishlistViewModel_Factory create(
      Provider<GetGamesByListUseCase> getGamesByListUseCaseProvider) {
    return new WishlistViewModel_Factory(getGamesByListUseCaseProvider);
  }

  public static WishlistViewModel newInstance(GetGamesByListUseCase getGamesByListUseCase) {
    return new WishlistViewModel(getGamesByListUseCase);
  }
}
