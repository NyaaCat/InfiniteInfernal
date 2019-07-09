package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.abilitiy.AbilityTick;
import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;

public class MainLoopTask {
    int interval = 60;//todo config this

    static class MainLoopRunnable extends BukkitRunnable {

        @Override
        public void run() {
            Collection<IMob> mobs = MobManager.instance().getMobs();
            if (!mobs.isEmpty()) {
                mobs.forEach(iMob -> {
                    iMob.showParticleEffect();
                    List<IAbility> abilities = iMob.getAbilities();
                    abilities.stream()
                            .filter(iAbility -> iAbility instanceof AbilityTick)
                            .map(iAbility -> ((AbilityTick) iAbility))
                            .forEach(abilityTick -> abilityTick.tick(iMob));
                });
            }

        }
    }
}
