import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.thesimpleteam.picohttp.Client;
import net.thesimpleteam.picohttp.ContentTypes;
import net.thesimpleteam.picohttp.HTTPMethods;
import net.thesimpleteam.picohttp.Path;
import net.thesimpleteam.picohttp.PicoHTTP;

import static org.junit.jupiter.api.Assertions.*;

class TestHTTP {

    final String getTextRequest = "<!DOCTYPE html><html><head><title>PicoHTTP - Test</title></head><body><h1>Hello gentleman, I'm Minemobs and this is a test for PicoHTTP/0.1</h1></body></html>";
    final String postTextRequest = "{\"message\": \"Hello World\"}";
    static final Logger LOG = Logger.getLogger("Test");
    static final int port = 8080;
    static PicoHTTP http;

    @BeforeAll
    static void setup() throws IOException {
        http = new PicoHTTP(port);
        http.addRoutes(TestHTTP.class, new TestHTTP());
        http.run();
    }

    @Test
    void testGet() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).GET().build();
        var client = HttpClient.newBuilder().version(Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> res = null;
        try {
            res = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            fail(e);
            return;
        }
        assertNotNull(res);
        assertEquals(res.body(), getTextRequest);
    }
    
    @Test
    void testPost() throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/")).POST(BodyPublishers.ofString("Hello World")).build();
        var client = HttpClient.newBuilder().version(Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> res = null;
        try {
            res = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            fail(e);
            return;
        }
        assertNotNull(res);
        assertEquals(res.body(), postTextRequest);
    }

    @AfterAll
    static void clean() throws IOException {
        http.close();
    }

    @Path(value = "/", method = HTTPMethods.POST)
    public void testPost(Client client) throws IOException {
        client.send(200, "Ok", ContentTypes.JSON, client.data().equalsIgnoreCase("Hello World") ? postTextRequest : "no");
    }

    @Path("/")
    private void helloWorld(Client client) throws IOException {
        client.send(200, "Ok", ContentTypes.HTML, getTextRequest);
    }
}