package net.thesimpleteam.picohttp;

public final class Throwing {
    private Throwing() {}

    //This is some kind of black magic
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable ex) throws E {
        throw (E) ex;
    }
}
