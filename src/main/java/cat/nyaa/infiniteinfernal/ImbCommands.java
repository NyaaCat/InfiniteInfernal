package cat.nyaa.infiniteinfernal;

import cat.nyaa.infiniteinfernal.data.Database;
import cat.nyaa.infiniteinfernal.data.PlayerData;
import cat.nyaa.infiniteinfernal.ui.UiManager;
import cat.nyaa.infiniteinfernal.ui.UiReceiveMode;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.cmdreceiver.Arguments;
import cat.nyaa.nyaacore.cmdreceiver.CommandReceiver;
import cat.nyaa.nyaacore.cmdreceiver.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ImbCommands extends CommandReceiver {
    /**
     * @param plugin for logging purpose only
     * @param _i18n
     */
    public ImbCommands(Plugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @SubCommand(isDefaultCommand = true)
    public void onDefault(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        toggle(player);
    }

    private void toggle(Player player) {
        PlayerData playerData = Database.getInstance().getPlayerData(player);
        UiReceiveMode uiReceiveMode = UiReceiveMode.valueOf(playerData.actionbarReceiveMode.toUpperCase());
        switch (uiReceiveMode){
            case ON:
                uiReceiveMode = UiReceiveMode.PARTIAL;
                new Message("").append(I18n.format("imd.partial")).send(player);
                break;
            case PARTIAL:
                uiReceiveMode = UiReceiveMode.OFF;
                new Message("").append(I18n.format("imd.off")).send(player);
                break;
            case OFF:
                uiReceiveMode = UiReceiveMode.ON;
                new Message("").append(I18n.format("imd.on")).send(player);
                break;
        }
        playerData.actionbarReceiveMode = uiReceiveMode.name();
        setData(player, playerData);
    }

    private void setData(Player player, PlayerData playerData) {
        UiManager.getInstance().invalidPlayerCache(player);
        Database.getInstance().setPlayerData(playerData);
    }

    @SubCommand(value = "on", permission = "imb.command")
    public void onOn(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        PlayerData playerData = Database.getInstance().getPlayerData(player);
        playerData.actionbarReceiveMode = UiReceiveMode.ON.name();
        new Message("").append(I18n.format("imd.on")).send(player);
        setData(player, playerData);
    }

    @SubCommand(value = "off", permission = "imb.command")
    public void onOff(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        PlayerData playerData = Database.getInstance().getPlayerData(player);
        playerData.actionbarReceiveMode = UiReceiveMode.OFF.name();
        new Message("").append(I18n.format("imd.off")).send(player);
        setData(player, playerData);
    }

    @SubCommand(value = "partial", permission = "imb.command")
    public void onPartial(CommandSender sender, Arguments arguments){
        Player player = asPlayer(sender);
        PlayerData playerData = Database.getInstance().getPlayerData(player);
        playerData.actionbarReceiveMode = UiReceiveMode.PARTIAL.name();
        new Message("").append(I18n.format("imd.partial")).send(player);
        setData(player, playerData);
    }

    @Override
    public String getHelpPrefix() {
        return null;
    }
}
