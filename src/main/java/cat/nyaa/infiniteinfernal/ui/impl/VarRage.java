package cat.nyaa.infiniteinfernal.ui.impl;


import cat.nyaa.infiniteinfernal.ui.BaseUi;
import cat.nyaa.infiniteinfernal.ui.DoubleVar;

public class VarRage extends DoubleVar {
    private final BaseUi baseUi;

    public VarRage(double value, double max, BaseUi baseUi) {
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
        double x = (tick - Math.max(lastRaise, lastDrop))/20d;
//        return -(Math.min(1.67 * x * x - 15 * x + 33.33,40));
        return - Math.max(0, 8 * x - 40);
    }

    @Override
    public String getName() {
        return "rage";
    }
}
