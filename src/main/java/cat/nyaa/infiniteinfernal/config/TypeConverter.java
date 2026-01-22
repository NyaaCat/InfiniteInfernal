package cat.nyaa.infiniteinfernal.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;

import java.util.*;

/**
 * Utility class for converting string values to various types.
 * Supports primitives, enums, lists, and Bukkit registry types.
 */
public class TypeConverter {

    /**
     * Converts a string value to the target type.
     *
     * @param value      The string value to convert
     * @param targetType The target class type
     * @return The converted value
     * @throws IllegalArgumentException if conversion fails
     */
    @SuppressWarnings("unchecked")
    public static Object convert(String value, Class<?> targetType) throws IllegalArgumentException {
        if (value == null) {
            return null;
        }

        // Primitives and wrapper types
        if (targetType == int.class || targetType == Integer.class) {
            return parseInteger(value);
        }
        if (targetType == long.class || targetType == Long.class) {
            return parseLong(value);
        }
        if (targetType == double.class || targetType == Double.class) {
            return parseDouble(value);
        }
        if (targetType == float.class || targetType == Float.class) {
            return parseFloat(value);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return parseBoolean(value);
        }
        if (targetType == String.class) {
            return parseString(value);
        }

        // Enums
        if (Enum.class.isAssignableFrom(targetType)) {
            return parseEnum(value, (Class<? extends Enum>) targetType);
        }

        // Bukkit registry types
        if (targetType == Biome.class) {
            return parseBiome(value);
        }
        if (targetType == Sound.class) {
            return parseSound(value);
        }
        if (targetType == Particle.class) {
            return parseParticle(value);
        }
        if (targetType == EntityType.class) {
            return parseEntityType(value);
        }

        throw new IllegalArgumentException("Unsupported type: " + targetType.getName());
    }

    /**
     * Converts a string to a List of the given element type.
     * Supports comma-separated values.
     *
     * @param value       The string value (comma-separated or space-separated)
     * @param elementType The element type of the list
     * @return The converted list
     */
    public static List<Object> convertToList(String value, Class<?> elementType) {
        List<Object> result = new ArrayList<>();
        if (value == null || value.isEmpty()) {
            return result;
        }

        // Handle comma-separated values
        String[] parts = value.contains(",") ? value.split(",") : new String[]{value};
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(convert(trimmed, elementType));
            }
        }
        return result;
    }

    /**
     * Converts multiple string values to a List.
     *
     * @param values      The string values
     * @param elementType The element type of the list
     * @return The converted list
     */
    public static List<Object> convertToList(String[] values, Class<?> elementType) {
        List<Object> result = new ArrayList<>();
        for (String value : values) {
            result.add(convert(value.trim(), elementType));
        }
        return result;
    }

    /**
     * Parses a map entry from "key:value" format.
     *
     * @param entry     The entry string in "key:value" format
     * @param valueType The type of the value
     * @return A Map.Entry with the parsed key and value
     */
    public static Map.Entry<String, Object> parseMapEntry(String entry, Class<?> valueType) {
        int colonIndex = entry.lastIndexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Map entry must be in 'key:value' format: " + entry);
        }
        String key = entry.substring(0, colonIndex);
        String value = entry.substring(colonIndex + 1);
        return new AbstractMap.SimpleEntry<>(key, convert(value, valueType));
    }

    // Primitive parsers

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer: " + value);
        }
    }

    private static Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long: " + value);
        }
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid double: " + value);
        }
    }

    private static Float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid float: " + value);
        }
    }

    private static Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value) || "no".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Invalid boolean: " + value);
    }

    private static String parseString(String value) {
        // Handle quoted strings
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> parseEnum(String value, Class<? extends Enum> enumClass) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try exact match first, then case-insensitive
            for (Enum<?> constant : enumClass.getEnumConstants()) {
                if (constant.name().equalsIgnoreCase(value)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Invalid enum value '" + value + "' for " + enumClass.getSimpleName() +
                    ". Valid values: " + Arrays.toString(enumClass.getEnumConstants()));
        }
    }

    // Bukkit registry type parsers

    private static Biome parseBiome(String value) {
        NamespacedKey key = NamespacedKey.minecraft(value.toLowerCase());
        Biome biome = Registry.BIOME.get(key);
        if (biome == null) {
            throw new IllegalArgumentException("Unknown biome: " + value);
        }
        return biome;
    }

    private static Sound parseSound(String value) {
        try {
            return Sound.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown sound: " + value);
        }
    }

    private static Particle parseParticle(String value) {
        try {
            return Particle.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown particle: " + value);
        }
    }

    private static EntityType parseEntityType(String value) {
        try {
            return EntityType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown entity type: " + value);
        }
    }

    /**
     * Formats a value for display.
     *
     * @param value The value to format
     * @return A string representation of the value
     */
    public static String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                return "[]";
            }
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return "{}";
            }
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(entry.getKey()).append(": ").append(formatValue(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        return String.valueOf(value);
    }

    /**
     * Gets completion suggestions for a given type.
     *
     * @param targetType The target type
     * @return A list of suggested values
     */
    public static List<String> getCompletions(Class<?> targetType) {
        List<String> completions = new ArrayList<>();

        if (targetType == boolean.class || targetType == Boolean.class) {
            completions.add("true");
            completions.add("false");
            return completions;
        }

        if (Enum.class.isAssignableFrom(targetType)) {
            for (Object constant : targetType.getEnumConstants()) {
                completions.add(((Enum<?>) constant).name());
            }
            return completions;
        }

        if (targetType == Biome.class) {
            Registry.BIOME.stream().forEach(b -> completions.add(b.getKey().getKey()));
            return completions;
        }

        if (targetType == Particle.class) {
            for (Particle p : Particle.values()) {
                completions.add(p.name());
            }
            return completions;
        }

        if (targetType == EntityType.class) {
            for (EntityType et : EntityType.values()) {
                if (et.getEntityClass() != null) {
                    completions.add(et.name());
                }
            }
            return completions;
        }

        if (targetType == Sound.class) {
            for (Sound s : Sound.values()) {
                completions.add(s.name());
            }
            return completions;
        }

        // For numeric types, provide examples
        if (targetType == int.class || targetType == Integer.class) {
            completions.add("0");
            completions.add("1");
            completions.add("10");
            completions.add("100");
        } else if (targetType == double.class || targetType == Double.class ||
                   targetType == float.class || targetType == Float.class) {
            completions.add("0.0");
            completions.add("1.0");
            completions.add("0.5");
            completions.add("10.0");
        }

        return completions;
    }
}
