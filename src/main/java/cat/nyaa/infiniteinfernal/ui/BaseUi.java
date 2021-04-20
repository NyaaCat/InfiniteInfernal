package cat.nyaa.infiniteinfernal.ui;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.data.Database;
import cat.nyaa.infiniteinfernal.data.PlayerData;
import cat.nyaa.infiniteinfernal.ui.impl.VarMana;
import cat.nyaa.infiniteinfernal.ui.impl.VarRage;
import cat.nyaa.infiniteinfernal.utils.Utils;
import cat.nyaa.nyaacore.Message;
import com.google.common.cache.Cache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ExecutionException;

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
        append(sb, "❰", rageInfo.indicate, ChatColor.DARK_RED);
        append(sb, "❰", rageInfo.filled, ChatColor.RED);

        sb.append(String.format(" &6&l% 4.0f", rage.getValue()));

        sb.append(" &6&lRAGE").append(String.format("&%c ◄ ❖ ► ", status.getColor().getChar())).append("&b&lMANA ");

        sb.append(String.format("&b&l% 4.0f ", mana.getValue()));

        append(sb, "❱", manaInfo.filled, ChatColor.BLUE);
        append(sb, "❱", manaInfo.indicate, ChatColor.AQUA);
        append(sb, "❱", manaInfo.empty, ChatColor.BLACK);

        return message.append(Utils.colored(sb.toString()));
    }

    void append(StringBuilder sb, String charr, int amount, ChatColor color) {
        sb.append(String.format("&%c", color.getChar()));
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
        PlayerData playerData = Database.getInstance().getPlayerData(player);
        mana = new VarMana(playerData.manaBase, playerData.manaBase, this);
        rage = new VarRage(playerData.rageBase, playerData.rageBase, this);
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
        rage.regenerate(rageReg);
        mana.refreshIndicate(tick);
        rage.refreshIndicate(tick);
    }

    public void refreshUi(Player poll) {
        if (status.equals(PlayerStatus.BUFFED)) {
            status = checkPlayer(poll);
        }
        Message message = buildMessage();
        message.send(poll, Message.MessageType.ACTION_BAR);
    }

    public void refreshIfAuto() {
        Player player = Bukkit.getPlayer(uuid);
        if(player == null) return;
        World world = player.getWorld();
        if ((InfPlugin.plugin.config().enableActionbarInfo && !InfPlugin.plugin.config().isEnabledInWorld(world)) && Database.getInstance().getPlayerData(player).actionbarReceiveMode.equals(UiReceiveMode.AUTO.name())) {
            refreshUi(player);
        }
    }

    public void refreshIfOn(Player poll) {
        if (!InfPlugin.plugin.config().enableActionbarInfo) return;
        UiReceiveMode uiReceiveMode;
        Cache<UUID, UiReceiveMode> cache = UiManager.cache;
        try {
            uiReceiveMode = cache.get(poll.getUniqueId(), () -> UiReceiveMode.valueOf(Database.getInstance().getPlayerData(poll).actionbarReceiveMode));
        } catch (ExecutionException e) {
            e.printStackTrace();
            PlayerData playerData = Database.getInstance().getPlayerData(poll);
            uiReceiveMode = UiReceiveMode.ON;
            playerData.actionbarReceiveMode = uiReceiveMode.name();
            Database.getInstance().setPlayerData(playerData);
        }
        if (uiReceiveMode.equals(UiReceiveMode.ON)) {
            refreshUi(poll);
        }else if (uiReceiveMode.equals(UiReceiveMode.AUTO) && InfPlugin.plugin.config().isEnabledInWorld(poll.getWorld())){
            refreshUi(poll);
        }
    }

    public void refreshBase(Player player){
        PlayerData playerData = Database.getInstance().getPlayerData(player);
        mana.setBaseMax(playerData.manaBase);
        rage.setBaseMax(playerData.rageBase);
    }

    PlayerStatus checkPlayer(Player player) {
        Collection<PotionEffect> activePotionEffects = player.getActivePotionEffects();
        if (activePotionEffects.stream().anyMatch(potionEffect -> buffList.contains(potionEffect.getType()) && potionEffect.getAmplifier() > 0)) {
            return PlayerStatus.BUFFED;
        } else return PlayerStatus.NORMAL;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    class BarInfo {
        int empty, filled, indicate;

        BarInfo(double max, double value, double indicate) {
            double totalSplits = 20;
            this.indicate = (int) Math.ceil((indicate / max) * totalSplits);
            filled = (int) Math.ceil((value / max) * totalSplits);
            int remains = (int) totalSplits - filled;
            this.indicate = Math.max(Math.min(remains, this.indicate), 0);
            empty = remains - this.indicate;
        }
    }

    static final Set<PotionEffectType> buffList = new HashSet<>();

    static {
        PotionEffectType[] potionTypes = {
                PotionEffectType.SPEED,
                PotionEffectType.FAST_DIGGING,
                PotionEffectType.INCREASE_DAMAGE,
                PotionEffectType.HEAL,
                PotionEffectType.JUMP,
                PotionEffectType.REGENERATION,
                PotionEffectType.DAMAGE_RESISTANCE,
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffectType.WATER_BREATHING,
                PotionEffectType.INVISIBILITY,
                PotionEffectType.NIGHT_VISION,
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.ABSORPTION,
                PotionEffectType.SATURATION,
                PotionEffectType.LUCK,
                PotionEffectType.SLOW_FALLING,
                PotionEffectType.CONDUIT_POWER,
                PotionEffectType.DOLPHINS_GRACE,
                PotionEffectType.HERO_OF_THE_VILLAGE,
        };
        buffList.addAll(Arrays.asList(potionTypes));
    }

    public static Set<PotionEffectType> getBuffList(){
        return buffList;
    }
}
