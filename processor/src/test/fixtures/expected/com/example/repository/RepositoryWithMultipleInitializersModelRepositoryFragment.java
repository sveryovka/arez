package com.example.repository;

import javax.annotation.Generated;
import javax.annotation.Nonnull;
import sting.Fragment;
import sting.Typed;

@Generated("arez.processor.ArezProcessor")
@Fragment
public interface RepositoryWithMultipleInitializersModelRepositoryFragment {
  @Nonnull
  @Typed(RepositoryWithMultipleInitializersModelRepository.class)
  default RepositoryWithMultipleInitializersModelRepository create() {
    return new Arez_RepositoryWithMultipleInitializersModelRepository();
  }
}
