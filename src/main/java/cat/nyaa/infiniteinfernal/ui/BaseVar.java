package cat.nyaa.infiniteinfernal.ui;

public abstract class BaseVar<T> implements IVar<T>{
    protected T value;
    protected T max;
    protected T baseMax;
    protected int lastChange = 0;

    public BaseVar(T value, T max) {
        this.value = value;
        this.max = max;
        this.baseMax = max;
    }

    protected BaseVar(){}

    @Override
    public T getMaxValue() {
        return max;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public T getBaseMax() {
        return baseMax;
    }

    @Override
    public void setValue(T val) {
        value = val;
    }

    @Override
    public void setMaxValue(T value) {
        max = value;
    }

    @Override
    public void setBaseMax(T value) {
        baseMax = value;
    }

    @Override
    public void setLastChange(int lastChange) {
        this.lastChange = lastChange;
    }

    @Override
    public int getLastChange() {
        return lastChange;
    }
}
