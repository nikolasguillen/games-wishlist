package com.example.gameswishlist.feature.lists;

import com.example.gameswishlist.core.domain.usecase.list.CreateListUseCase;
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
public final class ListsViewModel_Factory implements Factory<ListsViewModel> {
  private final Provider<GetListsUseCase> getListsUseCaseProvider;

  private final Provider<CreateListUseCase> createListUseCaseProvider;

  private ListsViewModel_Factory(Provider<GetListsUseCase> getListsUseCaseProvider,
      Provider<CreateListUseCase> createListUseCaseProvider) {
    this.getListsUseCaseProvider = getListsUseCaseProvider;
    this.createListUseCaseProvider = createListUseCaseProvider;
  }

  @Override
  public ListsViewModel get() {
    return newInstance(getListsUseCaseProvider.get(), createListUseCaseProvider.get());
  }

  public static ListsViewModel_Factory create(Provider<GetListsUseCase> getListsUseCaseProvider,
      Provider<CreateListUseCase> createListUseCaseProvider) {
    return new ListsViewModel_Factory(getListsUseCaseProvider, createListUseCaseProvider);
  }

  public static ListsViewModel newInstance(GetListsUseCase getListsUseCase,
      CreateListUseCase createListUseCase) {
    return new ListsViewModel(getListsUseCase, createListUseCase);
  }
}
