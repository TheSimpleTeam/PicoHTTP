# PicoHTTP
A lightweight http server

#### Install

```groovy
repositories {
  //...
  maven {
    url 'https://maven.thesimpleteam.net/snapshots'
  }
}

dependencies {
  //...
  implementation "net.thesimpleteam:picoHTTP:1.2-SNAPSHOT"
}
```

### Usage

```java
public class Server {
  public static void main(String[] args) {
    try(PicoHTTP http = new PicoHTTP()) {
      http.addRoutes(Server.class, this);
      http.addRoute("/test.js", (client) -> client.send(200, "OK", ContentTypes.JS, "console.log('Hello World')"));
      http.run();
    }
  }

  //Automatically added
  @Path("/")
  public void helloWorld(Client client) throws IOException {
    client.send(200, "Ok", ContentTypes.PLAIN, "Hello World");
  }
}

```
