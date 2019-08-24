package game.action;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class Command<T> implements Comparable<Command<T>> {

    private final T commandData;

    public Command(T commandData) {
        this.commandData = commandData;
    }

    public T get() {
        return commandData;
    }

    @Override
    public int compareTo(@NotNull Command o) {
        Objects.requireNonNull(o);
        if (this.get().hashCode() == o.get().hashCode()) {
            return 0;
        } else {
            return this.get().hashCode() > o.get().hashCode() ? 1 : -1;
        }
    }

    public Command<T> getThis() {
        return this;
    }
}
