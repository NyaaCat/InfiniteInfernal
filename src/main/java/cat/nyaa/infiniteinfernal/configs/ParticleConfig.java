package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParticleConfig implements ISerializable {
    @Serializable
    public Particle type = Particle.FIREWORKS_SPARK;
    @Deprecated
    @Serializable(manualSerialization = true)
    public List<Number> delta = new ArrayList<>(Arrays.asList(0d, 0d, 0d));
    @Serializable
    public double deltaX = 0;
    @Serializable
    public double deltaY = 0;
    @Serializable
    public double deltaZ = 0;
    @Serializable
    public double speed = 0;
    @Serializable
    public int amount = 500;
    @Serializable
    public String extraData = null;
    @Serializable
    public boolean forced = false;

    @Override
    public void deserialize(ConfigurationSection config) {
        ISerializable.deserialize(config, this);
        try {
            List<Number> del = (List<Number>) config.get("delta");
            if (deltaX == 0 && deltaY==0 && deltaZ == 0 && del !=null){
                deltaX = del.get(0).doubleValue();
                deltaY = del.get(1).doubleValue();
                deltaZ = del.get(2).doubleValue();
            }
        }catch (Exception ignored){}
    }

    public double getOffsetX() {
        return deltaX;
    }

    public double getOffsetY() {
        return deltaY;
    }

    public double getOffsetZ() {
        return deltaZ;
    }
}
