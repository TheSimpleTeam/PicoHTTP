import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Main {

    private static final ExecutorService SERVICE = Executors.newCachedThreadPool();
    private static final Map<String, BiConsumer<Socket, BufferedOutputStream>> routes = new HashMap<>();

    public static void main(String[] args) {
        routes.put("/", Main::helloWorld);
        try (ServerSocket server = new ServerSocket(8080)) {
            while (true) {
                listenForRequests(server);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void helloWorld(Socket client, BufferedOutputStream os) {
        try {
            String text = "Hello gentleman, I'm Minemobs and this is a test for PicoHTTP/0.1";
            os.write(String.format(
                    "HTTP/1.0 200 I'm lazy\nServer: PicoHTTP/0.1\nContent-type: text/plain\nContent-Length: %d\n\n%s",
                    text.length(), text).getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void error404(Socket client, BufferedOutputStream os) {
        try {
            String text = "<!DOCTYPE html><html><head><title>PicoHTTP - Error 404</title></head><body><h1 style=\"text-align: center;\">Error 404</h1></body></html>";
            os.write(String.format(
                    "HTTP/1.0 404 Not found\nServer: PicoHTTP/0.1\nContent-type: text/html\nContent-Length: %d\n\n%s",
                    text.length(), text).getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void listenForRequests(ServerSocket server)
            throws IOException, ExecutionException, InterruptedException {
        Socket socket = server.accept();
        Future<? extends IOException> future = SERVICE.submit(() -> {
            try (BufferedOutputStream os = new BufferedOutputStream(socket.getOutputStream());
                    BufferedReader is = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String[] headers = collectLines(is).split("\n");
                String path = headers[0].split("\\s+")[1];
                System.out.println(path);
                routes.getOrDefault(path, Main::error404).accept(socket, os);
                os.flush();
                return null;
            } catch (IOException e) {
                return e;
            }
        });
        IOException e = future.get();
        if (e != null)
            throw e;
        socket.close();
    }

    private static String collectLines(BufferedReader reader) {
        StringBuilder builder = new StringBuilder();
        while (true) {
            try {
                String str = reader.readLine();
                if (str == null || str.isEmpty() || str.isBlank())
                    break;
                builder.append(str + "\n");
            } catch (IOException e) {
                break;
            }
        }
        return builder.toString();
    }

}