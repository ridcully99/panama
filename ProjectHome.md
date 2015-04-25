## Panama 3.7.0 is here (28 December 2013) ##

Please see the [Release Notes](ReleaseNotes3_7.md) for a list of all changes.

## Overview ##

Panama is an easy to use, fast and lightweight framework for creating web applications with Java. Panama aims to be as simple as possible but not simpler. Originally created as an "antithesis" to Struts and JSPs, Panama requires no configuration and uses [Velocity](http://velocity.apache.org) for templating. Even for persistence ([Avaje EBean ORM](http://www.avaje.org)) and logging (SimpleLog) we use libraries that are easier to use and have less overhead and dependencies than the usual ones.

## Example ##

**ExampleController.java**
```
@Controller(alias="controller")
public class ExampleController extends BaseController {
    @Action
    public Target action() {
        context.put("name", "Panama");
        return render("example.vm");
    }
}
```

**example.vm**
```
<html>
    <head><title>example</title></head>
    <body>
        <h1>Welcome to $name!</h1>
    </body>
</html>
```

**example request and result**

`http://example.com/controller/action` ---> **Welcome to Panama!**

## More Examples ##

  * [Tiny Guestbook](TinyGuestbook.md)
  * [panama-examples project from SVN](http://code.google.com/p/panama/source/browse/#svn%2Ftrunk%2Fpanama-examples)

## Getting Started ##

The best way to start with Panama is to head over to [GettingStartedWithEclipse](GettingStartedWithEclipse.md) and follow the 10 steps to your first Panama application.


## Documentation ##

  * [Documentation](Documentation.md)
  * [GettingStartedWithEclipse](GettingStartedWithEclipse.md)
  * [API Docs](http://panama.googlecode.com/svn/trunk/panama/doc/index.html)