<a href='Hidden comment: 
= Table of Contents =

<wiki:toc max_depth="2" />
'></a>

# Introduction #

## Philosophy ##

The goal of Panama is, to make it easy to create web applications with Java. Panama tries not to hide the mechanics of a web-server. There are no secrets and no magic. Panama aims to be as simple as possible in itself and to make it as simple and easy as possible to create web applications. Therefore if comes with "some batteries included": There's Velocity for the templates and there's Avaje Ebean ORM as a database layer, both of which tightly integrated.

## General approach ##

Panama consists of a Dispatcher which is implemented as a Filter and Controller classes (written by you) providing Action methods. Every request sent to a Panama application contains the name (or alias) of a Controller and an Action. The Dispatcher parses the request and invokes the action. Every action returns a Target which mosten times is a Velocity template to be rendered but can also be another Action to be invoked or a redirect or a JSON result etc.

Panama uses the "open session in view" pattern for database connections, which means that the database-session is still open while the template is rendered. This way, your application can make great use of lazy loading (see the Tree example for a very impressive example how little code is needed this way).


---


# Main Coding Parts #

This section describes the coding part of your web application (in contrast to the template part, which is described below). There are four main parts you will need to know.

## Controllers ##

Controllers are classes that provide a set of action methods. Which and how many actions you put in one controller is up to you, but normally you’d put actions together that a logically connected (like create, update, delete and list for some entity type). A simple rule of thumb could be to put all actions in one controller that should share the same URL prefix.

To be recognized as a controller by Panama, a class must be derived from the `BaseController` class _and_ must be annotated with the `@Controller` annotation. This annotation has an optional `alias` property that allows you to give your controller class a nice short name, which can also be used in the URL. If you do not provide an alias, you have to specify the fully qualified class name of your controller in the URL (which might look a bit odd). Note, that even if your controller has an alias, you can also use the fully qualified class name instead.

### Default Controller ###

By setting the optional annotation property `isDefaultController=true`, you can make a controller the default controller of your project. Now, if an URL contains nothing but the servlet-context, a redirect to the same URL with the controller's name resp. alias is issued.

**Caution**: In order to make this work as intended, make sure you have neither a `welcome-file-list` in your `web.xml`, nor any of the default welcome-files (like `index.html`, `index.jsp`) in your web-content folder. Otherwise, the web-server will automatically add the name of the welcome-file to the URL and so that file will be shown.

**Caution**: You will also want to specify a `defaultAction` for the controller (see below).

**Caution**: Do not make more than one controller a default controller. This will lead to unpredictable behaviour.

## Actions ##

Actions are methods of a controller class. To be recognized as an action by Panama, a method must have the signature `public Target actionName()` and must be annotated with the `@Action` annotation. This annotation has an optional `alias` property that allows you to give your action a name that differs from the method name.

### Default Action ###

Per Controller you may specify one of the actions to be the default action. The default action will be invoked if an URL only specifies a controller but no action. You specify the name of the default action using the optional `defaultAction` attribute of the `@Controller` annotation.

**Caution:** The aliases for controllers and actions must not contain any slashes (/). Aliases containing slashes are ignored and a warning is logged at startup. The controllers resp. actions can still be accessed via their real name (FQCN for controllers, method name for the actions).

## Targets ##

Targets are objects returned by Actions and tell Panama what to do next. Most of the time the `TemplateTarget` will be used,  but there’s also `RedirectTarget`, `PlainTextTarget` and even `JSONTarget` (useful for responding to AJAX requests). And you can easily provide your own Targets as well, just extend the abstract `Target` class and implement the `go()` method.

## Context ##

For each and every request, the system creates an initializes a thread-local instance of the `Context` class, which provides you with all you’ll need in your actions:

  * current request
  * current response
  * current session
  * access to application scope
  * access to all request parameters
  * the locale preferred by the current user
  * localized strings
  * token support
  * easy ways to put values into request-, session- and application scope for use in templates
  * ...

