package cat.nyaa.infiniteinfernal.event;

import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.mob.IMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

public class LootDropEvent extends Event {
    private final EntityDamageEvent damageEvent;
    private final IMob iMob;
    private ILootItem loot;
    private ILootItem specialLoot;
    private EntityDeathEvent ev;
    public static final HandlerList handlerList = new HandlerList();

    public ILootItem getLoot() {
        return loot;
    }

    public ILootItem getSpecialLoot() {
        return specialLoot;
    }

    public void setLoot(ILootItem loot) {
        this.loot = loot;
    }

    public void setSpecialLoot(ILootItem specialLoot) {
        this.specialLoot = specialLoot;
    }

    public LivingEntity getKiller() {
        Player killer = ev.getEntity().getKiller();
        if (killer == null){
            if (damageEvent instanceof EntityDamageByEntityEvent) {
                Entity damager = ((EntityDamageByEntityEvent) damageEvent).getDamager();
                if (damager instanceof Projectile){
                    ProjectileSource shooter = ((Projectile) damager).getShooter();
                    if (shooter instanceof LivingEntity){
                        return ((LivingEntity) shooter);
                    }
                }
                if (damager instanceof LivingEntity){
                    return (LivingEntity) damager;
                }
            }
        }
        return killer;
    }

    public IMob getiMob() {
        return iMob;
    }

    public LootDropEvent(EntityDamageEvent event, IMob iMob, ILootItem loot, ILootItem specialLoot, EntityDeathEvent ev) {
        this.damageEvent = event;
        this.iMob = iMob;
        this.loot = loot;
        this.specialLoot = specialLoot;
        this.ev = ev;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public EntityDeathEvent getEntityDeathEvent() {
        return ev;
    }
}
