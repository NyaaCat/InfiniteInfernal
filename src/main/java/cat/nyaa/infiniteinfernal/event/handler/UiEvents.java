package cat.nyaa.infiniteinfernal.event.handler;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ui.BaseUi;
import cat.nyaa.infiniteinfernal.ui.PlayerStatus;
import cat.nyaa.infiniteinfernal.ui.UiManager;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class UiEvents implements Listener {
    @EventHandler
    public void onDamaged(EntityDamageEvent event){
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)){
            return;
        }

        Player player = (Player) entity;
        if (UiManager.getInstance().getPlayerStatus(player).equals(PlayerStatus.DAMAGED))return;
        UiManager.getInstance().setPlayerStatus(player, PlayerStatus.DAMAGED, 10);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event){
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getDamager();
        if (!(entity instanceof Player)){
            return;
        }
        Player player = (Player) entity;
        if (UiManager.getInstance().getPlayerStatus(player).equals(PlayerStatus.HIT_TARGET))return;
        UiManager.getInstance().setPlayerStatus(player, PlayerStatus.HIT_TARGET, 10);
    }

    @EventHandler
    public void onPlayerPotion(EntityPotionEffectEvent event){
        World world = event.getEntity().getWorld();
        if (!enabledInWorld(world))return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)){
            return;
        }
        Player player = (Player) entity;

        PotionEffect newEffect = event.getNewEffect();
        if (UiManager.getInstance().getPlayerStatus(player).equals(PlayerStatus.BUFFED))return;
        if (newEffect == null || !BaseUi.getBuffList().contains(newEffect.getType()))return;
        new BukkitRunnable() {
            @Override
            public void run() {
                UiManager.getInstance().setPlayerStatus(player, PlayerStatus.BUFFED, newEffect.getDuration());
            }
        }.runTaskLater(InfPlugin.plugin, 1);
    }

    private boolean enabledInWorld(World world) {
        return InfPlugin.plugin.config().isEnabledInWorld(world);
    }
}
