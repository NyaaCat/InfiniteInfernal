package cat.nyaa.infiniteinfernal.config;

import cat.nyaa.infiniteinfernal.Config;
import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.configs.*;
import cat.nyaa.nyaacore.configuration.ISerializable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Resolves dot-notation paths to config values.
 * Path syntax:
 * - "/" separates logical parts (config sections, files)
 * - "." accesses properties within an object
 *
 * Examples:
 * - "language" - direct field in main config
 * - "bossbar.enabled" - property of bossbar section
 * - "worlds/inf.aggro.range.max" - world "inf", then nested properties
 * - "mobs/boss-zombie.healthOverride" - mob file "boss-zombie", property
 * - "levels/24.attr.health" - level 24 config, nested property
 * - "abilities/set-1.weight" - ability set config, property
 * - "regions/spawn-area.mobs" - region config, property (list)
 */
public class ConfigPathResolver {

    /**
     * Result of a path resolution containing the parent object, field, and current value.
     */
    public static class PathResult {
        public final Object parent;
        public final Field field;
        public final Object value;
        public final Class<?> type;
        public final Type genericType;
        public final String path;
        public final boolean isMapEntry;
        public final String mapKey;
        public final boolean isListIndex;
        public final int listIndex;

        public PathResult(Object parent, Field field, Object value, String path) {
            this.parent = parent;
            this.field = field;
            this.value = value;
            this.type = field != null ? field.getType() : (value != null ? value.getClass() : Object.class);
            this.genericType = field != null ? field.getGenericType() : null;
            this.path = path;
            this.isMapEntry = false;
            this.mapKey = null;
            this.isListIndex = false;
            this.listIndex = -1;
        }

        public PathResult(Object parent, Field field, Object value, String path,
                          boolean isMapEntry, String mapKey, boolean isListIndex, int listIndex) {
            this.parent = parent;
            this.field = field;
            this.value = value;
            this.type = field != null ? field.getType() : (value != null ? value.getClass() : Object.class);
            this.genericType = field != null ? field.getGenericType() : null;
            this.path = path;
            this.isMapEntry = isMapEntry;
            this.mapKey = mapKey;
            this.isListIndex = isListIndex;
            this.listIndex = listIndex;
        }

        public Class<?> getListElementType() {
            if (genericType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                    return (Class<?>) actualTypeArguments[0];
                }
            }
            return String.class;
        }

