package cat.nyaa.infiniteinfernal.abilitiy.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.abilitiy.ActiveAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;

public class AbilityBleeding extends ActiveAbility {
    @Serializable
    public int duration = 60;
    @Serializable
    public double damage = 1;

    @Override
    public void active(IMob iMob) {
        int times = Math.min(1, duration / 20);
        Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(25, 25, 25))
                .forEach(entity -> {
                    AtomicInteger runTimes = new AtomicInteger(times);
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            try {
                                if (iMob.getEntity().isDead() || runTimes.getAndAdd(1) >= times) {
                                    this.cancel();
                                }
                                entity.damage(damage, iMob.getEntity());
                            }catch (Exception ex){
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(InfPlugin.plugin, 0, 20);
                });
    }

    @Override
    public String getName() {
        return "Bleeding";
    }
}
