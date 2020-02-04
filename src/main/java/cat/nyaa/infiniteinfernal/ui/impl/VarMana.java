package cat.nyaa.infiniteinfernal.ui.impl;

import cat.nyaa.infiniteinfernal.ui.BaseUi;
import cat.nyaa.infiniteinfernal.ui.DoubleVar;

public class VarMana extends DoubleVar {
    private BaseUi baseUi;

    public VarMana(double value, double max, BaseUi baseUi) {
        super(value, max);
        this.baseUi = baseUi;
    }

    @Override
    public void drop(double drop, int tick) {
        super.drop(drop, tick);
        baseUi.refreshIfAuto();
    }

    @Override
    public Double defaultRegeneration(int tick) {
        double x = (tick - lastDrop) / 20d;
        return Math.min(50, Math.max(0, 4 * x - 15));
    }

    @Override
    public String getName() {
        return "mana";
    }
}
