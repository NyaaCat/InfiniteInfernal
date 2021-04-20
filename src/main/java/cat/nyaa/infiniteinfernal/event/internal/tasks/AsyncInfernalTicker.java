package cat.nyaa.infiniteinfernal.event.internal.tasks;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.mob.ability.AbilityActive;
import cat.nyaa.infiniteinfernal.mob.ability.IAbilitySet;
import cat.nyaa.infiniteinfernal.utils.CorrectionParser;
import cat.nyaa.infiniteinfernal.utils.ICorrector;
import cat.nyaa.infiniteinfernal.utils.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class AsyncInfernalTicker {
    private final int interval;
    Queue<IMob> mobEffectQueue;
    private int nextTickTasks = 0;

    AsyncInfernalTicker(int interval) {
        this.interval = interval;
        mobEffectQueue = new LinkedList<>();
        String dementia = InfPlugin.plugin.config().addEffects.get("dementia");
        if (dementia != null) {
            iCorrector = CorrectionParser.parseStr(dementia);
        }
    }

    void tick() {
        if (mobEffectQueue.isEmpty()) return;
        for (int i = 0; i < nextTickTasks; i++) {
            if (mobEffectQueue.isEmpty()) return;
            IMob iMob = mobEffectQueue.poll();
            new BukkitRunnable() {
                @Override
                public void run() {
                    mobEffect(iMob);
                }
            }.runTask(InfPlugin.plugin);
        }
    }
    public void submitInfernalTickMobs(List<IMob> mobs) {
        if (mobs == null || mobs.isEmpty()) return;
        mobs.forEach(mob -> mobEffectQueue.offer(mob));
        nextTickTasks = (int) Math.ceil((mobs.size()) / (double) interval);
    }

    private static ICorrector iCorrector = null;

    private static void mobEffect(IMob iMob) {
        MobManager mobManager = MobManager.instance();
        LivingEntity entity = iMob.getEntity();
        if (entity == null || entity.isDead()) {
            mobManager.removeMob(iMob, false);
        }
        iMob.showParticleEffect();
        iMob.autoRetarget();
        if (iMob.isDynamicHealth()) {
            iMob.tweakHealth();
        }
        List<Player> playersNearMob = mobManager.getPlayersNearMob(iMob);
        if (iCorrector != null) {
            EntityEquipment equipment = iMob.getEntity().getEquipment();
            ItemStack itemInMainHand = null;
            if (equipment != null) {
                itemInMainHand = equipment.getItemInMainHand();
            }
            double correction = iCorrector.getCorrection(iMob.getEntity(), itemInMainHand);
            if (Utils.possibility(correction / 100d)) {
                return;
            }
        }
        if (playersNearMob.size() == 0) {
            mobManager.removeMob(iMob, false);
        }
        LivingEntity target = iMob.getTarget();
        if (target == null || !target.getWorld().equals(iMob.getEntity().getWorld()))
            return;

        List<IAbilitySet> abilities = iMob.getAbilities().stream()
                .filter(IAbilitySet::containsActive)
                .collect(Collectors.toList());
        IAbilitySet iAbilitySet = Utils.weightedRandomPick(abilities);
        if (iAbilitySet == null) {
            return;
        }
        iAbilitySet.getAbilitiesInSet().stream()
                .filter(iAbility -> iAbility instanceof AbilityActive)
                .map(iAbility -> ((AbilityActive) iAbility))
                .forEach(abilityTick -> abilityTick.active(iMob));
    }
}