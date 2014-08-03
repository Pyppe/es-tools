es-tools
========

Some helpful tools for [Elasticsearch](http://www.elasticsearch.org).

## Usage
```bash
# See help
java -jar es-tools.jar --help

# Save all entries of an index to a file
java -jar es-tools.jar save-to-file http://localhost:9200/twitter /tmp/twitter.json

# Stream all entries of an index to another Elasticsearch-destination (same or different instance)
java -jar es-tool.jar re-index http://host1:9200/twitter http://host2:9200/twitter_v2
```
