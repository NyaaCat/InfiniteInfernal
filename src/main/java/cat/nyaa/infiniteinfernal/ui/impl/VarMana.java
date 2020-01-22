package cat.nyaa.infiniteinfernal.ui.impl;

import cat.nyaa.infiniteinfernal.ui.DoubleVar;

public class VarMana extends DoubleVar {
    public VarMana(double value, double max) {
        super(value, max);
    }

    @Override
    public Double defaultRegeneration(int tick) {
        double x = (tick - lastChange) / 20d;
        return Math.min(20, Math.max(0, 4 * x - 15));
    }

    @Override
    public String getName() {
        return "mana";
    }
}
