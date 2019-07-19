package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Particle;

import java.util.Arrays;
import java.util.List;

public class ParticleConfig implements ISerializable {
    @Serializable
    public Particle type = Particle.FIREWORKS_SPARK;
    @Serializable
    public List<Double> delta = Arrays.asList(0d, 0d, 0d);
    @Serializable
    public double speed = 0;
    @Serializable
    public int amount = 500;
    @Serializable
    public String extraData = "";
    @Serializable
    public boolean forced = false;

    public double getOffsetX() {
        return delta.get(0);
    }

    public double getOffsetY() {
        return delta.get(1);
    }

    public double getOffsetZ() {
        return delta.get(2);
    }
}
