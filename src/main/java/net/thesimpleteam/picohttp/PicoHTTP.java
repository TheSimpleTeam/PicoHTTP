package net.thesimpleteam.picohttp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class PicoHTTP implements AutoCloseable {

    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Map<String, BiConsumer<Socket, BufferedOutputStream>> routes = new HashMap<>();
    private BiConsumer<Socket, BufferedOutputStream> error404 = this::error404;
    private final ServerSocket server;

    public PicoHTTP(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    @Override
    public void close() throws IOException {
        this.service.shutdownNow();
        try {
            this.service.awaitTermination(2, TimeUnit.SECONDS);
        } catch(InterruptedException ignored) {}
        this.server.close();
    }

    public void setDefaultError404(BiConsumer<Socket, BufferedOutputStream> consumer) {
        this.error404 = Objects.requireNonNull(consumer);
    }

    public void addRoute(String path, BiConsumer<Socket, BufferedOutputStream> consumer) {
        if(path == null || path.isEmpty()) path = "/";
        routes.put(path, consumer);
    }

    public void run() {
        service.execute(() -> {
            try {
                while (!this.server.isClosed()) {
                    listenForRequests(server);
                }
            } catch (IOException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void listenForRequests(ServerSocket server)
            throws IOException, ExecutionException, InterruptedException {
        if(server.isClosed()) return;
        Socket socket = server.accept();
        Future<? extends IOException> future = service.submit(() -> {
            try (BufferedOutputStream os = new BufferedOutputStream(socket.getOutputStream());
                    BufferedReader is = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String[] headers = collectLines(is).split("\n");
                String path = headers[0].split("\\s+")[1];
                routes.getOrDefault(path, this.error404).accept(socket, os);
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

    private String collectLines(BufferedReader reader) {
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

    public static String writeHTML(int code, String codeMessage, String html) {
        if(codeMessage == null) codeMessage = "I'm lazy.";
        return String.format(
            "HTTP/1.0 %d %s\nServer: PicoHTTP/0.1\nContent-type: text/html\nContent-Length: %d\n\n%s",
            code, codeMessage, html.length(), html
        );
    }

    private void error404(Socket client, BufferedOutputStream os) {
        try {
            String text = "<!DOCTYPE html><html><head><title>PicoHTTP - Error 404</title></head><body><h1>Error 404</h1></body></html>";
            os.write(writeHTML(404, "Not found", text).getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}