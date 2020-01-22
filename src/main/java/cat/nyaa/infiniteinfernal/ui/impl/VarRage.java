package cat.nyaa.infiniteinfernal.ui.impl;


import cat.nyaa.infiniteinfernal.ui.DoubleVar;

public class VarRage extends DoubleVar {
    public VarRage(double value, double max) {
        super(value, max);
    }

    @Override
    public Double defaultRegeneration(int tick) {
        double x = (tick - lastChange)/20d;
//        return -(Math.min(1.67 * x * x - 15 * x + 33.33,40));
        return - Math.min(20, Math.max(0, 4 * x - 20));
    }

    @Override
    public String getName() {
        return "rage";
    }
}
