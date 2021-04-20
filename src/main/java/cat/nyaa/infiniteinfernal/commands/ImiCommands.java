package cat.nyaa.infiniteinfernal.commands;

import cat.nyaa.infiniteinfernal.BroadcastManager;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.BroadcastMode;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ImiCommands extends CommandReceiver {
    InfPlugin plugin;

    public ImiCommands(InfPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        this.plugin = plugin;
    }

//    @Override
//    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//        if (!(sender instanceof Player)) return false;
//        if (args.length == 0) {
//            if (sender.hasPermission("infernal_mobs.imi")) {
//                toggleState(((Player) sender));
//                return true;
//            }
//        }
//        return super.onCommand(sender, command, label, args);
//    }

    @SubCommand(isDefaultCommand = true)
    public void onImi(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) return;
        toggleState(((Player) sender));
    }

    @SubCommand(value = "all", permission = "imi.command")
    public void onAll(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) return;
        setState(((Player) sender), BroadcastMode.ALL);
    }

    @SubCommand(value = "me", permission = "imi.command")
    public void onSelfOnly(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) return;
        setState(((Player) sender), BroadcastMode.SELF_ONLY);
    }

    @SubCommand(value = "near", permission = "imi.command")
    public void onNearby(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) return;
        setState(((Player) sender), BroadcastMode.NEARBY);
    }

    @SubCommand(value = "off", permission = "imi.command")
    public void onOff(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) return;
        setState(((Player) sender), BroadcastMode.OFF);
    }

//    @SubCommand("global")
//    public void onGlobalChange(CommandSender sender, Arguments arguments) {
//        if (!sender.isOp()) {
//            new Message(I18n.format("error.permission"))
//                    .send(sender);
//            return;
//        }
//        String arg1 = arguments.nextString();
//        try {
//            BroadcastMode receiveType = BroadcastMode.valueOf(arg1);
//            Bukkit.getScheduler().runTaskAsynchronously(InfPlugin.plugin, () -> {
//                BroadcastManager broadcastConfig = new BroadcastManager();
//                broadcastConfig.globalSetting(receiveType);
//                new Message(I18n.format("imi.global_success", receiveType.fileName()))
//                        .send(sender);
//            });
//        } catch (Exception e) {
//            new Message(I18n.format("error.wrong_argument"));
//        }
//    }

    private void setState(Player sender, BroadcastMode type) {
        BroadcastManager broadcastConfig = InfPlugin.plugin.getBroadcastManager();
        broadcastConfig.setReceivetype(sender.getUniqueId().toString(), type.name());
        broadcastConfig.sendHint(sender, type);
    }

    private void toggleState(Player sender) {
        InfPlugin.plugin.getBroadcastManager().toggle(sender);
    }

    public ImiCommands(JavaPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }


}
