package cat.nyaa.infiniteinfernal.ui;

import org.bukkit.ChatColor;

public enum PlayerStatus {
    NORMAL(ChatColor.GREEN), DAMAGED(ChatColor.RED), BUFFED(ChatColor.BLUE), HIT_TARGET(ChatColor.YELLOW);

    private ChatColor color;

    PlayerStatus(ChatColor color){
        this.color = color;
    }

    public ChatColor getColor(){
        return color;
    }
}
