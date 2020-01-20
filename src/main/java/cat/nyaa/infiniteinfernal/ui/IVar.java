package cat.nyaa.infiniteinfernal.ui;

public interface IVar<T> {
    T defaultRegeneration(int tick);
    T getMaxValue();
    T getValue();
    T getBaseMax();
    void setValue(T val);
    void setMaxValue(T value);
    void setBaseMax(T value);
    void setLastChange(int lastChange);
    int getLastChange();
    String getName();
}
