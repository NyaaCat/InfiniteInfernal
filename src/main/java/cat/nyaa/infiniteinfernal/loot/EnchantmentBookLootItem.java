package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.InfPlugin;
import org.bukkit.inventory.ItemStack;

public class EnchantmentBookLootItem implements ILootItem {

    public EnchantmentBookLootItem(InfPlugin plugin, String name, ItemStack enchantBook) throws IllegalArgumentException{

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
    public int getWeight(String level) {
        //todo
        return LootManager.getWeightForLevel(this, level);
    }

    @Override
    public boolean isDynamic() {
        //todo
        return false;
    }

    @Override
    public void setDynamic(boolean dynamic) {
        //todo
    }
}
