package com.example.repository;

import dagger.Binds;
import dagger.Module;
import javax.annotation.Generated;
import javax.inject.Singleton;

@Generated("arez.processor.ArezProcessor")
@Module
public interface RepositoryWithMultipleCtorsRepositoryDaggerModule {
  @Binds
  @Singleton
  RepositoryWithMultipleCtorsRepository bindComponent(
      Arez_RepositoryWithMultipleCtorsRepository component);
}