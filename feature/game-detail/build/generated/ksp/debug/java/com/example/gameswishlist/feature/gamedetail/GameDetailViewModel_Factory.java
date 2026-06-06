package com.example.gameswishlist.feature.gamedetail;

import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase;
import com.example.gameswishlist.core.domain.usecase.UpdateGameUseCase;
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase;
import com.example.gameswishlist.core.domain.usecase.list.GetListsUseCase;
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
public final class GameDetailViewModel_Factory implements Factory<GameDetailViewModel> {
  private final Provider<GetGameDetailUseCase> getGameDetailUseCaseProvider;

  private final Provider<UpdateGameUseCase> updateGameUseCaseProvider;

  private final Provider<GetListsUseCase> getListsUseCaseProvider;

  private final Provider<AddGameToListUseCase> addGameToListUseCaseProvider;

  private GameDetailViewModel_Factory(Provider<GetGameDetailUseCase> getGameDetailUseCaseProvider,
      Provider<UpdateGameUseCase> updateGameUseCaseProvider,
      Provider<GetListsUseCase> getListsUseCaseProvider,
      Provider<AddGameToListUseCase> addGameToListUseCaseProvider) {
    this.getGameDetailUseCaseProvider = getGameDetailUseCaseProvider;
    this.updateGameUseCaseProvider = updateGameUseCaseProvider;
    this.getListsUseCaseProvider = getListsUseCaseProvider;
    this.addGameToListUseCaseProvider = addGameToListUseCaseProvider;
  }

  @Override
  public GameDetailViewModel get() {
    return newInstance(getGameDetailUseCaseProvider.get(), updateGameUseCaseProvider.get(), getListsUseCaseProvider.get(), addGameToListUseCaseProvider.get());
  }

  public static GameDetailViewModel_Factory create(
      Provider<GetGameDetailUseCase> getGameDetailUseCaseProvider,
      Provider<UpdateGameUseCase> updateGameUseCaseProvider,
      Provider<GetListsUseCase> getListsUseCaseProvider,
      Provider<AddGameToListUseCase> addGameToListUseCaseProvider) {
    return new GameDetailViewModel_Factory(getGameDetailUseCaseProvider, updateGameUseCaseProvider, getListsUseCaseProvider, addGameToListUseCaseProvider);
  }

  public static GameDetailViewModel newInstance(GetGameDetailUseCase getGameDetailUseCase,
      UpdateGameUseCase updateGameUseCase, GetListsUseCase getListsUseCase,
      AddGameToListUseCase addGameToListUseCase) {
    return new GameDetailViewModel(getGameDetailUseCase, updateGameUseCase, getListsUseCase, addGameToListUseCase);
  }
}
