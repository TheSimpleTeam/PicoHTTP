import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import net.thesimpleteam.picohttp.PicoHTTP;

import static org.junit.jupiter.api.Assertions.*;
import static net.thesimpleteam.picohttp.PicoHTTP.writeHTML;

class TestHTTP {

    final String helloWorldText = "<!DOCTYPE html><html><head><title>PicoHTTP - Test</title></head><body><h1>Hello gentleman, I'm Minemobs and this is a test for PicoHTTP/0.1</h1></body></html>";
    
    @Test
    void run() throws IOException {
        try(PicoHTTP http = new PicoHTTP(8080)) {
            http.addRoute("/", this::helloWorld);
            http.run();
            URL url = new URL("http://localhost:8080");
            String str = new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
            assertArrayEquals(str.toCharArray(), helloWorldText.toCharArray());
        }
    }

    private void helloWorld(Socket client, BufferedOutputStream os) {
        try {
            os.write(writeHTML(200, "Ok", helloWorldText).getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}