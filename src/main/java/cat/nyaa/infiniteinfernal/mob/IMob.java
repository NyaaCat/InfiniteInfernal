package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public interface IMob {
    List<ILootItem> getLoots();
    List<ILootItem> getSpecialLoots();
    List<IAbility> getAbilities();
    LivingEntity getEntity();
    EntityType getEntityType();
    KeyedBossBar getBossBar();
    int getLevel();
    double getDamage();
    void makeInfernal(LivingEntity entity);
    boolean isAutoSpawn();
    boolean dropVanilla();
    String getName();
    String getTaggedName();
}
