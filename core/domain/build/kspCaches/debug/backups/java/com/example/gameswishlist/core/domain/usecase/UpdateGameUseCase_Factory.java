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
public final class UpdateGameUseCase_Factory implements Factory<UpdateGameUseCase> {
  private final Provider<GameRepository> repositoryProvider;

  private UpdateGameUseCase_Factory(Provider<GameRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public UpdateGameUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static UpdateGameUseCase_Factory create(Provider<GameRepository> repositoryProvider) {
    return new UpdateGameUseCase_Factory(repositoryProvider);
  }

  public static UpdateGameUseCase newInstance(GameRepository repository) {
    return new UpdateGameUseCase(repository);
  }
}
