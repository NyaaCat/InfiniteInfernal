package cat.nyaa.infiniteinfernal.data;

import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;

@Table("player_data")
public class PlayerData {
    @Column(primary = true, unique = true)
    public String uuid;
    @Column(name = "imi")
    public String killMessageReceiveMode = "ALL";
    @Column(name = "imd")
    public String actionbarReceiveMode = "ON";
    @Column(name = "mana")
    public int manaBase = 100;


}
