package cat.nyaa.infiniteinfernal.api;

import cat.nyaa.infiniteinfernal.ui.impl.VarMana;
import cat.nyaa.infiniteinfernal.ui.impl.VarRage;
import org.bukkit.entity.Player;

public interface InfVarApi {
    VarRage getRage(Player player);
    VarMana getMana(Player player);
    int getTick();
}
