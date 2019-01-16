package com.example.repository;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.InjectMode;
import arez.component.internal.AbstractRepository;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("arez.processor.ArezProcessor")
@ArezComponent(
    nameIncludesId = false,
    inject = InjectMode.NONE
)
public abstract class InjectDisabledRepositoryRepository extends AbstractRepository<Integer, InjectDisabledRepository, InjectDisabledRepositoryRepository> {
  InjectDisabledRepositoryRepository() {
  }

  @Nonnull
  public static InjectDisabledRepositoryRepository newRepository() {
    return new Arez_InjectDisabledRepositoryRepository();
  }

  @Action(
      name = "create_name"
  )
  @Nonnull
  public InjectDisabledRepository create(@Nonnull final String name) {
    final Arez_InjectDisabledRepository entity = new Arez_InjectDisabledRepository(name);
    attach( entity );
    return entity;
  }

  @Override
  @Action(
      reportParameters = false
  )
  public void destroy(@Nonnull final InjectDisabledRepository entity) {
    super.destroy( entity );
  }
}
