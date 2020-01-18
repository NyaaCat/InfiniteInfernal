package cat.nyaa.infiniteinfernal.group;

import cat.nyaa.infiniteinfernal.I18n;
import cat.nyaa.nyaacore.Message;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Group {
    String name;
    Set<Player> members = new LinkedHashSet<>();
    Set<UUID> admins = new LinkedHashSet<>();
    ExpDropMode dropMode = ExpDropMode.AVERAGE;
    LootMode lootMode = LootMode.KILLER;

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
        members.add(member);
        broadcast(new Message("").append(I18n.format("group.join.success", member.getName(), getName())));
    }

    public void addAdmin(Player member){
        admins.add(member.getUniqueId());
    }

    public boolean isAdmin(Player player){
        return admins.contains(player.getUniqueId());
    }

    public Set<Player> getMembers(){
        return Collections.unmodifiableSet(members);
    }

    public boolean containsMember(Player sender) {
        return members.contains(sender);
    }

    public void leaveMember(Player player) {
        members.remove(player);
        admins.remove(player.getUniqueId());
    }

    public String getName() {
        return name;
    }

    public void broadcast(Message message, Player from){
        new Message(I18n.format("group.chat_format", from)).append(message.inner);
        members.forEach(player -> message.send(player));
    }

    public Collection<? extends String> getMemberNames() {
        return members.stream().map(HumanEntity::getName).collect(Collectors.toList());
    }

    public void removeAdmin(Player player) {
        admins.remove(player.getUniqueId());
    }

    public void broadcast(Message append) {
        members.forEach(player -> append.send(player));
    }

    public void kick(Player player) {
        broadcast(new Message("").append(I18n.format("group.kick.player", player.getName())));
        members.remove(player);
    }

    public void disband() {
        broadcast(new Message("").append(I18n.format("group.disband.message")));
        new ArrayList<>(members).forEach(player -> leaveMember(player));
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
