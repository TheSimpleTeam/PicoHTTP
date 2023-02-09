import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.foreign.ValueLayout.*;

public class Main {
    private static String OS = System.getProperty("os.name").toLowerCase();
    private static boolean isRunningWindows = OS.contains("WIN");
    private static SymbolLookup stdlib;

    public static void main(String[] args) {
        /*
         * Easy way:
         * System.out.println("Hello World");
         */
        // Chad way:

        Linker linker = Linker.nativeLinker();
        stdlib = linker.defaultLookup();
        try {
            printf("%s %s\n", "Hello", "World!");
            printf("Is tty %d\n", isATTY());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static int isATTY() throws Throwable {
        var isatty = Linker.nativeLinker().downcallHandle(
                stdlib.find((isRunningWindows ? "_" : "") + "isatty").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, JAVA_INT));
        var fileno = Linker.nativeLinker().downcallHandle(
                stdlib.find((isRunningWindows ? "_" : "") + "fileno").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
        var stdin = stdlib.find("stdin").orElseThrow();
        return (int) isatty.invokeExact((int) fileno.invokeExact(stdin));
    }

    private static int printf(String fmt, Object... text) throws Throwable {
        MethodType type = MethodType.methodType(int.class, MemorySegment.class);
        FunctionDescriptor descriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS);
        List<MemoryLayout> segments = new ArrayList<>();
        try(Arena arena = Arena.openConfined()) {
            for(Object o : text) {
                Class<?> clazz = switch(o) {
                    case Integer i -> int.class;
                    case Long l -> long.class;
                    default -> MemorySegment.class;
                };
                type = type.appendParameterTypes(clazz);
                //segments.add(allocate(arena, o));
                segments.add(o instanceof Integer ? JAVA_INT : (o instanceof Long ? JAVA_LONG : ADDRESS));
            }
            Linker.Option varargIndex = Linker.Option.firstVariadicArg(descriptor.argumentLayouts().size());
            return (int) Linker.nativeLinker().downcallHandle(
                    stdlib.find("printf").orElseThrow(),
                    descriptor.appendArgumentLayouts(segments.toArray(MemoryLayout[]::new)),
                    varargIndex)
                .asSpreader(1, Object[].class, text.length)
                .invoke(allocate(arena, fmt),
                        Arrays.stream(text).map(o -> allocate(arena, o)).toArray());
        }
    }

    private static MemorySegment allocate(Arena arena, Object o) {
        return switch(o) {
            case Integer i -> arena.allocate(JAVA_INT, i.intValue());
            case Long l -> arena.allocate(JAVA_LONG, l.longValue());
            case Number n -> arena.allocate(JAVA_INT, n.intValue());
            case String str -> arena.allocateUtf8String(str);
            default -> null;
        };
    }
}
