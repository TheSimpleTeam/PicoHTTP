package net.thesimpleteam.picohttp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PicoHTTP implements AutoCloseable {

    private class Key<T, Z> {
        private final T key1;
        private final Z key2;

        public Key(T key1, Z key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Key key)) return false;
            return Objects.equals(key1, key.key1) && Objects.equals(key2, key.key2);
        }

        @Override
        public int hashCode() {
            int result = 17;
            if (key1 != null) {
                result = 31 * result + key1.hashCode();
            }
            if (key2 != null) {
                result = 31 * result + key2.hashCode();
            }
            return result;
        }
    }

    public static final String VERSION = "@VERSION@"; 
    private final ExecutorService service = Executors.newCachedThreadPool();
    private final Map<Key<String, String>, ThrowingConsumer<Client>> routes = new HashMap<>();
    private ThrowingConsumer<Client> error404 = this::error404;
    private final ServerSocket server;
    private final Logger logger = Logger.getLogger("PicoHTTP");

    public PicoHTTP(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    @Override
    public void close() throws IOException {
        this.service.shutdownNow();
        try {
            this.service.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        this.server.close();
    }

    public void setDefaultError404(ThrowingConsumer<Client> consumer) {
        this.error404 = Objects.requireNonNull(consumer);
    }

    public void addRoute(String path, ThrowingConsumer<Client> consumer) {
        addRoute(path, HTTPMethods.GET, consumer);
    }

    public void addRoute(String path, HTTPMethods method, ThrowingConsumer<Client> consumer) {
        if (path == null || path.isEmpty()) path = "/";
        routes.put(new Key<String, String>(path, method.name()), consumer);
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
                    HTTPMethods method = path.method();
                    strPath = strPath.charAt(0) != '/' ? "/" + strPath : strPath;
                    addRoute(strPath, method, (client) -> {
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
            if(split.length != 2) break;
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
                    var is = socket.getInputStream()) {
                String lines = collectLines(is);
                String[] headers = lines.split("\n");
                String[] pathAndMethod = headers[0].split(" ");
                String method = pathAndMethod[0];
                String path = pathAndMethod[1];
                String data = method.equalsIgnoreCase("GET") ? null : getData(headers).strip();
                //routes.getOrDefault(new Key<String, String>(path, method.toUpperCase()), this.error404).accept(new Client(socket, os, method, parseHeaders(headers), data, path));
                routes.entrySet().stream()
                    .filter(e -> e.getKey().key2.equalsIgnoreCase(method) && path.matches(e.getKey().key1))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(this::error404)
                    .accept(new Client(socket, os, method, parseHeaders(headers), data, path));
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

    private String getData(String[] headers) {
        StringBuilder builder = new StringBuilder();
        boolean foundAnEmptyLine = false;
        for(int i = 0; i < headers.length; i++) {
            if(!foundAnEmptyLine && headers[i].isEmpty()) foundAnEmptyLine = true;
            else if(!foundAnEmptyLine) continue;
            builder.append(headers[i]).append("\n");
        }
        return builder.toString();
    }

    private String collectLines(InputStream reader) throws IOException {
        //TODO: refactor this code
        List<String> lines = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        int contentLength = 0, i = 0;
        boolean isReadingHeaders = true;
        HTTPMethods method = HTTPMethods.GET;
        while(true) {
            char b = (char) reader.read();
            builder.append(b);
            if(isReadingHeaders && b == '\n') {
                String line = builder.toString().stripTrailing();
                lines.add(line);
                Optional<HTTPMethods> httpMethod = Arrays.stream(HTTPMethods.values())
                    .filter(m -> line.split(" ")[0].equalsIgnoreCase(m.name())).findFirst();
                if(httpMethod.isPresent()) method = httpMethod.get();
                if(line.startsWith("Content-Length:")) contentLength = parseInt(line.split(":")[1].strip(), 0);
                else if(!line.matches("[a-zA-Z\\-]+:\\s?.+") && httpMethod.isEmpty()) {
                    isReadingHeaders = false;
                }
                if(builder.toString().isBlank() && method == HTTPMethods.GET) break;
                builder.setLength(0);
            } else if(!isReadingHeaders && method != HTTPMethods.GET) {
                if(++i >= contentLength) break;
            }
        }
        return lines.stream().collect(Collectors.joining("\n")) + "\n" + builder.toString();
    }

    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch(NumberFormatException ignored) {
            logger.log(Level.WARNING, "Couldn't parse " + str, ignored);
            return defaultValue;
        }
    }

    private void error404(Client client) throws IOException {
        String text = "<!DOCTYPE html><html><head><title>PicoHTTP - Error 404</title></head><body><h1>Error 404</h1></body></html>";
        client.send(404, "Not found", ContentTypes.HTML, text);
    }
}