package cat.nyaa.infiniteinfernal.data;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.nyaacore.orm.annotations.Column;
import cat.nyaa.nyaacore.orm.annotations.Table;

@Table("player_data")
public class PlayerData {
    @Column(primary = true, unique = true)
    public String uuid;
    @Column(name = "imi")
    public String killMessageReceiveMode = "ALL";
    @Column(name = "imd")
    public String actionbarReceiveMode = "AUTO";
    @Column(name = "mana")
    public double manaBase = InfPlugin.plugin.config().defaultMana;
    @Column(name = "rage")
    public double rageBase = InfPlugin.plugin.config().defaultRage;

}