There are two ways for you, to get access to the current context:

  * From within controller classes, i.e. when you’re writing your action methods, you can use the `context` member variable, which is provided by the `BaseController` class and is automatically initialized with the current context (even if you create a controller instance yourself).

  * Outside of controller classes - e.g. in some helper class - you can use the static method `Context.getInstance()` to get your `Context` instance for the current request.

### One `Context` instance per request ###

Note, that for each request there is _exactly one instance_ of the `Context` class. All controllers created and all calls to `Context.getInstance()` during one request will always return the same object. This is quite useful, as anything you put into context at some place will also be there everywhere else, so there is no need to pass a lot of objects around. Very importantly, even during render-time in your Velocity templates you’ll have the same context instance at your disposal as `$context`.

Note that for your convinience everything you put into context with `context.put()` is directly available in the template by it’s key, without need for `$context`. That means, if you do `context.put(“foo”, “bar”)` in your action, the value is directly available as variable `$foo` in your template (`$context.foo` is possible too).

So if you put something into the context in your action class like so:
```
context.put("foo", "bar")
context.session.put("answer", 42)
context.application.put("list", new ArrayList<String>())
```

you can access it in your template like so:

```
$foo
$context.foo
$context.session.answer
$context.application.list
```

**Caution:** Panama automatically puts the controller object itself into the `context` by it's FQCN and it's alias if one is specified. So instead of putting stuff into the context you also can provide public methods in your controller and invoke them from within your templates. Thus, make sure you do not give your controller an alias name that matches any of the velocity tools you intend to use in templates rendered by that controller, because putting the controller object into request context using that alias would "shadow" the tool.


---


# Templates #

