package cat.nyaa.infiniteinfernal.mob.controller;

import cat.nyaa.infiniteinfernal.utils.ICorrector;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class Aggro {
    private double amplifier;
    private LivingEntity livingEntity;

    public Aggro(LivingEntity livingEntity) {
        this.livingEntity = livingEntity;
    }

    public double parseAttributeAggro(List<ICorrector> incs, List<ICorrector> decs, LivingEntity livingEntity) {
        double result = 0;
        if (!incs.isEmpty()) {
            result = incs.stream().mapToDouble(iCorrector -> iCorrector.getCorrection(livingEntity, null)).sum();
            result -= decs.stream().mapToDouble(iCorrector -> iCorrector.getCorrection(livingEntity, null)).sum();
        }
        amplifier = result;
        return result;
    }
}
