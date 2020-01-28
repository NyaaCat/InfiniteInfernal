package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.infiniteinfernal.ui.impl.VarMana;
import cat.nyaa.infiniteinfernal.ui.impl.VarRage;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

public class BaseUi {
    private UUID uuid;

    VarMana mana;
    VarRage rage;
    PlayerStatus status = PlayerStatus.NORMAL;

    public Message buildMessage() {
        Message message = new Message("");
        BarInfo rageInfo = new BarInfo(rage.getMaxValue(), rage.getValue(), rage.getDamageIndicate());
        BarInfo manaInfo = new BarInfo(mana.getMaxValue(), mana.getValue(), mana.getDamageIndicate());

        StringBuilder sb = new StringBuilder();

        append(sb, "❰", rageInfo.empty, ChatColor.BLACK);
        append(sb, "❰", rageInfo.indicate, ChatColor.YELLOW);
        append(sb, "❰", rageInfo.filled, ChatColor.GOLD);

        sb.append(String.format(" &6&l% 4.0f", rage.getValue()));

        sb.append(" &6&lRAGE").append(String.format("&%c ❖ ",status.getColor().getChar())).append("&b&lMANA ");

        sb.append(String.format("&b&l% 4.0f ", mana.getValue()));

        append(sb, "❱", manaInfo.filled, ChatColor.BLUE);
        append(sb, "❱", manaInfo.indicate, ChatColor.AQUA);
        append(sb, "❱", manaInfo.empty, ChatColor.BLACK);

        return message.append(Utils.colored(sb.toString()));
    }
    void append(StringBuilder sb, String charr, int amount, ChatColor color){
        sb.append(String.format("&%c",color.getChar()));
        for (int i = 0; i < amount; i++) {
            sb.append(charr);
        }
    }

    public VarMana getMana() {
        return mana;
    }

    public VarRage getRage() {
        return rage;
    }

    public BaseUi(UUID player) {
        this.uuid = player;
        mana = new VarMana(100, 100);
        rage = new VarRage(0, 100);
    }

    public Player getPlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    void regeneration(Player player, int tick) {
        RegenerationEvent manaEvt = new RegenerationEvent(player, mana, tick);
        RegenerationEvent rageEvt = new RegenerationEvent(player, rage, tick);
        Bukkit.getPluginManager().callEvent(manaEvt);
        Bukkit.getPluginManager().callEvent(rageEvt);
        double manaReg = (manaEvt.getRegeneration() / 20d) * manaEvt.getFactor();
        double rageReg = (rageEvt.getRegeneration() / 20d) * rageEvt.getFactor();
        mana.regenerate(manaReg);
        rage.regenerate(-rageReg);
        mana.refreshIndicate(tick);
        rage.refreshIndicate(tick);
    }

    public void refreshUi(Player poll) {
        Message message = buildMessage();
        message.send(poll, Message.MessageType.ACTION_BAR);
    }

    class BarInfo {
        int empty, filled, indicate;

        BarInfo(double max, double value, double indicate) {
            double totalSplits = 20;
            this.indicate = (int) Math.ceil((indicate / max) * totalSplits);
            filled = (int) Math.ceil((value / max) * totalSplits);
            int remains = (int) totalSplits - filled;
            this.indicate = Math.min(remains, this.indicate);
            empty = remains - this.indicate;
        }
    }
}
