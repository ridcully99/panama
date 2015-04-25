# Introduction #

If the possibilities described in [Documentation#Configuration](Documentation#Configuration.md) are not enough for your configuration needs, you might want to consider one of the ways described here.

# Custom Dispatcher with extended `init()` #

To be as plain and simple as it wants to be, Panama does not provide any callbacks or whatever to allow customization of the Dispatcher.

But you may simply create your own Dispatcher class by deriving from Panama's Dispatcher. In your own Dispatcher, override the `init()` method, which is invoked when your web application starts. Now you can implement everything you like, reading property- or xml-files etc.

Don't forget to change the `filter-class` in your _web.xml_ to your own Dispatcher class.

**Caution:** _The first thing you should do in your `init()`  method, is to call `super.init()`, so Panama gets initialized correctly._
