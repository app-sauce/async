# app-sauce.async

Reliable, asynchronous, concurrent processing.

Utilities that extend and play nicely with [core.async](https://github.com/clojure/core.async).

* timer: timers using core.async timeouts
* worker: reliable workers with core.async for input and output
* pipeline: utility functions for setting up pipelines of workers


## Getting Started

For Leiningen or Boot:

```clojure
[app-sauce/async "0.6.0"]
```

For deps.edn:

```clojure
app-sauce/async {:mvn/version "0.6.0"}
```

See the source for documentation on the API.


## License

Copyright © 2015-2019 App Sauce, LLC

Distributed under the Eclipse Public License.
