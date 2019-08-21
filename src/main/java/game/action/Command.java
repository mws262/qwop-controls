package game.action;

public class Command<T> {

    private final T commandData;

    public Command(T commandData) {
        this.commandData = commandData;
    }

    public T get() {
        return commandData;
    }

}
