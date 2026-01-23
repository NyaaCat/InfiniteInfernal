package cat.nyaa.infiniteinfernal;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests to ensure all language keys used in code are defined in language files.
 * This helps catch missing translations early in development.
 */
public class LanguageKeyTest {

    private static final String JAVA_SOURCE_PATH = "src/main/java";
    private static final String LANG_RESOURCE_PATH = "src/main/resources/lang";

    // Pattern to match I18n.format("key") or I18n.format("key", ...)
    private static final Pattern I18N_FORMAT_PATTERN = Pattern.compile("I18n\\.format\\(\"([^\"]+)\"");

    /**
     * Extracts all language keys used in Java source files.
     */
    private Set<String> extractKeysFromCode() throws IOException {
        Set<String> keys = new TreeSet<>();
        Path sourcePath = Paths.get(JAVA_SOURCE_PATH);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Source path does not exist: " + sourcePath.toAbsolutePath());
        }

        Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    String content = Files.readString(file);
                    Matcher matcher = I18N_FORMAT_PATTERN.matcher(content);
                    while (matcher.find()) {
                        keys.add(matcher.group(1));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return keys;
    }

    /**
     * Extracts all keys defined in a YAML language file.
     */
    private Set<String> extractKeysFromYaml(Path yamlFile) throws IOException {
        Set<String> keys = new TreeSet<>();
        Yaml yaml = new Yaml();

        try (InputStream is = Files.newInputStream(yamlFile)) {
            Map<?, ?> data = yaml.load(is);
            if (data != null) {
                extractKeysRecursive(data, "", keys);
            }
        }

        return keys;
    }

    /**
     * Recursively extracts keys from nested YAML structure.
     */
    @SuppressWarnings("unchecked")
    private void extractKeysRecursive(Map<?, ?> map, String prefix, Set<String> keys) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String keyName = String.valueOf(entry.getKey());
            String key = prefix.isEmpty() ? keyName : prefix + "." + keyName;
            Object value = entry.getValue();

            if (value instanceof Map) {
                extractKeysRecursive((Map<?, ?>) value, key, keys);
            } else if (value != null) {
                // Any non-null leaf value is a valid language string (String, Boolean, Integer, etc.)
                keys.add(key);
            }
        }
    }

    /**
     * Tests that all language keys used in code exist in en_US.yml (default language).
     */
    @Test
    public void testAllKeysExistInEnglish() throws IOException {
        Set<String> codeKeys = extractKeysFromCode();
        Path enUsFile = Paths.get(LANG_RESOURCE_PATH, "en_US.yml");

        if (!Files.exists(enUsFile)) {
            fail("Language file not found: " + enUsFile.toAbsolutePath());
        }

        Set<String> yamlKeys = extractKeysFromYaml(enUsFile);

        Set<String> missingKeys = codeKeys.stream()
                .filter(key -> !yamlKeys.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));

        if (!missingKeys.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Missing language keys in en_US.yml (").append(missingKeys.size()).append(" keys):\n");
            for (String key : missingKeys) {
                message.append("  - ").append(key).append("\n");
            }
            fail(message.toString());
        }
    }

    /**
     * Tests that all language keys used in code exist in zh_CN.yml.
     */
    @Test
    public void testAllKeysExistInChinese() throws IOException {
        Set<String> codeKeys = extractKeysFromCode();
        Path zhCnFile = Paths.get(LANG_RESOURCE_PATH, "zh_CN.yml");

        if (!Files.exists(zhCnFile)) {
            fail("Language file not found: " + zhCnFile.toAbsolutePath());
        }

        Set<String> yamlKeys = extractKeysFromYaml(zhCnFile);

        Set<String> missingKeys = codeKeys.stream()
                .filter(key -> !yamlKeys.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));

        if (!missingKeys.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append("Missing language keys in zh_CN.yml (").append(missingKeys.size()).append(" keys):\n");
            for (String key : missingKeys) {
                message.append("  - ").append(key).append("\n");
            }
            fail(message.toString());
        }
    }

    /**
     * Tests that en_US.yml and zh_CN.yml have the same keys.
     * This ensures translations are in sync.
     */
    @Test
    public void testLanguageFilesInSync() throws IOException {
        Path enUsFile = Paths.get(LANG_RESOURCE_PATH, "en_US.yml");
        Path zhCnFile = Paths.get(LANG_RESOURCE_PATH, "zh_CN.yml");

        if (!Files.exists(enUsFile) || !Files.exists(zhCnFile)) {
            fail("Language files not found");
        }

        Set<String> enUsKeys = extractKeysFromYaml(enUsFile);
        Set<String> zhCnKeys = extractKeysFromYaml(zhCnFile);

        Set<String> onlyInEnUs = new TreeSet<>(enUsKeys);
        onlyInEnUs.removeAll(zhCnKeys);

        Set<String> onlyInZhCn = new TreeSet<>(zhCnKeys);
        onlyInZhCn.removeAll(enUsKeys);

        StringBuilder message = new StringBuilder();
        boolean hasDifferences = false;

        if (!onlyInEnUs.isEmpty()) {
            hasDifferences = true;
            message.append("Keys only in en_US.yml (").append(onlyInEnUs.size()).append(" keys):\n");
            for (String key : onlyInEnUs) {
                message.append("  - ").append(key).append("\n");
            }
        }

        if (!onlyInZhCn.isEmpty()) {
            hasDifferences = true;
            message.append("Keys only in zh_CN.yml (").append(onlyInZhCn.size()).append(" keys):\n");
            for (String key : onlyInZhCn) {
                message.append("  - ").append(key).append("\n");
            }
        }

        if (hasDifferences) {
            fail(message.toString());
        }
    }

    /**
     * Prints a report of all language keys (useful for debugging).
     */
    @Test
    public void printLanguageKeyReport() throws IOException {
        Set<String> codeKeys = extractKeysFromCode();

        Path enUsFile = Paths.get(LANG_RESOURCE_PATH, "en_US.yml");
        Path zhCnFile = Paths.get(LANG_RESOURCE_PATH, "zh_CN.yml");

        Set<String> enUsKeys = Files.exists(enUsFile) ? extractKeysFromYaml(enUsFile) : Collections.emptySet();
        Set<String> zhCnKeys = Files.exists(zhCnFile) ? extractKeysFromYaml(zhCnFile) : Collections.emptySet();

        System.out.println("=== Language Key Report ===");
        System.out.println("Keys used in code: " + codeKeys.size());
        System.out.println("Keys in en_US.yml: " + enUsKeys.size());
        System.out.println("Keys in zh_CN.yml: " + zhCnKeys.size());
        System.out.println();

        // Find unused keys in language files
        Set<String> unusedInEnUs = new TreeSet<>(enUsKeys);
        unusedInEnUs.removeAll(codeKeys);
        if (!unusedInEnUs.isEmpty()) {
            System.out.println("Unused keys in en_US.yml (" + unusedInEnUs.size() + "):");
            for (String key : unusedInEnUs) {
                System.out.println("  - " + key);
            }
        }

        Set<String> unusedInZhCn = new TreeSet<>(zhCnKeys);
        unusedInZhCn.removeAll(codeKeys);
        if (!unusedInZhCn.isEmpty()) {
            System.out.println("Unused keys in zh_CN.yml (" + unusedInZhCn.size() + "):");
            for (String key : unusedInZhCn) {
                System.out.println("  - " + key);
            }
        }

        // This test always passes, it's just for reporting
        assertTrue(true);
    }
}
