package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class AbilityArmor extends AbilityPassive implements AbilitySpawn {
    @Serializable
    public String head = "";
    @Serializable
    public String chest = "";
    @Serializable
    public String leg = "";
    @Serializable
    public String feet = "";
    @Serializable
    public String mainHand = "";
    @Serializable
    public String offhand = "";

    @Override
    public void deserialize(ConfigurationSection config) {
        ISerializable.deserialize(config, this);
        head = refreshNbt(head);
        chest = refreshNbt(chest);
        leg = refreshNbt(leg);
        feet = refreshNbt(feet);
        mainHand = refreshNbt(mainHand);
        offhand = refreshNbt(offhand);
    }

    @Override
    public void onSpawn(IMob iMob) {
        LivingEntity entity = iMob.getEntity();
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null){
            ItemStack[] itemStacks = parseArmors();
            equipment.setArmorContents(itemStacks);
        }
    }

    private ItemStack[] parseArmors() {
        ItemStack[] armorContents = new ItemStack[4];
        armorContents[0] = parseItem(head);
        armorContents[1] = parseItem(chest);
        armorContents[2] = parseItem(leg);
        armorContents[3] = parseItem(feet);
        return armorContents;
    }

    private ItemStack parseItem(String str){
        try {
            return ItemStackUtils.itemFromBase64(str);
        }catch (Exception e){
            return null;
        }
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

    @Override
    public String getName() {
        return "Armor";
    }
}
