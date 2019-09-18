package game.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import game.cartpole.CommandCartPole;
import game.qwop.CommandQWOP;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommandQWOP.class, name = "qwop"),
        @JsonSubTypes.Type(value = CommandCartPole.class, name = "cartpole"),
})
public class Command<T> implements Comparable<Command<T>> {

    private final T commandData;

    public Command(@JsonProperty("commandData") T commandData) {
        this.commandData = commandData;
    }

    @JsonProperty("commandData")
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

    @JsonIgnore
    public Command<T> getThis() {
        return this;
    }

}
