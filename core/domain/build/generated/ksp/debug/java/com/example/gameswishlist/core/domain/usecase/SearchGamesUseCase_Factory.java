package com.example.gameswishlist.core.domain.usecase;

import com.example.gameswishlist.core.data.repository.GameRepository;
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
public final class SearchGamesUseCase_Factory implements Factory<SearchGamesUseCase> {
  private final Provider<GameRepository> repositoryProvider;

  private SearchGamesUseCase_Factory(Provider<GameRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SearchGamesUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static SearchGamesUseCase_Factory create(Provider<GameRepository> repositoryProvider) {
    return new SearchGamesUseCase_Factory(repositoryProvider);
  }

  public static SearchGamesUseCase newInstance(GameRepository repository) {
    return new SearchGamesUseCase(repository);
  }
}
