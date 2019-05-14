package value.containers;

public class ValueContainer_Null implements IValueContainer {
    @Override
    public float getValue() { return 0; }

    @Override
    public void update(float updateValue) {}

    @Override
    public int getUpdateCount() { return 0; }
}
