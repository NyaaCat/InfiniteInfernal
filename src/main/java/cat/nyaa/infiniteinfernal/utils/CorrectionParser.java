package cat.nyaa.infiniteinfernal.utils;

import cat.nyaa.infiniteinfernal.configs.IllegalConfigException;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CorrectionParser {
    public static List<ICorrector> parseStrs(List<String> strings) {
        List<ICorrector> corrections = new ArrayList<>();
        if (!strings.isEmpty()) {
            strings.forEach(s -> {
                String[] split = s.split(":", 2);
                try {
                    switch (split[0]) {
                        case "effect":
                            corrections.add(parseEffect(split[1]));
                            break;
                        case "attribute":
                            corrections.add(parseAttribute(split[1]));
                            break;
                        case "enchant":
                            corrections.add(parseEnchant(split[1]));
                            break;
                        default:
                            Bukkit.getLogger().log(Level.WARNING, "", new IllegalConfigException("wrong target " + s + "."));
                    }
                }catch (Exception e){
                    Bukkit.getLogger().log(Level.WARNING, "", new IllegalConfigException("wrong target " + s + "."));
                }
            });
        }
        return corrections;
    }

    public static ICorrector parseStr(String str) {
        String[] split = str.split(":", 2);
        try {
            switch (split[0]) {
                case "effect":
                    return parseEffect(split[1]);
                case "attribute":
                    return parseAttribute(split[1]);
                case "enchant":
                    return parseEnchant(split[1]);
                default:
                    Bukkit.getLogger().log(Level.WARNING, "", new IllegalConfigException("wrong target " + split[0] + "."));
            }
        }catch (Exception e){
            Bukkit.getLogger().log(Level.WARNING, "", new IllegalConfigException("wrong target " + split[0] + "."));
        }
        return null;
    }

    static CorrectorEffect parseEffect(String effect) {
        String[] split = effect.split(":");
        PotionEffectType effectType = PotionEffectType.getByName(split[0]);
        Integer amplifier = Integer.valueOf(split[1]);
        return new CorrectorEffect(effectType, amplifier);
    }

    static CorrectorEnchant parseEnchant(String enchant) {
        String[] split = enchant.split(":", 2);
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(split[0]));
        return new CorrectorEnchant(enchantment, Integer.valueOf(split[1]));
    }

    static CorrectorAttribute parseAttribute(String attrs) {
        String[] split = attrs.split(":", 2);
        Attribute attribute = Attribute.valueOf(split[0]);
        Integer integer = Integer.valueOf(split[1]);
        return new CorrectorAttribute(attribute, integer);
    }
}
