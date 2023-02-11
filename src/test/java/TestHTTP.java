import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.thesimpleteam.picohttp.Client;
import net.thesimpleteam.picohttp.ContentTypes;
import net.thesimpleteam.picohttp.PicoHTTP;

import static org.junit.jupiter.api.Assertions.*;

class TestHTTP {

    final String helloWorldText = "<!DOCTYPE html><html><head><title>PicoHTTP - Test</title></head><body><h1>Hello gentleman, I'm Minemobs and this is a test for PicoHTTP/0.1</h1></body></html>";
    
    @Test
    void run() throws IOException {
        try(PicoHTTP http = new PicoHTTP(8080)) {
            http.addRoute("/", this::helloWorld);
            http.addRoute("/build.gradle", (t) -> t.send(200, "Ok", ContentTypes.PLAIN, read(Path.of("build.gradle"))));
            http.run();
            URL url = new URL("http://localhost:8080");
            String str = new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
            assertArrayEquals(str.toCharArray(), helloWorldText.toCharArray());
        }
    }

    private String read(Path path) {
        String content = "";
        try {
            content = Files.readString(path);
        } catch(IOException ignored) {}
        return content;
    }

    private void helloWorld(Client client) {
        client.send(200, "Ok", ContentTypes.HTML, helloWorldText);
    }
}