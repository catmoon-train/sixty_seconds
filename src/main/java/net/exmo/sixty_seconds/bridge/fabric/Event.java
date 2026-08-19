package net.exmo.sixty_seconds.bridge.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Simple Fabric-style listener list with an invoker. */
public final class Event<T> {
    private final List<T> listeners = new ArrayList<>();
    private final Function<List<T>, T> factory;
    private T invoker;

    public Event() {
        this(null);
    }

    public Event(Function<List<T>, T> factory) {
        this.factory = factory;
        rebuild();
    }

    public void register(T listener) {
        listeners.add(listener);
        rebuild();
    }

    public List<T> invokers() {
        return listeners;
    }

    public T invoker() {
        return invoker;
    }

    public void forEach(Consumer<T> consumer) {
        for (T listener : listeners) {
            consumer.accept(listener);
        }
    }

    @SuppressWarnings("unchecked")
    private void rebuild() {
        if (factory != null) {
            invoker = factory.apply(List.copyOf(listeners));
        } else {
            invoker = (T) (Object) this;
        }
    }
}
