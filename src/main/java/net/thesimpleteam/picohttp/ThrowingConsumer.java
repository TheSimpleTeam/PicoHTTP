package net.thesimpleteam.picohttp;

import java.util.function.Consumer;

/**
* @author <a href="https://gist.github.com/myui/9722c1301434a3b69cf898ccd9090ff1">myui</a>
*/

@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {
	default void accept(T t) {
		try {
			accept0(t);
		} catch(Throwable ex) {
			Throwing.sneakyThrow(ex);
		}
	}

	void accept0(T t) throws Throwable;
}
