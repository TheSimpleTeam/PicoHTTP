import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import net.thesimpleteam.picohttp.Client;
import net.thesimpleteam.picohttp.ContentTypes;
import net.thesimpleteam.picohttp.Path;
import net.thesimpleteam.picohttp.PicoHTTP;

import static org.junit.jupiter.api.Assertions.*;

class TestHTTP {

    final String helloWorldText = "<!DOCTYPE html><html><head><title>PicoHTTP - Test</title></head><body><h1>Hello gentleman, I'm Minemobs and this is a test for PicoHTTP/0.1</h1></body></html>";
    
    @Test
    void run() throws IOException {
        try(PicoHTTP http = new PicoHTTP(8080)) {
            //http.addRoute("/", this::helloWorld);
            http.addRoutes(TestHTTP.class, this);
            http.addRoute("/build.gradle", (t) -> t.send(200, "Ok", ContentTypes.PLAIN, read("build.gradle")));
            http.run();
            URL url = new URL("http://localhost:8080");
            try(var is = url.openStream()) {
                assertEquals(new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8), helloWorldText);
            }
        }
    }

    private String read(String path) {
        String content = "";
        try {
            content = Files.readString(Paths.get(path));
        } catch(IOException ignored) {}
        return content;
    }

    @Path("/")
    private void helloWorld(Client client) throws IOException {
        client.send(200, "Ok", ContentTypes.HTML, helloWorldText);
    }
}