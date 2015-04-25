# Introduction #

Getting started with a new web framework is not an easy task per se. To make it as easy as possible for you, we've set up this page that guides you step by step in creating your first Panama powered web application with Eclipse.
For information on how to use a database with Panama (which will mosten often be the case) please see [Documentation#Persistence](Documentation#Persistence.md).

# Prerequisites #

To follow this guide, you'll need an Eclipse IDE for Java EE Developers (3.5 or higher) and an application server (e.g. Tomcat) to the ready. (Of course you can use Panama and the examples in any other environment as well, but the example project and this page are optimized for the described environment.)

# Your First Panama Application in 10 Easy Steps #

  1. Create a Dynamic Web Application with Eclipse.
  1. [Download](http://code.google.com/p/panama/downloads/list) the latest **`panama-x.y.z.jar`** and put it into your project's `WEB-INF/lib` directory.
  1. [Download](http://code.google.com/p/panama/downloads/list) the matching **`panama-x.y.z-dependencies.zip`**, and unpack it into your project's `WEB-INF/lib` directory. There should now be a list of jar Files. These are required by Panama.
  1. Create a package named `panama.kickstart` in your project's `src` directory.
  1. Create a class file named `ExampleController.java` in there and edit it to look like this:
```
package panama.kickstart;

import panama.annotations.Action;
import panama.annotations.Controller;
import panama.core.BaseController;
import panama.core.Target;

@Controller(alias="controller")
public class ExampleController extends BaseController {
    @Action
    public Target action() {
        context.put("name", "Panama");
        return render("example.vm");
    }
}
```
  1. Create a text-file named `example.vm` in the same package and edit it to look like this:
```
<html>
    <head><title>example</title></head>
    <body>
        <h1>Welcome to $name!</h1>
    </body>
</html>
```
  1. Edit your project's `WEB-INF/web.xml` to look like this:
```
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd" id="WebApp_ID" version="2.5">
<display-name>your project's name</display-name>
  <filter>
    <filter-name>panama</filter-name>
    <filter-class>panama.core.Dispatcher</filter-class>
  </filter>
  <filter-mapping>
    <filter-name>panama</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>
</web-app>
```
  1. Publish your application onto your application server.
  1. Start your application server.
  1. Open this URL in your browser: `http://localhost:8080/<your project's name>/controller/action`.<br>
This should show a quite large <b>Welcome to Panama!</b> on your browser.<br>
Congratulations! You've just finished your first Panama Application.</li></ul>


---


# Your First Panama Application With Database Support #

## Ebean ORM and Eclipse Plugin ##

Panama uses the [Avaje Ebean ORM](http://www.avaje.org/) as it's Persistence Layer. To use it in your Eclipse projects, you must install the Ebean-Eclipse plugin (add `http://www.avaje.org/eclipseupdate/` to the list of Available Software Sites and install the plugin from there).
Background information about what the plugin does and why that is good, can be found in the [Ebean documentation](http://www.avaje.org/ebean/documentation.html).

After you have installed the plugin, you have to activate the plugin for your project, by selecting _Enable Ebean Enhancement_ from the project's context menu.

## Driver and Configuration ##

Now, add the driver-jar for your database to the `WEB-INF/lib` directory of your project and put an `ebean.properties` file directly into the `src` directory of your project. See Ebean's [Get Started with Ebean](http://www.avaje.org/ebean/getstarted_props.html) for further information.


---


# What next #

  * Have a look at the [panama-examples](http://code.google.com/p/panama/source/browse/#svn%2Ftrunk%2Fpanama-examples) project in the SVN and try it's examples.
  * Read the [Documentation](Documentation.md)
  * Have a look at the [Javadoc](http://panama.googlecode.com/svn/trunk/panama/doc/index.html)
  * Post your questions in our [Discussion Group](https://groups.google.com/group/panama-framework)