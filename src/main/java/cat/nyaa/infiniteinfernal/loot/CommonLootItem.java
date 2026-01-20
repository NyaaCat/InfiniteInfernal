package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import org.bukkit.configuration.ConfigurationSection;
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
    public void deserialize(ConfigurationSection config) {
        ISerializable.deserialize(config, this);
        nbt = refreshNbt(nbt);
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
    public int getWeight(int level) {
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

    private String refreshNbt(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return base64;
        }
        try {
            ItemStack itemStack = ItemStackUtils.itemFromBase64(base64);
            if (itemStack == null) {
                return base64;
            }
            return ItemStackUtils.itemToBase64(itemStack);
        } catch (Exception ex) {
            return base64;
        }
    }
}
