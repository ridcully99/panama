## Introduction ##

Getting started with a new web framework is not an easy task per se. To make it as easy as possible for you to get to know Panama, we've provided a separate project for download and in the SVN with lots of examples and this wiki page that explains step by step how to get it to run. There'll be separate pages for every example, explaining all the details.

## Environment ##

The explanations here presume a certain environment:

  * Eclipse IDE for Java EE Developers (3.5 or 3.6) with an application server (e.g. Tomcat) to the ready. (Of course you can use Panama and the examples in any other environment as well, but the example project and this page are optimized for the described environment.)

## Step By Step ##

  1. Download the latest _panama-examples-x.y.zip_ file from the [Downloads section](http://code.google.com/p/panama-framework/downloads/list).
  1. Start Eclipse and import the downloaded zip-file into your workspace:
    * Open the import dialog using File/Import.../General/Existing Project into Workspace
    * Enable _Select Archive File_, click _Browse_ and select the downloaded zip-file.
    * Click Finish and you'll get a new Dynamic Web Project named panama-examples.
  1. Publish panama-examples onto your application server.
    * In the _Servers_ view, select _Add and remove..._ from the context menu of your application server.
    * In the upcoming dialog, select panama-examples from the Available column and click the Add button to move it to the Configured column.
    * Click the Finish button to confirm and close the dialog.
    * select _Publish_ from the context menu of your application server to publish the project.
  1. Start your application server
  1. Open this URL in your browser: http://localhost:8080/panama-examples

## Examples using database ##

Panama has [Ebean ORM](http://www.avaje.org/) built in for use as a persistence layer. There are some examples that show what you can do with it (and how easily you can do it).

In order to run those examples a few extra requirements and steps are necessary.

### Environment ###

  * MySQL Database (the driver is included in the examples project)
  * An empty database schema with UTF-8 encoding.<br>You can create one with <code>CREATE DATABASE panama_examples DEFAULT CHARACTER SET utf8;</code>
<ul><li>The <b>Ebean Eclipse IDE Enhancer Plugin</b><br>(see <a href='http://www.avaje.org/doc/ebean-userguide.pdf'>Reference Guide</a> chapter 15.5.5)</li></ul>

<h3>Additional steps</h3>

<ol><li>Edit <i>ebean.properties</i>
<ul><li>set the following properties according to your system.<br>
<ul><li><code>datasource.mysql.username</code>
</li><li><code>datasource.mysql.password</code>
</li><li><code>datasource.mysql.databaseUrl</code>
</li></ul></li><li>set the following properties to true (this will generate all required tables in the schema)<br>
<ul><li><code>ebean.ddl.generate</code>
</li><li><code>ebean.ddl.run</code>
</li></ul></li></ul></li><li>Build, deploy and restart your application server<br>
</li><li>Open the index URL in your browser: <a href='http://localhost:8080/panama-examples'>http://localhost:8080/panama-examples</a>
</li><li>Start one of the database depending examples<br>
</li><li>Stop the application server<br>
</li><li>Set <code>ebean.ddl.generate</code> and <code>ebean.ddl.run</code> to false<br>
</li><li>Build, deploy and restart your application server