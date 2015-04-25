# Panama Web-Framework

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
