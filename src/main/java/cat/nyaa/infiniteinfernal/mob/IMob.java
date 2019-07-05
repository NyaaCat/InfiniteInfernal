package cat.nyaa.infiniteinfernal.mob;

import cat.nyaa.infiniteinfernal.abilitiy.IAbility;
import cat.nyaa.infiniteinfernal.loot.ILootItem;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public interface IMob {
    List<ILootItem> getLoots();
    List<ILootItem> getSpecialLoots();
    List<IAbility> getAbilities();
    LivingEntity getEntity();
    int getLevel();
    int getDamage();
    boolean isAutoSpawn();
    String getName();
    String getTaggedName();
}
