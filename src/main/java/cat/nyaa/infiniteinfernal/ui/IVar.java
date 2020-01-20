package cat.nyaa.infiniteinfernal.ui;

public interface IVar {

    double defaultRegeneration(int tick);
    double evalRegeneration(int tick, String expr);
    double getMaxValue();
    double getValue();
    String getName();

}
