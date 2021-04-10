package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.inventory.ItemStack;

public interface ILootItem extends ISerializable {
    ItemStack getItemStack();
    String getName();
    String toNbt();
    int getWeight(String level);
    boolean isDynamic();
    void setDynamic(boolean dynamic);
}
