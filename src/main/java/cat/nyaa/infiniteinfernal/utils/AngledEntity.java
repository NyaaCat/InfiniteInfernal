package cat.nyaa.infiniteinfernal.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class AngledEntity implements Comparable<AngledEntity> {
        private static int CLOSE_DISTANCE = (25 * 25) / 4;
        double angle;
        double distance;
        LivingEntity entity;

    public double getAngle() {
        return angle;
    }

    public double getDistance() {
        return distance;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public AngledEntity(double angle, double distance, LivingEntity currentMobEntity) {
            this.angle = angle;
            this.distance = distance;
            entity = currentMobEntity;
        }

        public static AngledEntity of(LivingEntity entity, LivingEntity player) {
            Vector direction = player.getEyeLocation().getDirection();
            Vector entityDirection = entity.getEyeLocation().toVector().subtract(player.getEyeLocation().toVector());
            double angle = direction.angle(entityDirection);
            double distance = player.getLocation().distance(entity.getLocation());
            return new AngledEntity(angle, distance, entity);
        }

        @Override
        public int hashCode() {
            return (int) (angle + entity.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AngledEntity && angle == ((AngledEntity) obj).angle && entity.equals(((AngledEntity) obj).entity);
        }

        @Override
        public int compareTo(AngledEntity o) {
            double c1, c2;
            c1 = angle;
            c2 = o.angle;
            double distanceShift = 1000000d;
            if (distance > CLOSE_DISTANCE) {
                c1 += distanceShift;
            }
            if (o.distance > CLOSE_DISTANCE) {
                c2 += distanceShift;
            }
            if (c1 - c2 > 0) return 1;
            if (c1 == c2) return 0;
            return -1;
        }
    }