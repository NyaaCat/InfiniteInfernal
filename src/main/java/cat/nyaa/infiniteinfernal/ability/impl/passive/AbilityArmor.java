package cat.nyaa.infiniteinfernal.ability.impl.passive;

import cat.nyaa.infiniteinfernal.ability.AbilitySpawn;
import cat.nyaa.infiniteinfernal.ability.AbilityPassive;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.nyaacore.utils.ItemStackUtils;
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

    @Override
    public String getName() {
        return "Armor";
    }
}
