package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.ISerializable;
import org.bukkit.inventory.ItemStack;

public class EnchantmentBookLootItem implements ILootItem {

    public EnchantmentBookLootItem(InfPlugin plugin, ItemStack enchantBook) throws IllegalArgumentException{

    }

    @Serializable
    public String name = "";

    @Override
    public ItemStack getItemStack() {
        //todo
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toNbt() {
        //todo
        return null;
    }

    @Override
    public int getWeight(int level) {
        //todo
        return LootManager.getWeightForLevel(this, level);
    }

    @Override
    public boolean isDynamic() {
        //todo
        return false;
    }
}
