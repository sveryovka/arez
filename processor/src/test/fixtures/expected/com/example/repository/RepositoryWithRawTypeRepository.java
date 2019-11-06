package com.example.repository;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.component.internal.AbstractRepository;
import java.util.concurrent.Callable;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

@SuppressWarnings("rawtypes")
@Generated("arez.processor.ArezProcessor")
@ArezComponent
@Singleton
public abstract class RepositoryWithRawTypeRepository extends AbstractRepository<Integer, RepositoryWithRawType, RepositoryWithRawTypeRepository> {
  RepositoryWithRawTypeRepository() {
  }

  @Nonnull
  public static RepositoryWithRawTypeRepository newRepository() {
    return new Arez_RepositoryWithRawTypeRepository();
  }

  @Action(
      name = "create"
  )
  @Nonnull
  public RepositoryWithRawType create(@Nonnull final Callable action) {
    final Arez_RepositoryWithRawType entity = new Arez_RepositoryWithRawType(action);
    attach( entity );
    return entity;
  }

  @Override
  @Action(
      reportParameters = false
  )
  public void destroy(@Nonnull final RepositoryWithRawType entity) {
    super.destroy( entity );
  }
}