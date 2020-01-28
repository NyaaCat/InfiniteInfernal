package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.nyaacore.Message;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class Group {
    String name;
    Set<UUID> members = new LinkedHashSet<>();
    Set<UUID> admins = new LinkedHashSet<>();
    ExpDropMode dropMode = ExpDropMode.AVERAGE;
    LootMode lootMode = LootMode.ROLL;

    public Group(String name){
        this.name = name;
    }

    public void setDropMode(ExpDropMode dropMode) {
        this.dropMode = dropMode;
    }

    public void setLootMode(LootMode lootMode) {
        this.lootMode = lootMode;
    }

    public void joinMember(Player member){
        members.add(member.getUniqueId());
        broadcast(new Message("").append(I18n.format("group.join.success", member.getName(), getName())));
    }

    public void addAdmin(Player member){
        admins.add(member.getUniqueId());
    }

    public boolean isAdmin(Player player){
        return admins.contains(player.getUniqueId());
    }

    public Set<Player> getMembers(){
        return Collections.unmodifiableSet(members.stream().map(uuid -> Bukkit.getPlayer(uuid)).collect(Collectors.toSet()));
    }

    public boolean containsMember(Player sender) {
        return members.contains(sender.getUniqueId());
    }

    public void leaveMember(UUID player) {
        members.remove(player);
        admins.remove(player);
    }

    public String getName() {
        return name;
    }

    public void broadcast(Message message, Player from){
        new Message(I18n.format("group.chat_format", from)).append(message.inner);
        members.forEach(uuid -> message.send(Bukkit.getOfflinePlayer(uuid)));
    }

    public Collection<? extends String> getMemberNames() {
        return members.stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList());
    }

    public void removeAdmin(Player player) {
        admins.remove(player.getUniqueId());
    }

    public void broadcast(Message append) {
        members.forEach(uuid -> append.send(Bukkit.getOfflinePlayer(uuid)));
    }

    public void kick(Player player) {
        broadcast(new Message("").append(I18n.format("group.kick.player", player.getName())));
        members.remove(player);
    }

    public void disband() {
        broadcast(new Message("").append(I18n.format("group.disband.message")));
        new ArrayList<>(members).forEach(uuid -> {
            leaveMember(uuid);
        });
        admins.clear();
    }

    public ExpDropMode getExpDropMode() {
        return dropMode;
    }

    public LootMode getLootMode() {
        return lootMode;
    }

    enum ExpDropMode {
        AVERAGE, SELF;
    }

    enum LootMode{
        ROLL, KILLER;
    }
}
