package cat.nyaa.infiniteinfernal.loot;

import cat.nyaa.infiniteinfernal.BroadcastManager;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.BroadcastMode;
import cat.nyaa.infiniteinfernal.configs.MessageConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.HexColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public class InfMessager implements IMessager {
    private List<String> drop;
    private List<String> mobKill;
    private List<String> noDrop;
    private List<String> playerKill;
    private List<String> specialDrop;

    public InfMessager(MessageConfig config) {
        setupFromConfig(config);
    }

    private void setupFromConfig(MessageConfig config) {
        drop = config.drop;
        mobKill = config.mobKill;
        noDrop = config.noDrop;
        playerKill = config.playerKill;
        specialDrop = config.specialDrop;
    }

    @Override
    public void broadcastToWorld(IMob deadMob, Entity killer, ILootItem item) {
        if (killer == null)return;
        buildMessage(MessageType.MOB_KILLED, deadMob, killer, item)
                .broadcast(Message.MessageType.CHAT, player -> Utils.shouldReceiveMessage(killer, player));
        if (item != null) {
            buildMessage(MessageType.DROP, deadMob, killer, item)
                    .broadcast(Message.MessageType.CHAT, player -> Utils.shouldReceiveMessage(killer, player));
        } else {
            buildMessage(MessageType.NO_DROP, deadMob, killer, item)
                    .broadcast(Message.MessageType.CHAT, player -> Utils.shouldReceiveMessage(killer, player));
        }
    }

    private Message buildMessage(MessageType messageType, IMob deadMob, Entity killer, ILootItem lootItem) {
        BroadcastMessage message = new BroadcastMessage("");
        String str = "";
        EntityEquipment equipment;
        switch (messageType) {
            case DROP:
                str = Utils.randomPick(drop);
                if (str == null) {
                    Bukkit.getLogger().log(Level.WARNING, I18n.format("message.error.drop"));
                    return new Message(I18n.format("message.error.drop"));
                }
                str = str.replaceAll("\\{player\\.name}", killer.getName())
                        .replaceAll("\\{mob\\.name}", deadMob.getTaggedName())
                        .replaceAll("\\{drop\\.item}", "{itemName}")
                        .replaceAll("\\{drop\\.amount}", "{amount}");
                message.append(HexColorUtils.hexColored( str), lootItem.getItemStack());
                break;
            case SPETIAL_DROP:
                str = Utils.randomPick(specialDrop);
                if (str == null) {
                    Bukkit.getLogger().log(Level.WARNING, I18n.format("message.error.special_drop"));
                    return new Message(I18n.format("message.error.special_drop"));
                }
                str = str.replaceAll("\\{player\\.name}", killer.getName())
                        .replaceAll("\\{mob\\.name}", deadMob.getTaggedName())
                        .replaceAll("\\{drop\\.special}", "{itemName}")
                        .replaceAll("\\{drop\\.amount}", "{amount}");
                message.append(HexColorUtils.hexColored( str), lootItem.getItemStack());
                break;
            case NO_DROP:
                str = Utils.randomPick(noDrop);
                if (str == null) {
                    Bukkit.getLogger().log(Level.WARNING, I18n.format("message.error.drop"));
                    return new Message(I18n.format("message.error.drop"));
                }
                str = str.replaceAll("\\{player\\.name}", killer.getName())
                        .replaceAll("\\{mob\\.name}", deadMob.getTaggedName());
                message.append(HexColorUtils.hexColored( str));
                break;
            case MOB_KILLED:
                if (!(killer instanceof LivingEntity)){
                    break;
                }
                str = Utils.randomPick(playerKill);
                if (str == null) {
                    Bukkit.getLogger().log(Level.WARNING, I18n.format("message.error.drop"));
                    return new Message(I18n.format("message.error.drop"));
                }
                equipment = ((LivingEntity) killer).getEquipment();
                str = str.replaceAll("\\{player\\.name}", killer.getName())
                        .replaceAll("\\{mob\\.name}", deadMob.getTaggedName())
                        .replaceAll("\\{player\\.item}", "{itemName}");
                message.append(HexColorUtils.hexColored( str), equipment == null ? new ItemStack(Material.AIR) : equipment.getItemInMainHand());
                break;
            case PLAYER_KILLED:
                str = Utils.randomPick(mobKill);
                if (str == null) {
                    Bukkit.getLogger().log(Level.WARNING, I18n.format("message.error.drop"));
                    return new Message(I18n.format("message.error.drop"));
                }
                equipment = deadMob.getEntity().getEquipment();
                str = str.replaceAll("\\{mob\\.name}", deadMob.getTaggedName())
                        .replaceAll("\\{player\\.name}", killer.getName())
                        .replaceAll("\\{mob\\.item}", "{itemName}");
                message.append(HexColorUtils.hexColored( str), equipment == null ? new ItemStack(Material.AIR) : equipment.getItemInMainHand());
                break;
        }
        return message;
    }

    @Override
    public void broadcastExtraToWorld(IMob deadMob, Entity killer, ILootItem item) {
        if (killer == null)return;
        if (item != null) {
            buildMessage(MessageType.SPETIAL_DROP, deadMob, killer, item)
                    .broadcast(Message.MessageType.CHAT, player -> Utils.shouldReceiveMessage(killer, player));
        }
    }

    public enum MessageType {
        DROP, SPETIAL_DROP, NO_DROP, MOB_KILLED, PLAYER_KILLED
    }

    class BroadcastMessage extends Message {

        public BroadcastMessage(String text) {
            super(text);
        }

        @Override
        public Message broadcast(MessageType type, Predicate<Player> playerFilter) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playerFilter.test(player)) {
                    this.send(player, type);
                }
            }
            return this;
        }
    }
}
