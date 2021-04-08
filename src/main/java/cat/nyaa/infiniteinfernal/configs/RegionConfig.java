package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class RegionConfig extends NamedFileConfig {

    public RegionConfig(String id) {
        super(id);
    }

    public RegionConfig(String id, Region region) {
        super(id);
        this.region = region;
    }

    @Serializable
    public String name = "region";

    @Serializable
    public Region region = new Region();

    @Serializable
    public List<String> mobs = new ArrayList<>();

    @Serializable
    public String level = "";

    @Serializable
    public int maxSpawnAmountOverride = -1;

    @Override
    public String getPrefix() {
        return "";
    }

    @Override
    protected String getFileName() {
        return super.getFileName();
    }

    @Override
    protected String getFileDirName() {
        return "regions";
    }

    public static class Region implements ISerializable {
        public Region() {
        }

        public Region(Location location1, Location location2) {
            this.world = location1.getWorld() == null ? "" : location1.getWorld().getName();
            int x1 = location1.getBlockX();
            int x2 = location2.getBlockX();

            int y1 = location1.getBlockY();
            int y2 = location2.getBlockY();

            int z1 = location1.getBlockZ();
            int z2 = location2.getBlockZ();
            setRegion(location1.getWorld(), x1, x2, y1, y2, z1, z2);
        }

        public Region(World world, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax) {
            this.world = world == null ? "" : world.getName();
            int x1 = xMax;
            int x2 = xMin;

            int y1 = yMax;
            int y2 = yMin;

            int z1 = zMax;
            int z2 = zMin;

            setRegion(world, x1, x2, y1, y2, z1, z2);
        }

        private void setRegion(World world, int x1, int x2, int y1, int y2, int z1, int z2) {
            xMax = Math.max(x1, x2);
            xMin = Math.min(x1, x2);

            yMax = Math.max(y1, y2);
            yMin = Math.min(y1, y2);

            zMax = Math.max(z1, z2);
            zMin = Math.min(z1, z2);
            location1 = new Location(world, x1, y1, z1);
            location2 = new Location(world, x2, y2, z2);
        }

        @Serializable
        public String world = "world";
        @Serializable
        public int xMax = 0;
        @Serializable
        public int xMin = 0;
        @Serializable
        public int yMax = 0;
        @Serializable
        public int yMin = 0;
        @Serializable
        public int zMax = 0;
        @Serializable
        public int zMin = 0;

        private Location location1;
        private Location location2;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Region region = (Region) o;
            return xMax == region.xMax &&
                    xMin == region.xMin &&
                    yMax == region.yMax &&
                    yMin == region.yMin &&
                    zMax == region.zMax &&
                    zMin == region.zMin &&
                    Objects.equals(world, region.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, xMax, xMin, yMax, yMin, zMax, zMin);
        }

        public boolean contains(Location location) {
            if (location1 == null || location2 == null) {
                World world = Bukkit.getWorld(this.world);
                if (world == null) {
                    return false;
                }
                setRegion(world, xMin, xMax, yMin, yMax, zMin, zMax);
            }
            try {
                return BoundingBox.of(location1, location2).contains(location.toVector());
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().log(Level.SEVERE, "invalid location", e);
                return false;
            }
        }
    }
}
