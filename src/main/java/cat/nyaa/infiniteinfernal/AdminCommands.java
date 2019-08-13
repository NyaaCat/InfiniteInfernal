package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.loot.ILootItem;
import cat.nyaa.infiniteinfernal.loot.LootManager;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.mob.MobManager;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class AdminCommands extends CommandReceiver {
    private InfPlugin plugin;

    public AdminCommands(InfPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        this.plugin = plugin;
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }

    @SubCommand(value = "reload", permission = "im.admin")
    public void onReload(CommandSender sender, Arguments arguments) {
        plugin.onReload();
    }

    @SubCommand(value = "spawn", permission = "im.spawn")
    public void onSpawn(CommandSender sender, Arguments arguments) {
        String mobName = arguments.nextString();
        if (arguments.top() == null) {
            if (sender instanceof Player) {
                Block targetBlock = ((Player) sender).getTargetBlock(null, 50);
                Location location = Utils.randomSpawnLocation(targetBlock.getLocation(), 0, 1);
                if (location != null) {
                    MobManager.instance().spawnMobByName(mobName, location, null);
                }
                return;
            }
        }
        String worldName = arguments.nextString();
        double x = arguments.nextDouble();
        double y = arguments.nextDouble();
        double z = arguments.nextDouble();
        String top = arguments.top();
        Integer level = top == null ? null : Integer.valueOf(top);
        World world = Bukkit.getWorld(worldName);
        MobManager.instance().spawnMobByName(mobName, new Location(world, x, y, z), level);
    }

    @SubCommand(value = "addloot", permission = "im.addloot")
    public void onAddLoot(CommandSender sender, Arguments arguments) {
        LootManager lootManager = plugin.getLootManager();
        String itemName = arguments.nextString();
        boolean isDynamic = arguments.top() != null && arguments.nextBoolean();
        if (sender instanceof Player) {
            ItemStack itemInMainHand = ((Player) sender).getInventory().getItemInMainHand();
            if (itemInMainHand.getType().equals(Material.AIR)) {
                new Message("").append(I18n.format("loot.add.error.no_item"))
                        .send(sender);
                return;
            }
            ILootItem loot = lootManager.getLoot(itemName);
            if (loot != null) {
                Message message = new Message("");
                message.append(I18n.format("loot.add.error.exists", itemName), loot.getItemStack())
                        .send(sender);
                return;
            }
            lootManager.addLoot(itemName, isDynamic, itemInMainHand);
            Message append = new Message("");
            append.append(I18n.format("loot.add.success", itemName, isDynamic), itemInMainHand)
                    .send(sender);

        } else {
            new Message(I18n.format("error.not_player")).send(sender);
        }
    }

    @SubCommand(value = "getloot", permission = "im.getloot")
    public void onGetLoot(CommandSender sender, Arguments arguments) {
        String lootName = arguments.nextString();
        ILootItem loot = plugin.getLootManager().getLoot(lootName);
        if (loot != null) {
            if (InfPlugin.plugin.config().isGetDropMessageEnabled) {
                new Message("").append(I18n.format("loot.get.success"), loot.getItemStack())
                        .send(sender);
            }
            if (sender instanceof Player) {
                if (!InventoryUtils.addItem((Player) sender, loot.getItemStack())) {
                    ((Player) sender).getWorld().dropItem(((Player) sender).getLocation(), loot.getItemStack());
                }
            }
        } else {
            new Message("").append(I18n.format("loot.get.no_item", lootName))
                    .send(sender);
        }
    }

    @SubCommand(value = "setdrop", permission = "im.setdrop")
    public void onSetDrop(CommandSender sender, Arguments arguments) {
        String itemName = arguments.nextString();
        int level = arguments.nextInt();
        int weight = arguments.nextInt();
        ILootItem lootItem = LootManager.instance().getLoot(itemName);
        if (lootItem == null) {
            new Message(I18n.format("loot.set.no_item", itemName))
                    .send(sender);
            return;
        }
        LootManager.instance().addCommonLoot(lootItem, level, weight);
        new Message("").append(I18n.format("loot.set.success", level, weight), lootItem.getItemStack())
                .send(sender);
    }

    @SubCommand(value = "inspect", permission = "im.inspect")
    public void onInspect(CommandSender sender, Arguments arguments) {
        String target = arguments.nextString();
        String id = arguments.nextString();
        switch (target) {
            case "item":
                ILootItem loot = LootManager.instance().getLoot(id);
                if (loot == null) {
                    new Message("").append(I18n.format("inspect.item.no_item"))
                            .send(sender);
                } else {
                    new Message("").append(I18n.format("inspect.item.success", id, loot.isDynamic()), loot.getItemStack())
                            .send(sender);
                }
                break;
            case "level":
                int level = Integer.parseInt(id);
                List<ILootItem> loots = LootManager.instance().getLoots(level);
                if (!loots.isEmpty()) {
                    new Message("").append(I18n.format("inspect.level.success", level))
                            .send(sender);
                    loots.forEach(lootItem -> {
                        int weight = lootItem.getWeight(level);
                        new Message("").append(I18n.format("inspect.level.info", weight, lootItem.isDynamic()), lootItem.getItemStack())
                                .send(sender);
                    });
                }else {
                    new Message("").append(I18n.format("inspect.level.no_level", level))
                            .send(sender);
                }
                break;
            default:
                new Message("").append(I18n.format("inspect.error.invalid_action", target))
                        .send(sender);
                break;
        }
    }

    @SubCommand(value = "killall", permission = "im.kill.all")
    public void onKillAll(CommandSender sender, Arguments arguments) {
        String top = arguments.top();
        Collection<IMob> toKill;
        if (top != null) {
            World world = Bukkit.getWorld(top);
            if (world != null) {
                toKill = new LinkedList<>(MobManager.instance().getMobsInWorld(world));
            } else {
                new Message(I18n.format("killall.error.unknown_world", top))
                        .send(sender);
                return;
            }
        } else {
            toKill = new LinkedList<>(MobManager.instance().getMobs());
        }
        toKill.stream().forEach(iMob -> MobManager.instance().removeMob(iMob, false));
        new Message(I18n.format("killall.success", toKill.size()))
                .send(sender);
    }

    @SubCommand(value = "killDamages", permission = "im.kill.damages")
    public void onKillDamages(CommandSender sender, Arguments arguments) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getWorlds().stream().
                        forEach(world -> world.getLivingEntities().stream()
                                .forEach(entity -> {
                                    if (entity.getScoreboardTags().contains("inf_damage_indicator")) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                entity.remove();
                                            }
                                        }.runTask(InfPlugin.plugin);
                                    }
                                }));
            }
        }.runTaskAsynchronously(InfPlugin.plugin);
    }
}
