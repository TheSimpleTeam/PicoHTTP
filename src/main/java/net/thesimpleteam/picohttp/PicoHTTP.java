package net.thesimpleteam.picohttp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PicoHTTP implements AutoCloseable {

    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Map<String, ThrowingConsumer<Client>> routes = new HashMap<>();
    private ThrowingConsumer<Client> error404 = this::error404;
    private final ServerSocket server;

    public PicoHTTP(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    @Override
    public void close() throws IOException {
        this.service.shutdownNow();
        try {
            this.service.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        this.server.close();
    }

    public void setDefaultError404(ThrowingConsumer<Client> consumer) {
        this.error404 = Objects.requireNonNull(consumer);
    }

    public void addRoute(String path, ThrowingConsumer<Client> consumer) {
        if (path == null || path.isEmpty())
            path = "/";
        routes.put(path, consumer);
    }

    public <T> void addRoutes(Class<T> clazz, T instance) {
        Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == Client.class)
                .filter(m -> m.getAnnotation(Path.class) != null)
                .filter(m -> m.trySetAccessible())
                .filter(m -> (instance == null && Modifier.isStatic(m.getModifiers())) || instance != null)
                .forEach(m -> {
                    Path path = m.getAnnotation(Path.class);
                    String strPath = path.value();
                    strPath = strPath.charAt(0) != '/' ? "/" + strPath : strPath;
                    addRoute(strPath, (client) -> {
                        try {
                            m.invoke(instance, client);
                        } catch (InvocationTargetException | IllegalAccessException ex) {
                            ex.printStackTrace();
                        }
                    });
                });
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

    private Map<String, String> parseHeaders(String[] headers) {
        Map<String, String> map = new HashMap<>();
        for (int i = 1; i < headers.length; i++) {
            String[] split = headers[i].split(": ");
            map.put(split[0], split[1]);
        }
        return map;
    }

    public void listenForRequests(ServerSocket server)
            throws IOException, ExecutionException, InterruptedException {
        if (server.isClosed())
            return;
        Socket socket = server.accept();
        Future<? extends IOException> future = service.submit(() -> {
            try (BufferedOutputStream os = new BufferedOutputStream(socket.getOutputStream());
                    BufferedReader is = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String[] headers = collectLines(is).split("\n");
                String path = headers[0].split("\\s+")[1];
                String method = headers[0].split(" ")[0];
                routes.getOrDefault(path, this.error404).accept(new Client(socket, os, method, parseHeaders(headers)));
                os.flush();
                return null;
            } catch (IOException e) {
                return e;
            }
        });
        IOException e = future.get();
        if (e != null) throw e;
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

    private void error404(Client client) throws IOException {
        String text = "<!DOCTYPE html><html><head><title>PicoHTTP - Error 404</title></head><body><h1>Error 404</h1></body></html>";
        client.send(404, "Not found", ContentTypes.HTML, text);
    }
}