Panama uses [Apache Velocity](http://velocity.apache.org) for it’s templates. Velocity is fully integrated with Panama, no entry in _web.xml_ is required.

The configuration of Velocity is done via `velocity.properties` in the _src_ root directory. A minimal version would look something like this:

```
resource.loader = class
class.resource.loader.class = org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
runtime.log.logsystem.class=panama.util.SimpleLogChute
input.encoding = UTF-8
output.encoding = UTF-8
```

Please see the `velocity.properties` of the _panama-examples_ project for more options and explanations and of course the [official documentation](http://velocity.apache.org/engine/releases/velocity-1.7/developer-guide.html#Velocity_Configuration_Keys_and_Values).

If no `velocity.properties` file is found, Panama uses a reasonable [set of default properties](http://code.google.com/p/panama/source/browse/trunk/panama/src/panama-default-velocity.properties).

## Pre-Set Variables ##

There are a bunch of preset variables available for use in your templates:

### `$context` ###

This is the `Context` object you also have in your controller (see above).

### `$request`, `$response` ###

Access to the request and response objects.

### `$<controller-FQCN>` and `$<controller-alias>` ###

Panama automatically puts the controller instance of the executed action into the context by it’s fully qualified class name, as well as by it’s alias name (if any is set in the @Controller annotation). In both cases, each dot in the name is replaced by an underscore, as dots are understood by velocity as a separation of objects. So the `GuestbookController` from the _panama-examples_ project is available via `$panama_examples_guestbook_GuestbookController` as well as via `$guestbook`.

## Tools ##

Tools are a powerful feature of Velocity as described [here](http://velocity.apache.org/tools/devel/).

### Default-Tools ###

Panama has [VelocityTools 2.0](http://velocity.apache.org/tools/releases/2.0/) included. All its [GenericTools](http://velocity.apache.org/tools/releases/2.0/generic.html) and [VelocityView Tools](http://velocity.apache.org/tools/releases/2.0/view.html) are automatically available for use in the templates. They are available via their default names.

  * [$math](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/MathTool.html)
  * [$field](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/FieldTool.html)
  * [$text](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/ResourceTool.html)
  * [$alternator](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/AlternatorTool.html)
  * [$sorter](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/SortTool.html)
  * [$esc](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/EscapeTool.html)
  * [$convert](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/ConversionTool.html)
  * [$class](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/ClassTool.html)
  * [$xml](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/XmlTool.html)
  * [$number](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/NumberTool.html)
  * [$display](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/DisplayTool.html)
  * [$date](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/DateTool.html)
  * [$loop](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/LoopTool.html)
  * [$render](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/generic/RenderTool.html)
  * [$browser](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/view/BrowserTool.html)
  * [$pager](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/view/PagerTool.html)
  * [$link](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/view/LinkTool.html)
  * [$import](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/view/ImportTool.html)
  * [$params](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/view/ParameterTool.html)
  * [$cookies](http://velocity.apache.org/tools/releases/2.0/javadoc/org/apache/velocity/tools/view/CookieTool.html)

Additionally the following Panama tools are also automatically available:

  * [$tiles](#Tiles.md)
  * [$null](http://wiki.apache.org/velocity/NullTool) (this tool is taken from Velocity Wiki)

### Your own Tools ###

You can also provide your own Velocity tools. To do so, create your tool classes and `tools.xml` file as explained here. Place your `tools.xml` file in your _src_ root directory. See the _panama-examples_ for an example.

### Overwriting ###

Note, that you can overwrite the tools with your own stuff, using #set(). <br>
Hint: If you need e.g. a variable <code>$xml</code> and still want to use the <code>$xml</code> tool, just assign the tool to another variable in your template:<br>
<br>
<pre><code>#set($xmltool = $xml)<br>
#set($xml = 'foo')<br>
</code></pre>

Note, that overwriting only takes place within your template. You do not remove or delete the tool itself and it can still be accessed by it’s default name in other templates.<br>
<br>
<hr />

<h1>Configuration</h1>

<h2>Panama</h2>

Panama does not require any configuration to work. However there are a few optional parameters, which can be set as init-parameters for the filter declaration in <i>web.xml</i>.<br>
<br>
<table><thead><th> param-name         </th><th> param-value </th><th> Explanation </th></thead><tbody>
<tr><td> <code>panama.languages</code> </td><td> <code>en,de,fr</code> </td><td> A comma separated list of languages your application supports.<br>See chapter <a href='#Internationalization.md'>#Internationalization</a> for details. </td></tr>
<tr><td> <code>panama.maxfileuploadsize</code> </td><td> <code>16</code> </td><td> The maximum file upload size in MByte. The default values is 1 MByte. </td></tr></tbody></table>

e.g. to set maxfileuploadsize to 16MB:<br>
<pre><code>&lt;init-param&gt;<br>
    &lt;param-name&gt;panama.maxfileuploadsize&lt;/param-name&gt;<br>
    &lt;param-value&gt;16&lt;/param-value&gt;<br>
&lt;/init-param&gt;<br>
</code></pre>

<h2>SimpleLog</h2>

SimpleLog requires a <code>simplelog.properties</code> file in the <i>src</i> root directory. Actually the file may even be empty, but please see the <a href='http://code.google.com/p/panama/source/browse/trunk/panama-kickstart/src/simplelog.properties'>simplelog.properties</a> file from <i>panama-kickstart</i>, which contains a good documentation in form of comments.<br>
<br>
<h2>Avaje Ebean ORM</h2>

If you're using the integrated Persistence layer based on Avaje Ebean ORM, you'll have to provide an <code>ebean.properties</code> file in the <i>src</i> root directory. Please see the <a href='#Persistence.md'>#Persistence</a> section below and <a href='http://www.avaje.org'>http://www.avaje.org</a> for further information.<br>
<br>
<h2>Custom</h2>

If you need some configuration of your own, Panama has limited support for that, in that you can add your own custom init-parameters to the filter declaration in <i>web.xml</i> and access them via <code>context.getCore().getInitParam(key)</code> from within your controller classes. If that is not enough for your needs you'll have to come up with something by your own (<a href='CustomConfigurationHowTo.md'>Hint</a>).<br>
<br>
<hr />

<h1>Forms</h1>

In Panama, forms are defined using the <code>Form</code> and <code>Field</code> classes like so:<br>
<pre><code>Form myForm = new Form();<br>
myForm.addField(new StringField("firstname"));<br>
myForm.addField(new DateField("birthday", false, "yyyy-MM-dd"));<br>
</code></pre>
Usually this is done within a <code>static</code> block in your controller class, as this is only a definition of the form and as long as it isn't changed during run time, only one instance is needed and can be shared by all instances of your controller.<br>
<br>
<b>Note</b> The <code>Form</code> class has some other constructors too, e.g. <code>new Form(Foo.class)</code> would create a form with <code>Field</code>s for all properties of class <code>Foo</code> or <code>Form(Field... fields)</code> which allows for a cleaner and shorter version of the above:<br>
<pre><code>Form myForm = new Form(<br>
    new StringField("firstname"),<br>
    new DateField("birthday", false, "yyyy-MM-dd"));<br>
</code></pre>

<h2>Fields</h2>

Panama provides <code>Field</code> classes for all kinds of input: There's <code>StringField</code>, <code>IntegerField</code>, <code>BooleanField</code> and even a <code>DateField</code>. Please see the <a href='http://panama.googlecode.com/svn/trunk/panama/doc/panama/form/package-summary.html'>JavaDoc</a> for a complete list. And you can easily create and use your own <code>Field</code> classes as well, if needed.<br>
<br>
<h2><code>FormData</code></h2>

While a <code>Form</code> is merely a set of <code>Field</code>s, an instance of <code>FormData</code> represents one set of input data, entered into the form. <code>FormData</code> is used in two directions:<br>
<br>
<ol><li>HTML => Java : Mapping input into a HTML form to a Java representation of the entered data<br>
</li><li>Java => HTML : Providing Java data in a convenient way to be used within templates</li></ol>

<h3>HTML => Java</h3>

Let's start with a simple HTML form for our example from above:<br>
<br>
<pre><code>&lt;form name="f" method="post" action="save"&gt;<br>
Your name: &lt;input type="text" name="firstname" /&gt;&lt;br /&gt;<br>
Your birthday: &lt;input type="text" name="birthday" /&gt;&lt;br /&gt;<br>
&lt;input type="submit" name="submit" value="Submit" /&gt;<br>
&lt;/form&gt; <br>
</code></pre>

This will give us a simple form with two input fields and a Submit button.<br>
<br>
Now let's implement the action to handle the input.<br>
<br>
<pre><code>@Action<br>
public Target save() {<br>
  FormData fd = new FormData(myForm)                   // 1<br>
                       .withDataFromRequest(context);  // 2<br>
  String firstName = fd.getString("firstName");        // 3<br>
  Date birthday = fd.getDate("birthday");              // 4<br>
}<br>
</code></pre>

In line (1) we create an instance of <code>FormData</code> based on our form.<br>
<br>
In line (2) we fill our form data object with data from the request.<br>
<br>
In line (3) we fetch the first name, entered into the HTML form; nothing special here.<br>
<br>
In line (4) we fetch the birthday, entered into the HTML form; here the input is automatically converted to a <code>Date</code> object, using the pattern we provided when creating the <code>DateField</code>. If this goes wrong, <code>getDate()</code> returns <code>null</code> and the error is remembered within our form data's <code>errors</code> structure. This is explained in depth in the section about <a href='#Input_Validation.md'>#Input_Validation</a> below.<br>
<br>
Starting with version 3.4.2 lines (1) and (2) can be combined by using <code>FormData fd = new FormData(myForm).withDataFromRequest(context);</code>

<h3>Java => HTML</h3>

If you want to edit an existing set of data you'll want so somehow prefill the HTML form that's shown to the user. For this, you create a <code>FormData</code> object and provide input values in Java and then put it into the context so it can be accessed from within your HTML template.<br>
<br>
<pre><code>@Action<br>
public Target edit() {<br>
    FormData fd = new FormData(myForm);<br>
    fd.setInput("firstName", "Ridcully");<br>
    fd.setInput("birthday", new Date());<br>
    return showForm(fd);<br>
}<br>
<br>
// private method that will come handy when we're using validation.<br>
private Target showForm(FormData fd) {<br>
    context.put("formdata", fd);<br>
    return render("template.vm");<br>
}<br>
</code></pre>

<h2>Input Validation</h2>

Panama comes with automated, "lazy" input validation. Validation is done by the <code>FormData</code> class when, (and only if) you access the input it has stored for a certain field. As described above, you access the input using the <code>FormData#get...</code> methods or (indirectly) via <code>FormData.applyTo()</code>.<br>
<br>
The validation is done based on<br>
<br>
<ul><li>The type of field you specified when defining the Form, e.g. a <code>DateField</code>
</li><li>The <code>get...</code> method you use on the FormData (e.g. calling <code>getDate()</code> on a <code>StringField</code> would give an error)<br>
</li><li>Any additional <code>Validator</code>s added to the Field when defining the Form.</li></ul>

Validation errors are collected by <code>FormData</code> and can be checked and accessed via <code>FormData#hasErrors()</code> resp. <code>FormData#getErrors()</code>.<br>
<br>
<b>Note</b> The validation process for a field is stopped as soon as any of these steps fails - so, there's at most one validation error per field at any time (although an input might violate several rules at once).<br>
<br>
<b>Note</b> Unlike other frameworks, Panama does not automatically force the user back to the input form when errors occur. It's up to you to check if there are errors and what to make of it.<br>
<br>
Now, let's extend our example from above.<br>
<pre><code>@Action<br>
public Target save() {<br>
  FormData fd = new FormData(myForm)                   // 1<br>
                       .withDataFromRequest(context);  // 2<br>
  String firstName = fd.getString("firstName");        // 3<br>
  Date birthday = fd.getDate("birthday");              // 4<br>
<br>
  if (fd.hasErrors()) {                                // 5<br>
    return showForm(fd);                               // 6 <br>
  }<br>
  <br>
}<br>
</code></pre>

The validation already happens without anything for us to do, in line (3) and (4).<br>
Now in line (5) we check if there were errors, and if so, we render the form template again. We reuse the <code>showForm</code> method from above, passing or <code>FormData</code> object, which still contains the user's original inputs as well as all the validation errors.<br>
<br>
By extending the template a little, we can easily show those inputs and hints about errors as well:<br>
<br>
<pre><code>TODO<br>
</code></pre>

<h3>Lazy Validation</h3>

<b>Note</b> Due to the lazy validation approach, it's crucial, that you fetch the needed data from the FormData object <b>before</b> you can check if there are Validation errors.<br>
<br>
<hr />

<h1>Tables</h1>

As the presentation of tabular data is very common in web applications (especially for backends or administration areas), we've included some features into Panama that make creating these kinds of things very easy. With Panama you can out of the box have<br>
<br>
<ul><li>Tables backed with data from arbitrary sources, with special support for data from database tables.<br>
</li><li>Sorting tabular data by any column.<br>
</li><li>Paging for tabular data.<br>
</li><li>Filters of arbitrary complexity.<br>
</li><li>Fulltext search filter.</li></ul>

<h2>Filters</h2>

To restrict your table to only show data with certain characteristics, you can apply any number of filters to the table. The <code>Filter</code> class provides a lot of factory methods to very easily create all sorts of filters, but you also can create your own implementations by extending <code>Filter</code> and implementing the <code>match()</code> method.<br>
<br>
To only show data where property <i>name</i> has the value "panama":<br>
<pre><code>getTable("tablename").setFilter("filtername", Filter.eq("name", "panama"));<br>
</code></pre>

For classical full-text search in a bunch of properties there's the <code>stdSearchFilter()</code> factory method.<br>
<pre><code>getTable("tablename").setFilter("search", Filter.stdSearchFilter("query", "firstName", "lastName", "street", "city"); <br>
</code></pre>
This filter matches, if any of the values of the given properties matches (in a "LIKE" or "contains" sense) the given query string. So, if you pass e.g. "Jo" as the query, table will contain only items where e.g. firstName is John or lastName is Mojo etc.<br>
<br>
To remove all filters:<br>
<pre><code>getTable("tablename").clearFilters(); <br>
</code></pre>

<h1>Internationalization</h1>

Externalize strings to <code>resources.properties</code> resp. <code>resources_&lt;lang&gt;.properties</code> files in your <i>src</i> root directory.<br>
<br>
<h2>Usage in Templates</h2>

In your templates use the <code>$text</code> tool to write the strings:<br>
<br>
<table><thead><th> <code>$text.hello.world</code>         </th><th> Hello World!              </th></thead><tbody>
<tr><td> <code>$text.bar</code>                 </td><td> The args are {0} and {1}. </td></tr>
<tr><td> <code>$text.bar.insert(4)</code>       </td><td>  The args are 4 and {1}.  </td></tr>
<tr><td> <code>$text.bar.insert(4, true)</code> </td><td>  The args are 4 and true. </td></tr></tbody></table>

<h2>Usage in Code</h2>

In your code, use the <code>Context#getLocalizedString()</code> methods provided by the <code>Context</code> class:<br>
<br>
<table><thead><th> <code>context.getLocalizedString("hello.world")</code>  </th><th> Hello World!             </th></thead><tbody>
<tr><td> <code>context.getLocalizedString("bar", 4, true)</code>	</td><td> The args are 4 and true. </td></tr></tbody></table>

<h2>How Panama selects the language to use</h2>

By default, Panama matches the user's preferred languages (as set in her browser) and the languages your application supports (see <a href='#Configuration.md'>#Configuration</a> below). Alternatively, you can explicitly use the <code>Context#setLocale()</code> method to set the language to use, e.g. if you allow the user to explicitly select a language in your application. The locale is stored in session scope.<br>
<br>
If no resources property file exists for the selected language, the standard fallback procedure to the default resources happens.<br>
<br>
<b>See also</b> InternationalizationPitfalls<br>
<br>
<b>See also</b> the <a href='http://code.google.com/p/panama/source/browse/#svn%2Ftrunk%2Fpanama-examples%2Fsrc%2Fpanama%2Fexamples%2Fi18n'>Internationalization example</a> in the panama-examples project.<br>
<br>
<hr />

<h1>Tiles</h1>

Panama allows you to embed the results of other actions within your templates. This is different from the simple <code>#include()</code> or <code>#parse()</code> functionality provided by Velocity, in that it allows the reuse of business logic in different places -- in some way it’s a lightweight version of portlets.<br>
<br>
Imagine you have a webshop and want to show the user’s current shopping cart on every page. Without tiles, you’d have to ensure in every action that everything needed for rendering the cart is provided and calculated and in every template you’d have to include another template that renders the cart.<br>
<br>
With tiles you simply create an action that’s only responsible for the shopping cart. It might provide a list of all the user’s items and computes some total sum and shipping costs and so on and renders on to a velocity template that is not a complete HTML page, but just a snippet showing the items and costs.<br>
<br>
Use the <code>$tiles</code> tool to embed the result of actions within your template like so:<br>
<br>
<pre><code>$tiles.embed("controller", "action", {"param1":"value"})<br>
</code></pre>

The optional parameter map extends or the alters the original request parameters while invoking the action. Afterwards the parameters are restored to the original values.<br>
<br>
<b>See also</b> InternationalizationPitfalls<br>
<br>
<b>See also</b> the <a href='http://code.google.com/p/panama/source/browse/#svn%2Ftrunk%2Fpanama-examples%2Fsrc%2Fpanama%2Fexamples%2Ftiles'>Tiles example</a> in the panama-examples project.<br>
<br>
<hr />

<h1>Persistence</h1>

Panama provides some support for creating web apps using databases with the Ebean ORM.<br>
<br>
<h3>Configuration</h3>

Ebean requires an ebean.properties file in the <i>src</i> root directory of your application.<br>
<br>
<b>IMPORTANT</b> As of Ebean 3.x you have to add the line <b><code>ebean.search.jars=panama.jar</code></b> to your <code>ebean.properties</code> for Ebean to search the panama.jar for <code>Entity</code> resp. <code>Embeddable</code> artifacts.<br>
<br>
<br>
<h2>Ebean ORM with JNDI</h2>

For production environment, we recommend to use a JNDI datasource instead of Ebean's internal connection pool. Here is, how that can be archieved:<br>
<br>
<h3>META-INF/context.xml</h3>

First, create a <code>context.xml</code> file in the <code>META-INF</code> directory of your project (instead of that, you could also specify a datasource in your application server's server.xml, but if you do it like explained here, everything is nicely self-contained).<br>
<br>
<pre><code>&lt;?xml version="1.0" encoding="UTF-8"?&gt;<br>
&lt;Context&gt;<br>
  &lt;Resource <br>
    name="jdbc/ExampleDB" <br>
    auth="Container" <br>
    type="javax.sql.DataSource"<br>
    maxActive="100" <br>
    maxIdle="30"<br>
    maxWait="10000"<br>
    username="&lt;username for database&gt;" <br>
    password="&lt;password for database&gt;"<br>
    driverClassName="com.mysql.jdbc.Driver"<br>
    url="jdbc:mysql://localhost:3306/exampledatabase"<br>
    validationQuery="select 1 from dual;"<br>
		<br>
    removeAbandoned="true"<br>
    logAbandoned="true"<br>
    defaultTransactionIsolation="READ_COMMITTED"<br>
    defaultAutoCommit="false"<br>
  /&gt;<br>
  &lt;WatchedResource&gt;WEB-INF/web.xml&lt;/WatchedResource&gt;<br>
  &lt;WatchedResource&gt;META-INF/context.xml&lt;/WatchedResource&gt;<br>
&lt;/Context&gt;<br>
</code></pre>

For more information about JNDI configuration, please see the <a href='http://tomcat.apache.org/tomcat-6.0-doc/jndi-datasource-examples-howto.html'>JNDI Datasource HOW-TO</a>.<br>
<br>
<h3>web.xml</h3>

Add the following snippet to you<code>r web.xml</code> file.<br>
<br>
<pre><code>&lt;resource-ref&gt;<br>
  &lt;description&gt;DB Connection&lt;/description&gt;<br>
  &lt;res-ref-name&gt;jdbc/ExampleDB&lt;/res-ref-name&gt;<br>
  &lt;res-type&gt;javax.sql.DataSource&lt;/res-type&gt;<br>
  &lt;res-auth&gt;Container&lt;/res-auth&gt;<br>
&lt;/resource-ref&gt;<br>
</code></pre>

<h3>ebean.properties</h3>

Finally, in your <code>ebean.properties</code>, specify the usage of your JNDI datasource like so:<br>
<br>
<pre><code>datasource.default=jndi<br>
# -------------------------------------------------------------  <br>
# JNDI DataSource  <br>
# -------------------------------------------------------------   <br>
ebean.jndi.dataSourceJndiName=java:comp/env/jdbc/ExampleDB<br>
ebean.datasource.factory=jndi<br>
ebean.datasource.jndi.prefix=java:comp/env/jdbc/ <br>
</code></pre>