package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

public class Events implements Listener {
    private InfPlugin plugin;

    public Events(InfPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDeath(EntityDamageEvent ev){
        Entity entity = ev.getEntity();
        if (MobManager.instance().isIMob(entity)){
            IMob iMob = MobManager.instance().toIMob(entity);
            if (iMob == null)return;
            List<IAbility> abilities = iMob.getAbilities();
        }
    }
}