        public Class<?> getMapValueType() {
            if (genericType instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
                if (actualTypeArguments.length > 1 && actualTypeArguments[1] instanceof Class) {
                    return (Class<?>) actualTypeArguments[1];
                }
            }
            return Object.class;
        }
    }

    private final Config config;

    public ConfigPathResolver(Config config) {
        this.config = config;
    }

    /**
     * Resolves a path to a PathResult.
     *
     * @param path The path to resolve (e.g., "worlds/inf.aggro.range.max")
     * @return PathResult containing the resolved value and metadata
     * @throws IllegalArgumentException if the path is invalid
     */
    public PathResult resolve(String path) throws IllegalArgumentException {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        // Split by "/" to get logical segments
        String[] segments = path.split("/", 2);
        String firstSegment = segments[0];
        String remainingPath = segments.length > 1 ? segments[1] : null;

        // Check for standalone config shortcuts
        Object currentObject = config;
        String propertyPath = firstSegment;

        if (remainingPath != null) {
            // We have a "/" so the first segment is a config type
            switch (firstSegment) {
                case "mobs":
                    return resolveNamedDirConfig(config.mobConfigs, remainingPath, "mobs/" + remainingPath);
                case "levels":
                    return resolveDirConfig(config.levelConfigs, remainingPath, "levels/" + remainingPath);
                case "abilities":
                    return resolveNamedDirConfig(config.abilityConfigs, remainingPath, "abilities/" + remainingPath);
                case "regions":
                    return resolveNamedDirConfig(config.regionConfigs, remainingPath, "regions/" + remainingPath);
                case "worlds":
                    return resolveWorldConfig(remainingPath, "worlds/" + remainingPath);
                default:
                    throw new IllegalArgumentException("Unknown config section: " + firstSegment);
            }
        }

        // No "/", so resolve directly from main config
        return resolvePropertyPath(config, propertyPath, propertyPath);
    }

    private PathResult resolveNamedDirConfig(NamedDirConfigs<?> configs, String path, String fullPath) {
        // Split by "." - first part is the config name, rest is property path
        String[] parts = path.split("\\.", 2);
        String configName = parts[0];
        String propertyPath = parts.length > 1 ? parts[1] : null;

        Object configObj = configs.get(configName);
        if (configObj == null) {
            throw new IllegalArgumentException("Config not found: " + configName);
        }

        if (propertyPath == null) {
            // Return the config object itself
            return new PathResult(configs, null, configObj, fullPath);
        }

        return resolvePropertyPath(configObj, propertyPath, fullPath);
    }

    private PathResult resolveDirConfig(DirConfigs<?> configs, String path, String fullPath) {
        // Split by "." - first part is the config ID (integer), rest is property path
        String[] parts = path.split("\\.", 2);
        String configIdStr = parts[0];
        String propertyPath = parts.length > 1 ? parts[1] : null;

        int configId;
        try {
            configId = Integer.parseInt(configIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid level ID: " + configIdStr);
        }

        Object configObj = configs.get(configId);
        if (configObj == null) {
            throw new IllegalArgumentException("Config not found for ID: " + configId);
        }

        if (propertyPath == null) {
            return new PathResult(configs, null, configObj, fullPath);
        }

        return resolvePropertyPath(configObj, propertyPath, fullPath);
    }

    private PathResult resolveWorldConfig(String path, String fullPath) {
        // Split by "." - first part is the world name, rest is property path
        String[] parts = path.split("\\.", 2);
        String worldName = parts[0];
        String propertyPath = parts.length > 1 ? parts[1] : null;

        WorldConfig worldConfig = config.worlds.get(worldName);
        if (worldConfig == null) {
            throw new IllegalArgumentException("World config not found: " + worldName);
        }

        if (propertyPath == null) {
            return new PathResult(config.worlds, null, worldConfig, fullPath);
        }

        return resolvePropertyPath(worldConfig, propertyPath, fullPath);
    }

    private PathResult resolvePropertyPath(Object obj, String propertyPath, String fullPath) {
        String[] parts = propertyPath.split("\\.", 2);
        String currentPart = parts[0];
        String remainingPath = parts.length > 1 ? parts[1] : null;

        // Check if it's a list index
        if (isNumeric(currentPart)) {
            int index = Integer.parseInt(currentPart);
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                if (index < 0 || index >= list.size()) {
                    throw new IllegalArgumentException("List index out of bounds: " + index);
                }
                Object element = list.get(index);
                if (remainingPath == null) {
                    return new PathResult(obj, null, element, fullPath, false, null, true, index);
                }
                return resolvePropertyPath(element, remainingPath, fullPath);
            }
        }

        // Check if obj is a Map
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Object value = map.get(currentPart);
            if (remainingPath == null) {
                return new PathResult(obj, null, value, fullPath, true, currentPart, false, -1);
            }
            if (value == null) {
                throw new IllegalArgumentException("Map key not found: " + currentPart);
            }
            return resolvePropertyPath(value, remainingPath, fullPath);
        }

        // Try to find a field
        Field field = findSerializableField(obj.getClass(), currentPart);
        if (field == null) {
            throw new IllegalArgumentException("Field not found: " + currentPart + " in " + obj.getClass().getSimpleName());
        }

        field.setAccessible(true);
        try {
            Object value = field.get(obj);
            if (remainingPath == null) {
                return new PathResult(obj, field, value, fullPath);
            }
            if (value == null) {
                throw new IllegalArgumentException("Field value is null: " + currentPart);
            }
            return resolvePropertyPath(value, remainingPath, fullPath);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access field: " + currentPart, e);
        }
    }

    private Field findSerializableField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                ISerializable.Serializable annotation = field.getAnnotation(ISerializable.Serializable.class);
                if (annotation != null) {
                    String fieldName = annotation.name().isEmpty() ? field.getName() : annotation.name();
                    if (fieldName.equals(name) || field.getName().equals(name)) {
                        return field;
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Lists available keys/fields at a given path.
     */
    public List<String> listKeys(String path) {
        List<String> keys = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            // List top-level sections
            keys.add("language");
            keys.add("nameTag");
            keys.add("bossbar");
            keys.add("tags");
            keys.add("groupShareRange");
            keys.add("enableActionbarInfo");
            keys.add("groupCapacity");
            keys.add("worlds");
            keys.add("enabled");
            keys.add("mobs");
            keys.add("levels");
            keys.add("abilities");
            keys.add("regions");
            return keys;
        }

        // Special handling for standalone config types
        if (path.equals("mobs")) {
            keys.addAll(config.mobConfigs.keys());
            return keys;
        }
        if (path.equals("levels")) {
            for (Integer id : config.levelConfigs.keys()) {
                keys.add(String.valueOf(id));
            }
            return keys;
        }
        if (path.equals("abilities")) {
            keys.addAll(config.abilityConfigs.keys());
            return keys;
        }
        if (path.equals("regions")) {
            keys.addAll(config.regionConfigs.keys());
            return keys;
        }
        if (path.equals("worlds")) {
            keys.addAll(config.worlds.keySet());
            return keys;
        }

        try {
            PathResult result = resolve(path);
            Object value = result.value;

            if (value instanceof Map) {
                for (Object key : ((Map<?, ?>) value).keySet()) {
                    keys.add(String.valueOf(key));
                }
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    keys.add(String.valueOf(i));
                }
            } else if (value instanceof ISerializable) {
                keys.addAll(getSerializableFieldNames(value.getClass()));
            } else if (value != null) {
                keys.addAll(getSerializableFieldNames(value.getClass()));
            }
        } catch (IllegalArgumentException e) {
            // Path doesn't exist, return empty list
        }

        return keys;
    }

    private List<String> getSerializableFieldNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                ISerializable.Serializable annotation = field.getAnnotation(ISerializable.Serializable.class);
                if (annotation != null) {
                    names.add(field.getName());
                }
            }
            current = current.getSuperclass();
        }
        return names;
    }

    /**
     * Sets a value at the given path.
     */
    public void setValue(String path, Object value) throws IllegalArgumentException {
        PathResult result = resolve(path);

        if (result.isMapEntry && result.parent instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result.parent;
            map.put(result.mapKey, value);
        } else if (result.isListIndex && result.parent instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) result.parent;
            list.set(result.listIndex, value);
        } else if (result.field != null) {
            try {
                result.field.setAccessible(true);
                result.field.set(result.parent, value);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Cannot set field value", e);
            }
        } else {
            throw new IllegalArgumentException("Cannot set value at path: " + path);
        }
    }

    /**
     * Deletes a value at the given path (for lists/maps).
     */
    public boolean deleteValue(String path) throws IllegalArgumentException {
        PathResult result = resolve(path);

        if (result.isMapEntry && result.parent instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result.parent;
            return map.remove(result.mapKey) != null;
        } else if (result.isListIndex && result.parent instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) result.parent;
            list.remove(result.listIndex);
            return true;
        }

        throw new IllegalArgumentException("Can only delete map entries or list elements");
    }

    /**
     * Adds a value to a list at the given path.
     */
    public void addToList(String path, Object value) throws IllegalArgumentException {
        PathResult result = resolve(path);

        if (result.value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) result.value;
            list.add(value);
        } else {
            throw new IllegalArgumentException("Path does not point to a list: " + path);
        }
    }

    /**
     * Removes a value from a list at the given path.
     */
    public boolean removeFromList(String path, Object value) throws IllegalArgumentException {
        PathResult result = resolve(path);

        if (result.value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) result.value;
            return list.remove(value);
        }

        throw new IllegalArgumentException("Path does not point to a list: " + path);
    }

    /**
     * Adds an entry to a map at the given path.
     */
    public void addToMap(String path, String key, Object value) throws IllegalArgumentException {
        PathResult result = resolve(path);

        if (result.value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result.value;
            map.put(key, value);
        } else {
            throw new IllegalArgumentException("Path does not point to a map: " + path);
        }
    }

    /**
     * Removes an entry from a map at the given path.
     */
    public boolean removeFromMap(String path, String key) throws IllegalArgumentException {
        PathResult result = resolve(path);

        if (result.value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result.value;
            return map.remove(key) != null;
        }

        throw new IllegalArgumentException("Path does not point to a map: " + path);
    }
}
