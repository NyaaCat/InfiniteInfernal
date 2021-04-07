package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.configs.BroadcastMode;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class BroadcastManager extends FileConfigure {
    @Serializable
    Map<String, String> broadcastSettings = new LinkedHashMap<>();

    public void setReceivetype(String uuid, String receiveType){
        broadcastSettings.put(uuid, receiveType);
        this.save();
    }

    public BroadcastMode getReceiveType(World world, String uuid){
        return BroadcastMode.valueOf(broadcastSettings.computeIfAbsent(uuid, s -> {
            final Config config = InfPlugin.plugin.config();
            return config.defaultBroadcastMode.name();
        }));
    }

    @Override
    protected String getFileName() {
        return "broadcast.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return InfPlugin.plugin;
    }

    public int getNearbyRange(World world) {
        final int broadcastRange = InfPlugin.plugin.config().broadcastRange;
        return broadcastRange;
    }

    public void toggle(Player sender) {
        BroadcastMode receiveType = getReceiveType(sender.getWorld(), sender.getUniqueId().toString());
        switch (receiveType){
            case ALL:
                receiveType = BroadcastMode.NEARBY;
                break;
            case NEARBY:
                receiveType = BroadcastMode.SELF_ONLY;
                break;
            case SELF_ONLY:
                receiveType = BroadcastMode.OFF;
                break;
            case OFF:
                receiveType = BroadcastMode.ALL;
                break;
        }
        setReceivetype(sender.getUniqueId().toString(), receiveType.name());
        sendHint(sender,receiveType);
    }

    public void sendHint(Player sender, BroadcastMode type) {
        Message message = new Message("");
        switch (type){
            case ALL:
                message.append(I18n.format("imi.state_all"));
                break;
            case NEARBY:
                message.append(I18n.format("imi.state_near"));
                break;
            case SELF_ONLY:
                message.append(I18n.format("imi.state_self"));
                break;
            case OFF:
                message.append(I18n.format("imi.state_off"));
                break;
        }
        message.send(sender);
    }
}
