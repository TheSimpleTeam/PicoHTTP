# PicoHTTP
A lightweight http server

:warning: **YOU SHOULD NOT use this library in production due to https://github.com/thesimpleteam/picohttp/issues/2.**

#### Install

<details>
  <summary>Using Gradle</summary>
  
```groovy
repositories {
  //...
  maven {
    url 'https://maven.thesimpleteam.net/snapshots'
  }
}

dependencies {
  //...
  implementation "net.thesimpleteam:picoHTTP:1.3-SNAPSHOT"
}
```
</details>

### Usage
<details>
  <summary>Usage</summary>

```java
public class Server {
  public static void main(String[] args) {
    try(PicoHTTP http = new PicoHTTP(8080)) {
      http.addRoutes(Server.class, new Server());
      http.addRoute("/test.js", (client) -> client.send(200, "OK", ContentTypes.JS, "console.log('Hello World')"));
      http.run();
      while(true) {} //It's a way to avoid closing the server
    }
  }

  //Automatically added
  @Path("/")
  public void helloWorld(Client client) throws IOException {
    client.send(200, "Ok", ContentTypes.PLAIN, "Hello World");
  }

  @Path(value = "/", method = HTTPMethods.POST)
  public void postExamle(Client client) throws IOException {
    String data = client.data();
    String contentType = client.getHeaders().get("Content-Type");
    //Your code
    client.send(501, "Not Implemented");
  }

  @Path("/hello/\\w+") //Regex example
  public void helloSomeone(Client client) throws IOException {
    String name = client.path().split("/")[2]; //When you split the path it should return something like {"", "hello", "(theName)"}
    client.send(200, "Ok", ContentTypes.PLAIN, "Hello " + name);
  }
}
```
</details>
