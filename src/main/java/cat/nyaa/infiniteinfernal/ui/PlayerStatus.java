package cat.nyaa.infiniteinfernal.ui;

import org.bukkit.ChatColor;

public enum PlayerStatus {
    NORMAL(ChatColor.GREEN), DAMAGED(ChatColor.DARK_RED), BUFFED(ChatColor.AQUA), HIT_TARGET(ChatColor.GOLD);

    private ChatColor color;

    PlayerStatus(ChatColor color){
        this.color = color;
    }

    public ChatColor getColor(){
        return color;
    }
}
