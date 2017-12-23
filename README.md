<p align="center"><img src="/docs/_icons/arez.png" alt="Arez" width="120"></p>

# Arez

[![Build Status](https://secure.travis-ci.org/arez/arez.png?branch=master)](http://travis-ci.org/arez/arez)
[<img src="https://img.shields.io/maven-central/v/org.realityforge.arez/arez-core.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.arez%22)
[![codecov](https://codecov.io/gh/arez/arez/branch/master/graph/badge.svg)](https://codecov.io/gh/arez/arez)

Arez is a simple, efficient and scalable state management library for client-side applications powered by a
transparent functional reactive programming core. Arez tracks usage of observable data and notifies observers
when the observable data changes. The library scales from simple, flat domain models to complex, highly
inter-connected graph-like domain models. Arez trades a higher memory usage for a faster execution speed
and better developer experience. Arez's optimizes for developer happiness by eliminating the need to monitor
or even think about when state changes and instead react to those changes on demand.

Arez is under heavy development and sometimes the documentation does not keep up to date. However the goal of
the toolkit is to be easy to use and this includes clear and concise documentation. If something is unclear
please [report it as a bug](https://github.com/arez/arez/issues) because it *is* a bug. If a new user
has a bad time then then we need to fix the problem.

For more information about Arez, please see the [Website](https://arez.github.io/). For the source code
and project support please visit the [GitHub project](https://github.com/arez/arez).

# Contributing

Arez was released as open source so others could benefit from the project. We are thankful for any
contributions from the community. A [Code of Conduct](CODE_OF_CONDUCT.md) has been put in place and
a [Contributing](CONTRIBUTING.md) document is under development.

# License

Arez is licensed under [Apache License, Version 2.0](LICENSE).

# Credit

* [Stock Software](http://www.stocksoftware.com.au/) for providing significant support in building and maintaining
  Arez. The company was willing take a risk and base their next suite of applications on an idea and the
  bet paid off.

* The toolkit began life as an attempt to port [Mobx](https://mobx.js.org/) to java. Not all of the Mobx
  primitives made sense in a java world. There were also places where it was felt that we could improve on
  the Mobx model, at least for our environment. The idea of a "port" was soon discarded and Arez became a
  reimplementation of similar concepts. However there are some places (i.e. `Transaction.completeTracking()`)
  where the Mobx heritage is clear. Credit goes to Michel Weststrate and the Mobx team for their clean conceptual
  model and some inspirational talks.

* Other frameworks that influenced the development of Arez include;
  - [Meteor Tracker](https://docs.meteor.com/api/tracker.html)
  - [VueJS watchers and Computed properties](https://vuejs.org/v2/guide/computed.html)
  -  KnockoutJS [Observables](http://knockoutjs.com/documentation/observables.html) and [Computed Observables](http://knockoutjs.com/documentation/computedObservables.html)

* The website is derived from the [Edition Template](https://github.com/CloudCannon/edition-jekyll-template)
  by [Cloud Cannon](https://cloudcannon.com/) that is licensed under the [MIT License](https://github.com/CloudCannon/edition-jekyll-template/blob/master/LICENSE).
  They also have amazing [tutorial website](https://learn.cloudcannon.com/) for developing jekyll based websites.
