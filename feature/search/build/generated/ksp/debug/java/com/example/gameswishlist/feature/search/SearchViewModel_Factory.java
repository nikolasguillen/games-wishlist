package com.example.gameswishlist.feature.search;

import com.example.gameswishlist.core.domain.usecase.SearchGamesUseCase;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<SearchGamesUseCase> searchGamesUseCaseProvider;

  private SearchViewModel_Factory(Provider<SearchGamesUseCase> searchGamesUseCaseProvider) {
    this.searchGamesUseCaseProvider = searchGamesUseCaseProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(searchGamesUseCaseProvider.get());
  }

  public static SearchViewModel_Factory create(
      Provider<SearchGamesUseCase> searchGamesUseCaseProvider) {
    return new SearchViewModel_Factory(searchGamesUseCaseProvider);
  }

  public static SearchViewModel newInstance(SearchGamesUseCase searchGamesUseCase) {
    return new SearchViewModel(searchGamesUseCase);
  }
}
