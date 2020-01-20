package cat.nyaa.infiniteinfernal.ui.impl;

import cat.nyaa.infiniteinfernal.ui.BaseVar;

public class VarMana extends BaseVar<Double> {
    public VarMana(double value, double max) {
        super(value, max);
    }

    @Override
    public Double defaultRegeneration(int tick) {
        int x = tick - lastChange;
        return 1.67 * x * x - 15 * x + 33.33;
    }

    @Override
    public String getName() {
        return "mana";
    }
}
