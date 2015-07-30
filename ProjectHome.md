# What is GwtRpcPlus? #
A layer between GWTs default HTTP-RPC-Mechanism, supporting ServerPush, QueueRequests and other communicationtypes (like Websockets).

## It supports ##
  * Full GWT-PRC-Serialiser support
  * Sending all Gwt-Rpc-Requests via Websockets or in Bundles, without changing a single line of client-code
  * ServerPush via the default GWT-RPC-API
  * AllowResend: to support Failover on ServerTimeouts
  * QueuedRequests: add many cancelable Requests so only one request is pending
  * Prioritised Queues: set the Priority of the Requests, so important ones are executed first
  * Bundle support: Bundle some Requests, to reduce HTTP-Calls (available at version 0.0.2)
[Changelog](https://code.google.com/p/gwtrpcplus/wiki/Changelog)

## How to add it to your project ##
Add the following to your pom.xml in dependencies
```
<dependency>
    <groupId>com.googlecode.gwtrpcplus</groupId>
    <artifactId>gwtRpcPlusClient</artifactId>
    <version>1.0.0</version>
</dependency>
```
[current available versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.googlecode.gwtrpcplus%22%20AND%20a%3A%22gwtRpcPlus%22)

**Server**

There is a Guice-Module (requires Guice-Servlet) setting up all needed stuff:
You need to pass all Servlets in this Module, that are using the Gwt-Rpc.
```
Guice.createInjector(
  ...
  new ModuleGwtRpcPlus("myProject", MyGwtServlet1.class, MyGwtServlet2.class),
  ...
);
```
**Client**

add the GwtRpcPlus to your myproject.gwt.xml
```
<module rename-to="myProject">
  ...
  <inherits name="com.googlecode.gwtrpcplus.GwtRpcPlus" />
  ...
</module>
```

now all your Gwt-Rpc-Requests will be send via GwtRpcPlus

**Other Features**

add bundle-support (Requests will be collected with GWTs Scheduler.scheduleFinally) [0.0.2]
```
<set-configuration-property name="gwtrpcplus_bundleHttpRequests"value="true" />
```

## Websocket Support ##

GwtRpcPlus supports websockets. It will route all requests through websockets when client and Server supports it. [1.0.0]

To enable WebsocketSupport, you need to setup the Client by adding the following to your myproject.gwt.xml
```
<set-configuration-property name="gwtrpcplus_websockets_enabled" value="true" />
```

At the Serverside, you need to add one of the GwtRpcPlus-WebsocketProjects to your ClassPath. The Guice ModuleGwtRpcPlus will automaticly try to add the Websocketsupport on startup.

**WARN**: If you use something of the HttpServletRequest in your Services, it may not work (throws Exceptions if not supported by the Websocket-Module yet).

For example JSR-356 Websockets (used in tomcat8):
```
<dependency>
    <groupId>com.googlecode.gwtrpcplus</groupId>
    <artifactId>gwtRpcPlusWebsockets</artifactId>
    <version>1.0.0</version>
</dependency>
```

## How to use QueuedRequests ##
To enable it, you just need extend your RemoteService from RemoteServicePlus and annotate your RemoteService like:
```
@RemoteServiceRelativePath("MyService")
public interface MyGwtService1 extends RemoteServicePlus {
  @Queued
  String test(String param1);
}
```
you can also set the amount of pending Requests with @Queued(2).

to change the Priority of the Requests, or cancel them, just add the Returntype "Request" to the Async-Part of the Interface, and cast it to PrioritisedRequest:
```
public interface MyGwtService1Async {
  @Queued
  Request test(String param1, AsyncCallback<String> callback);
}
```
```
PrioritisedRequest request = (PrioritisedRequest)service.test("foobar");
// set the priority of the Request
request.setPriority(20);
// or to cancel call
request.cancel();
```

## How to use ServerPush ##
ServerPush is designed like the original Gwt-RPC-Syntax.

**Interface**

The Interfaces looks like the original ones, but with the @ServerPush-Annotation:
```
@RemoteServiceRelativePath("MyService")
public interface MyGwtService2 extends RemoteServicePlus {
  @ServerPush
  String pushIt(String param1);
}
```
To be able to cancel the Serverpush from the Clientside, you can return the Request in the Async-Part and call request.cancel() on it:
```
public interface MyGwtService2Async {
  Request pushIt(String param1, AsyncCallback<String> callback);
}
```
```
Request request = service.pushIt("foobar", callback);
request.cancel();
```

**Server**

The GwtRpcPlus will call an other Method instead of the overwritten pushIt-Method. The implementing Servlet should implement the ServerPush like this:
```
@Singleton
public class MyGwtServlet2 extends RemoteServiceServlet implements MyGwtService2 {
  @Override
  public String pushIt(String param1) {
    assert (false) : "Serverpush: never called";
    return null;
  }

  private final ExecutorService executor = Executors.newCachedThreadPool();

  public CancelHandler pushIt(final String param1, final ReturnHandler<String> returnHandler) {
    // Do some Async stuff and inform the Client about the state
    executor.submit(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        returnHandler.answer(param1 + "0");
        Thread.sleep(1000);
        returnHandler.answer(param1 + "1");
        Thread.sleep(1000);
        // After finishing the Request, call the finish Method to stop the ServerPush
        returnHandler.finish(param1 + "2");
        return null;
      }
    });
   
    return new CancelHandler() {
      @Override
      public void onCancel() {
        // Client has canceled the Request
      }
    };
  }
}
```

**Client**

On the Client, you can use the default GwtRpc-Syntax, but the onSuccess Method will be invoked multiple times.
```
service.pushIt("foobar", new AsyncCallback<String>() {
  @Override
  public void onSuccess(String result) {
    // Called multiple times
  }
  @Override
  public void onFailure(Throwable caught) {
    // TODO do some stuff
  }
});
```
OPTIONAL: If you need more control, you can use the ServerPushCallback instead:
```
service.pushIt("foobar", new ServerPushCallback<String>() {
  @Override
  public void onAnswer(Result result) {
    // Called when more Answers are expected
  }
  @Override
  public void onFinish(String result) {
    // Called when the server sends the last Answer
  }
  @Override
  public void onException(Throwable caught) {
    // Same like the onFailure(caught); in the other Method
  }
  @Override
  protected void onResend() {
     // OPTIONAL: only used if the Serverpush also has the @AllowResend-Annotation
  }
});
```

## Compatibility ##
If you want to use the GwtRpcPlus for Services, extended from RemoteServices too, just add the following line to your .gwt.xml
```
<module>
  ...
  <inherits name="com.googlecode.gwtrpcplus.GwtRpcPlus" />
  ...
  <set-property name="gwtrpcplus_UseRemoteService" value="TRUE" />
  ...
</module>
```
This cause, that the default RemoteService Services will use the GwtRpcPlus too.