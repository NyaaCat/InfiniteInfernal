package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.BroadcastManager;
import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.BroadcastMode;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.Message;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupListener implements Listener {


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        GroupManager instance = GroupManager.getInstance();
        instance.autoJoin(player);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GroupManager instance = GroupManager.getInstance();
        instance.savePlayerState(player);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLoot(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        Player killer = mob.getKiller();
        GroupManager gm = GroupManager.getInstance();
        Group playerGroup = gm.getPlayerGroup(killer);
        if (playerGroup == null) return;
        int groupShareRange = InfPlugin.plugin.config().groupShareRange;
        Group.ExpDropMode expDropMode = playerGroup.getExpDropMode();
        Group.LootMode lootMode = playerGroup.getLootMode();
        if (expDropMode == Group.ExpDropMode.SELF && lootMode == Group.LootMode.KILLER) return;
        List<Player> collect = mob.getNearbyEntities(groupShareRange, groupShareRange, groupShareRange).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> ((Player) entity))
                .filter(entity -> entity.getLocation().distance(mob.getLocation()) < groupShareRange)
                .filter(entity -> playerGroup.containsMember(entity))
                .collect(Collectors.toList());
        if (expDropMode.equals(Group.ExpDropMode.AVERAGE)) {
            int size = collect.size();
            int droppedExp = event.getDroppedExp();
            float v = ((float) droppedExp) / ((float) size);
            collect.forEach(player -> player.setExp(v));
            event.setDroppedExp(0);
        }
        if (lootMode.equals(Group.LootMode.ROLL)) {
            Player player = Utils.randomPick(collect);
            List<ItemStack> drops = event.getDrops();
            if (!drops.isEmpty()) {
                drops.forEach(itemStack -> {
                            Utils.addToPlayer(player, itemStack);
                            String format = I18n.format("group.drop.roll");
                            format = format.replaceAll("\\{playerName}", player.getName());
                            new Message("").append(format, itemStack)
                                    .broadcast(Message.MessageType.CHAT, player1 -> Utils.shouldReceiveMessage(killer, player1));
                        }
                );
            }
        }
        List<ItemStack> drops = event.getDrops();

    }



}
