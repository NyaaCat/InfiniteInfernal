package cat.nyaa.infiniteinfernal.ui;

public abstract class BaseVar<T> implements IVar<T>{
    protected T value;
    protected T max;
    protected T baseMax;
    protected int lastDrop = 0;
    protected int lastRaise = 0;
    protected T damageIndicate;

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
    public void setLastDrop(int lastChange) {
        this.lastDrop = lastChange;
    }

    @Override
    public int getLastRaise() {
        return lastRaise;
    }

    @Override
    public void setLastRaise(int lastRaise) {
        this.lastRaise = lastRaise;
    }

    @Override
    public int getLastDrop() {
        return lastDrop;
    }
}
