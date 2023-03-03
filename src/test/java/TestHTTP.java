import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import net.thesimpleteam.picohttp.Client;
import net.thesimpleteam.picohttp.ContentTypes;
import net.thesimpleteam.picohttp.Path;
import net.thesimpleteam.picohttp.PicoHTTP;

import static org.junit.jupiter.api.Assertions.*;

class TestHTTP {

    final String helloWorldText = "<!DOCTYPE html><html><head><title>PicoHTTP - Test</title></head><body><h1>Hello gentleman, I'm Minemobs and this is a test for PicoHTTP/0.1</h1></body></html>";
    final Logger log = Logger.getLogger("Test");
    final int port = 8080;
    
    @Test
    void run() throws IOException {
        log.info("Test");
        try(PicoHTTP http = new PicoHTTP(port)) {
            http.addRoutes(TestHTTP.class, this);
            http.addRoute("/build.gradle", (t) -> t.send(200, "Ok", ContentTypes.PLAIN, read("build.gradle")));
            log.info("Added routes");
            http.run();
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port))
                .timeout(Duration.ofSeconds(3)).GET().build();
            var client = HttpClient.newBuilder().build();
            HttpResponse<String> res = null;
            try {
                res = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch(InterruptedException e) {
                fail(e);
                return;
            }
            assertEquals(res.body(), helloWorldText);
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