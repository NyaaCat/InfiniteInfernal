package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.BadCommandException;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupCommands extends CommandReceiver {

    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public GroupCommands(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
        manageCommand = new ManageCommands(plugin, _i18n);
    }

    @SubCommand(value = "join", permission = "im.group", tabCompleter = "joinCompleter")
    public void onJoin(CommandSender sender, Arguments arguments) {
        String groupName = arguments.nextString();
        Group group = GroupManager.getInstance().getGroup(groupName);
        if (group == null) {
            new Message("").append(I18n.format("group.join.no_group", groupName)).send(sender);
            return;
        }
        Player player = null;
        if (sender.hasPermission("im.group.admin") && arguments.top() != null) {
            player = arguments.nextPlayer();
        } else {
            player = asPlayer(sender);
        }
        if (group.containsMember(player)) {
            new Message("").append(I18n.format("group.join.already_in", groupName)).send(sender);
            return;
        }
        join(player, group);
        new Message("").append(I18n.format("group.join.success", player.getName(), groupName)).send(sender);
        if (sender != player){
            Message append = new Message("").append(I18n.format("group.join.success", player.getName(), groupName));
            group.broadcast(append);
        }
    }

    public List<String> joinCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        GroupManager groupManager = GroupManager.getInstance();
        switch (arguments.remains()) {
            case 1:
                completeStr.add("<teamName>");
                completeStr.addAll(groupManager.getGroupNames());
                break;
            case 2:
                String s = arguments.nextString();
                Group group = groupManager.getGroup(s);
                if (sender.hasPermission("im.group.admin")) {
                    List<String> collect = Bukkit.getServer().getOnlinePlayers().stream()
                            .filter(player -> group == null ? true : !group.containsMember(player))
                            .map(HumanEntity::getName)
                            .collect(Collectors.toList());
                    completeStr.addAll(collect);
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "leave", permission = "im.group", tabCompleter = "leaveCompleter")
    public void onLeave(CommandSender sender, Arguments arguments) {
        if (sender.hasPermission("im.group.admin") && arguments.top() != null){
            Player player = arguments.nextPlayer();
            new Message("").append(I18n.format("group.leave.cleared", player.getName())).send(sender);
            return;
        }
        Player player = null;
        if (sender.hasPermission("im.group.admin") && arguments.top() != null) {
            player = arguments.nextPlayer();
        } else {
            player = asPlayer(sender);
        }
        Group group = GroupManager.getInstance().getPlayerGroup(player);
        if (group == null){
            new Message("").append(I18n.format("group.leave.no_group")).send(sender);
            return;
        }
        group.leaveMember(player);
        group.broadcast(new Message("").append(I18n.format("group.leave.success", player.getName())));
    }

    public List<String> leaveCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                if (sender.hasPermission("im.group.admin")){
                    completeStr.addAll(GroupManager.getInstance().getPlayerNames());
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "invite", permission = "im.group", tabCompleter = "inviteCompleter")
    public void onInvite(CommandSender sender, Arguments arguments) {
        Player player = arguments.nextPlayer();
        Group target = null;
        Player inviter = null;
        GroupManager instance = GroupManager.getInstance();
        if (sender.isOp() || sender.hasPermission("im.group.admin")){
            if (arguments.top() != null) {
                target = instance.getGroup(arguments.nextString());
            }else {
                if (sender instanceof Player) {
                    target = instance.getPlayerGroup(((Player) sender));
                    inviter = ((Player) sender);
                }
            }
        }
        if (target == null){
            new Message("").append(I18n.format("group.invite.no_group", player.getName())).send(sender);
            return;
        }
        GroupManager.getInstance().invite(inviter, player, target);
        new Message("").append(I18n.format("group.invite.requested", player.getName(), target.getName()));
    }

    public List<String> inviteCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "accept", permission = "im.group")
    public void onAccept(CommandSender sender, Arguments arguments) {
        Player player = asPlayer(sender);
        GroupManager.InviteSession session = GroupManager.getInstance().getSession(player.getUniqueId());
        if (session == null) {
            new Message("").append(I18n.format("group.invite.no_session")).send(sender);
            return;
        }
        if (session.player.equals(player)) {
            Group target = session.target;
            join(player, target);
            GroupManager.getInstance().removeSession(player.getUniqueId());
        }
    }

    @SubCommand(value = "deny", permission = "im.group")
    public void onDeny(CommandSender sender, Arguments arguments) {
        Player player = asPlayer(sender);
        GroupManager.InviteSession session = GroupManager.getInstance().getSession(player.getUniqueId());
        if (session == null) {
            new Message("").append(I18n.format("group.invite.no_session")).send(sender);
            return;
        }
        if (session.player.equals(player)) {
            Group target = session.target;
            deny(session);
        }
    }

    private void deny(GroupManager.InviteSession session) {
        new Message("").append(I18n.format("group.invite.denied_inviter", session.player.getName(), session.target.getName())).send(session.inviter);
        new Message("").append(I18n.format("group.invite.denied_player", session.target.getName())).send(session.player);
        GroupManager.getInstance().removeSession(session.player.getUniqueId());
    }

    @SubCommand(value = "addPlayer", permission = "im.group.admin", tabCompleter = "addPlayerCompleter")
    public void onAddPlayer(CommandSender sender, Arguments arguments) {
        String s = arguments.nextString();
        Group group = GroupManager.getInstance().getGroup(s);
        Player player = arguments.nextPlayer();
        if (group == null){
            new Message("").append(I18n.format("group.add_player.no_group", s)).send(sender);
            return;
        }
        join(player, group);
    }

    public List<String> addPlayerCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                completeStr.addAll(GroupManager.getInstance().getGroupNames());
                break;
            case 2:
                String s = arguments.nextString();
                Group group = GroupManager.getInstance().getGroup(s);
                if (group!=null){
                    completeStr.addAll(Bukkit.getOnlinePlayers().stream().filter(player -> !group.containsMember(player)).map(HumanEntity::getName).collect(Collectors.toList()));
                }
                break;
        }
        return filtered(arguments, completeStr);
    }

    @SubCommand(value = "create", permission = "im.group", tabCompleter = "createCompleter")
    public void onCreateGroup(CommandSender sender, Arguments arguments){
        String name = arguments.nextString();
        GroupManager instance = GroupManager.getInstance();
        if (instance.getGroup(name) != null){
            new Message("").append(I18n.format("group.create.exists", name)).send(sender);
            return;
        }
        Player player = asPlayer(sender);
        instance.createGroup(name, player);
        new Message("").append(I18n.format("group.create.success", name)).send(sender);
    }

    @SubCommand(value = "manage", permission = "im.group", tabCompleter = "leaveCompleter")
    ManageCommands manageCommand;

    private void join(Player player, Group group) {
        checkAndLeave(player);
        group.joinMember(player);
    }

    private void checkAndLeave(Player player) {
        Group group1 = GroupManager.getInstance().getPlayerGroup(player);
        if (group1 != null){
            group1.leaveMember(player);
            group1.broadcast(new Message("").append(I18n.format("group.leave.success", player.getName())));
        }
    }

    public List<String> sampleCompleter(CommandSender sender, Arguments arguments) {
        List<String> completeStr = new ArrayList<>();
        switch (arguments.remains()) {
            case 1:
                break;
        }
        return filtered(arguments, completeStr);
    }

    private static List<String> filtered(Arguments arguments, List<String> completeStr) {
        String next = arguments.at(arguments.length() - 1);
        return completeStr.stream().filter(s -> s.startsWith(next)).collect(Collectors.toList());
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }

    public class ManageCommands extends CommandReceiver {

        /**
         * @param plugin for logging purpose only
         * @param _i18n
         */
        public ManageCommands(Plugin plugin, ILocalizer _i18n) {
            super(plugin, _i18n);
        }

        Set<CommandSender> disbandSet = new HashSet<>();

        @SubCommand(value = "admin", permission = "im.group", tabCompleter = "adminCompleter")
        public void onAdmin(CommandSender sender, Arguments arguments) {
            String action = arguments.nextString();
            Group group;
            switch (action){
                case "add":
                case "remove":
                    group = GroupManager.getInstance().getPlayerGroup(asPlayer(sender));
                    break;
                default:
                    group = GroupManager.getInstance().getGroup(action);
                    if (group == null){
                        new Message("").append(I18n.format("group.admin.no_group_name"));
                    }
                    action = arguments.nextString();
            }
            if (group == null){
                throw new IllegalArgumentException();
            }
            if (!sender.isOp() && !group.isAdmin(asPlayer(sender))){
                new Message("").append(I18n.format("error.permission")).send(sender);
                return;
            }
            Player player = arguments.nextPlayer();
            switch (action){
                case "add":
                    group.addAdmin(player);
                    new Message("").append(I18n.format("group.manage.admin.add_success", player.getName(), group.name)).send(sender);
                    if (sender != player){
                        new Message("").append(I18n.format("group.manage.admin.add_success", player.getName(), group.name)).send(player);
                    }
                    break;
                case "remove":
                    group.removeAdmin(player);
                    new Message("").append(I18n.format("group.manage.admin.remove_success", player.getName(), group.name)).send(sender);
                    if (sender != player){
                        new Message("").append(I18n.format("group.manage.admin.add_success", player.getName(), group.name)).send(player);
                    }
                    break;
                default:
                    throw new BadCommandException();
            }
        }

        public List<String> adminCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    if (sender instanceof Player){
                        Player player = (Player) sender;
                        Group group = GroupManager.getInstance().getPlayerGroup(player);
                        if (group != null){
                            completeStr.addAll(group.getMemberNames());
                        }else {
                            completeStr.addAll(GroupManager.getInstance().getGroupNames());
                        }
                    }
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "kick", permission = "im.group", tabCompleter = "kickCompleter")
        public void onKick(CommandSender sender, Arguments arguments) {
            Player player = asPlayer(sender);
            Group group = GroupManager.getInstance().getPlayerGroup(player);
            if (group == null){
                new Message("").append(I18n.format("group.manage.no_group")).send(sender);
                return;
            }
            if (!sender.isOp() && !group.isAdmin(asPlayer(sender))){
                new Message("").append(I18n.format("error.permission")).send(sender);
                return;
            }
            group.kick(player);
        }

        public List<String> kickCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    if (sender instanceof Player){
                        Group group = GroupManager.getInstance().getPlayerGroup((Player) sender);
                        if (group !=null){
                            completeStr.addAll(group.getMemberNames());
                        }
                    }
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "expDropMode", permission = "im.group", tabCompleter = "expDropModeCompleter")
        public void onDropMode(CommandSender sender, Arguments arguments){
            Player player = asPlayer(sender);
            Group group = GroupManager.getInstance().getPlayerGroup(player);

            if (arguments.top() == null){
                new Message(group.dropMode.name()).send(sender);
                return;
            }
            Group.ExpDropMode expDropMode = arguments.nextEnum(Group.ExpDropMode.class);
            group.setDropMode(expDropMode);
            new Message("").append(I18n.format("group.expdropmode_change", expDropMode.name())).send(sender);
        }

        public List<String> expDropModeCompleter(CommandSender sender, Arguments arguments) {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(Stream.of(Group.ExpDropMode.values()).map(Enum::name).collect(Collectors.toList()));
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "lootMode", permission = "im.group", tabCompleter = "lootModeCompleter")
        public void onLootMode(CommandSender sender, Arguments arguments) {
            Player player = asPlayer(sender);
            Group group = GroupManager.getInstance().getPlayerGroup(player);

            if (arguments.top() == null) {
                new Message(group.dropMode.name()).send(sender);
                return;
            }
            Group.ExpDropMode expDropMode = arguments.nextEnum(Group.ExpDropMode.class);
            group.setDropMode(expDropMode);
            new Message("").append(I18n.format("group.expdropmode_change", expDropMode.name())).send(sender);
        }

        public List<String> lootModeCompleter(CommandSender sender, Arguments arguments)  {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    completeStr.addAll(Stream.of(Group.LootMode.values()).map(Enum::name).collect(Collectors.toList()));
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @SubCommand(value = "disband", permission = "im.group", tabCompleter = "disbandCompleter")
        public void onDisband(CommandSender sender, Arguments arguments){
            if (arguments.top() != null){
                String s = arguments.nextString();
                Group group = GroupManager.getInstance().getGroup(s);
                if (group == null){
                    new Message("").append(I18n.format("group.disband.no_group")).send(sender);
                    return;
                }
                if (sender.isOp() || sender.hasPermission("im.group.admin")){
                    if (!disbandSet.contains(sender)){
                        sendConfirmMessage(sender, group.getName());
                    }else {
                        disband(group, sender.getName());
                    }
                }
                return;
            }
            Player player = asPlayer(sender);
            Group playerGroup = GroupManager.getInstance().getPlayerGroup(player);
            if (playerGroup == null){
                new Message("").append(I18n.format("group.disband.no_group")).send(sender);
                return;
            }
            if (!disbandSet.contains(sender)){
                sendConfirmMessage(sender, playerGroup.getName());
            }else {
                disband(playerGroup, sender.getName());
            }
        }

        private void disband(Group group, String name) {
            group.disband();
            GroupManager.getInstance().disband(group);
        }

        private void sendConfirmMessage(CommandSender sender, String name) {
            new Message("").append(I18n.format("group.disband.confirm_message", name)).send(sender);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (disbandSet.remove(sender)) {
                        new Message("").append("group.disband.aborted").send(sender);
                    }
                }
            }.runTaskLater(InfPlugin.plugin, 200);
        }

        public List<String> disbandCompleter(CommandSender sender, Arguments arguments)  {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    if (sender.isOp() || sender.hasPermission("im.group.admin")){
                        completeStr.addAll(GroupManager.getInstance().getGroupNames());
                    }
                    break;
            }
            return filtered(arguments, completeStr);
        }

        public List<String> sampleCompleter(CommandSender sender, Arguments arguments)  {
            List<String> completeStr = new ArrayList<>();
            switch (arguments.remains()) {
                case 1:
                    break;
            }
            return filtered(arguments, completeStr);
        }

        @Override
        public String getHelpPrefix() {
            return null;
        }
    }
}

