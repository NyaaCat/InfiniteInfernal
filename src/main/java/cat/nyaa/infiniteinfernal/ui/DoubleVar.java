package cat.nyaa.infiniteinfernal.ui;

public abstract class DoubleVar extends BaseVar<Double> {

    public DoubleVar(double value, double max) {
        super(value, max);
        damageIndicate = 0d;
    }

    public void drop(double drop, int tick){
        Double orig = this.value;
        setValue(Math.max(0, Math.min(this.value - drop, max)));
        damageIndicate = Math.min(damageIndicate + orig - getValue(), max);
        if (drop > 0) {
            setLastDrop(tick);
        }else {
            setLastRaise(tick);
        }
    }

    @Override
    public Double getDamageIndicate() {
        return damageIndicate;
    }

    public void refreshIndicate(int tick) {
        int x = tick - lastDrop;
        double indicateDrop = getIndicateDrop(x);
        damageIndicate = Math.max(damageIndicate - indicateDrop, 0);
    }

    private double getIndicateDrop(int tick) {
        if (damageIndicate <= 0)return 0;
        int x = tick;
        return Math.max(0, Math.max(0, Math.min(5, (0.004761904761904764 * x * x * x - 0.21428571428571414 * x * x + 3.195238095238093 * x - 15.285714285714278))));
    }

    public void regenerate(double manaReg) {
        setValue(Math.max(0,Math.min(max, getValue() + manaReg)));
    }
}
