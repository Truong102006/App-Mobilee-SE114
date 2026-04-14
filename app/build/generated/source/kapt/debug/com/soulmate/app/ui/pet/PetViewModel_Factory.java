package com.soulmate.app.ui.pet;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
    "KotlinInternalInJava"
})
public final class PetViewModel_Factory implements Factory<PetViewModel> {
  @Override
  public PetViewModel get() {
    return newInstance();
  }

  public static PetViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PetViewModel newInstance() {
    return new PetViewModel();
  }

  private static final class InstanceHolder {
    private static final PetViewModel_Factory INSTANCE = new PetViewModel_Factory();
  }
}
