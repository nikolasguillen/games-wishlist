package com.example.gameswishlist.core.domain.usecase.list;

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
public final class GetListsUseCase_Factory implements Factory<GetListsUseCase> {
  private final Provider<GameRepository> repositoryProvider;

  private GetListsUseCase_Factory(Provider<GameRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetListsUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetListsUseCase_Factory create(Provider<GameRepository> repositoryProvider) {
    return new GetListsUseCase_Factory(repositoryProvider);
  }

  public static GetListsUseCase newInstance(GameRepository repository) {
    return new GetListsUseCase(repository);
  }
}
