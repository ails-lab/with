# WITHCulture

WITHCulture is a service that provides access to digital cultural heritage items from different repositories
and offers a number of added-value services for the creative reuse and intelligent exploitation of that content.


* Federated and faceted search.
* API mashup from different digital CH resources, such as Europeana, the Digital Public Library of America, Rijksmuseum, the British Library, National Library of Australia, YouTube, Historypin etc.
* Access to a huge set of heterogeneous items (images, videos, different metadata schemata etc).
* Support of different data models (e.g., EDM, LIDO etc) and formats (e.g. XML, JSON-LD).
* Full-text indexing of cultural records.
* Automatic and manual enrichment of cultural data.
* User and Collection management system.

## Getting Started

[Download sbt](https://www.scala-sbt.org/download.html) and run:

```bash
$ sbt "run 9060" -jvm-debug 9999
$ sbt -Dconfig.file=conf/local.conf "run 9060" -jvm-debug 9999
```
