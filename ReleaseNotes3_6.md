**Warning:** Release 3.6 contains some API changes due to consolitations and simplifications of the framework. This may result in compiler errors when you switch to panama-3.6.0. See the relative items below on how to handle API changes - mosten times it's just fixing the import statements.


  * Upgraded Ebean to version 3.2.1
    * New dependency to slf4j-1.7.5.jar
    * **Important** You have to add the line `ebean.search.jars=panama.jar` to your `ebean.properties`
    * Implementation of SLF4J (used by Ebean) for our simple logger
  * Consolidated Form construction and FormData#applyTo methods (API change - use the consolidated methods if necessary)
  * Goodbye message now takes name from context instead of filter
  * Removed FilterExtensions - they were hardly used and quite complicated to understand (API  change - implement and use your own Filter if necessary).
  * Added Factory Methods to Filter for common Expression filters like gt, lt, ...
  * Made filter package a sub-package of collections, as it belongs there logically (API change - adopt import statements).
  * Added setFilter(), removeFilter(), clearFilters() to Table for cleaner Filter handling.
  * Refactored TableController and TreeController allowing new methods getTable() and getTree() in BaseController.
  * Repackaged the JSON classes from org.json to panama.json (API change - adopt import statements)
  * Repackaged the simple log classes into panama.log (API change - adopt import statements)
  * Support for Multi-Lingual data with LocalizedPersistentBean (rudimentary PolyglotPersistentBean got dropped). See panama-examples/polyglot for a concrete example.