package cat.nyaa.infiniteinfernal.configs;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Objects;

public class RegionConfig extends IdFileConfig {

    public RegionConfig(int id){
        super(id);
    }

    public RegionConfig(int id, Region region){
        super(id);
        this.region = region;
    }

    @Serializable
    public Region region;

    @Serializable
    public List<String> mobs;

    @Override
    public String getPrefix() {
        return "region";
    }

    public static class Region implements ISerializable {

        public Region(Location location1, Location location2){
            this.world = location1.getWorld() == null ? "" :location1.getWorld().getName();
            int x1 = location1.getBlockX();
            int x2 = location2.getBlockX();

            int y1 = location1.getBlockY();
            int y2 = location2.getBlockY();

            int z1 = location1.getBlockZ();
            int z2 = location2.getBlockZ();
            this.location1 = location1;
            this.location2 = location2;
            setRegion(x1,x2,y1,y2,z1,z2);
        }

        public Region(World world, int xMin, int xMax, int yMin, int yMax, int zMin, int zMax){
            this.world = world == null ? "" : world.getName();
            int x1 = xMax;
            int x2 = xMin;

            int y1 = yMax;
            int y2 = yMin;

            int z1 = zMax;
            int z2 = zMin;
            location1 = new Location(world, x1,y1,z1);
            location2 = new Location(world, x2,y2,z2);
            setRegion(x1,x2,y1,y2,z1,z2);
        }

        private void setRegion(int x1, int x2, int y1, int y2, int z1, int z2){
            xMax = Math.max(x1,x2);
            xMin = Math.min(x1,x2);

            yMax = Math.max(y1,y2);
            yMin = Math.min(y1,y2);

            zMax = Math.max(z1,z2);
            zMin = Math.min(z1,z2);
        }

        @Serializable
        public String world;
        @Serializable
        public int xMax;
        @Serializable
        public int xMin;
        @Serializable
        public int yMax;
        @Serializable
        public int yMin;
        @Serializable
        public int zMax;
        @Serializable
        public int zMin;

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
            try{
                return BoundingBox.of(location1, location2).contains(location.toVector());
            }catch (IllegalArgumentException e){
                //todo: log
                return false;
            }
        }
    }
}
