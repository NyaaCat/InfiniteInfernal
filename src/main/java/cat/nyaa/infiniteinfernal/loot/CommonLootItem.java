package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import org.bukkit.inventory.ItemStack;

public class CommonLootItem implements ILootItem, ISerializable {
    private final InfPlugin plugin;

    @Serializable
    String nbt = "";

    @Serializable
    String name = "";

    @Serializable
    public boolean dynamic = false;

    public CommonLootItem(){
        plugin = InfPlugin.plugin;
    }

    public CommonLootItem(InfPlugin plugin, String name, ItemStack item){
        this.plugin = plugin;
        this.name = name;
        this.nbt = ItemStackUtils.itemToBase64(item);
    }

    @Override
    public ItemStack getItemStack() {
        return ItemStackUtils.itemFromBase64(nbt);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toNbt() {
        return nbt;
    }

    @Override
    public int getWeight(String level) {
        return LootManager.getWeightForLevel(this, level);
    }

    @Override
    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }
}
