package value.containers;

public class ValueContainer_Average implements IValueContainer {

    private float totalValue;
    private int visitCount;

    @Override
    public float getValue() {
        return totalValue / (float) visitCount;
    }

    @Override
    public void update(float updateValue) {
        visitCount++;
        totalValue += updateValue;
    }

    @Override
    public int getUpdateCount() {
        return visitCount;
    }
}
