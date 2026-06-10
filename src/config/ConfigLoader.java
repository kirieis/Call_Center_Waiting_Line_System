package config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Utility class for loading and managing configuration.
 * 
 * Handles:
 * - Finding the project root directory
 * - Loading settings.properties from correct location
 * - Resolving all file paths relative to project root
 * 
 * @author Group 7
 */
public class ConfigLoader {

    private static final String CONFIG_FILE_NAME = "settings.properties";
    private static final String CONFIG_FOLDER = "src/config";
    private Properties config;
    private Path projectRoot;

    public ConfigLoader() {
        this.projectRoot = findProjectRoot();
        this.config = new Properties();
        loadConfig();
    }

    /**
     * Finds the project root directory by looking for key files/folders.
     * 
     * Starting from the current directory, it searches upward for:
     * - src/ folder AND README.md file (typical project markers)
     * 
     * @return Path to project root, or current directory if not found
     */
    private Path findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));

        // Search up to 5 levels to find project root
        for (int i = 0; i < 5; i++) {
            if (isProjectRoot(current)) {
                System.out.println("  DONE! Project root found: " + current.toAbsolutePath());
                return current;
            }
            Path parent = current.getParent();
            if (parent == null)
                break;
            current = parent;
        }

        System.out.println("  [!] Project root not found, using current directory [!]");
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * Checks if a directory is the project root.
     * 
     * @param path directory to check
     * @return true if directory contains src/ and README.md
     */
    private boolean isProjectRoot(Path path) {
        File srcDir = path.resolve("src").toFile();
        File readmeFile = path.resolve("README.md").toFile();
        return srcDir.exists() && srcDir.isDirectory() && readmeFile.exists();
    }

    /**
     * Loads configuration from settings.properties file.
     */
    private void loadConfig() {
        Path configPath = projectRoot.resolve(CONFIG_FOLDER).resolve(CONFIG_FILE_NAME);
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            System.out.println("  [!] Configuration file not found at: " + configPath.toAbsolutePath());
            System.out.println("  [!] Using default values");
            return;
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            config.load(fis);
            System.out.println("  DONE! Configuration loaded from: " + configPath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("  [!] Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Gets a configuration property with a default value.
     * 
     * @param key          property key
     * @param defaultValue default value if key not found
     * @return property value or default value
     */
    public String getProperty(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }

    /**
     * Gets an integer configuration property with a default value.
     * 
     * @param key          property key
     * @param defaultValue default value if key not found
     * @return property value as integer or default value
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = config.getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Resolves a file path relative to the project root.
     * 
     * This ensures that all relative paths in settings.properties
     * are resolved from the project root, not the current working directory.
     * 
     * @param relativePath relative path from settings.properties
     * @return full absolute path resolved from project root
     */
    public String resolvePath(String relativePath) {
        Path fullPath = projectRoot.resolve(relativePath);
        return fullPath.toAbsolutePath().toString();
    }

    /**
     * Gets the project root path.
     * 
     * @return Path to project root
     */
    public Path getProjectRoot() {
        return projectRoot;
    }

    /**
     * Gets the raw data file path (resolved from project root).
     * 
     * @return absolute path to raw data CSV file
     */
    public String getRawDataPath() {
        String configPath = getProperty("data.raw.calls.path", "data/CustomerCalls.csv");
        return resolvePath(configPath);
    }

    /**
     * Gets the call history file path (resolved from project root).
     * 
     * @return absolute path to call history CSV file
     */
    public String getHistoryPath() {
        String configPath = getProperty("data.call.history.path", "data/call_history.csv");
        return resolvePath(configPath);
    }

    /**
     * Gets circular queue capacity from configuration.
     * 
     * @return capacity value
     */
    public int getCircularQueueCapacity() {
        return getIntProperty("circular.queue.capacity", 100);
    }

    /**
     * Gets default data generation count from configuration.
     * 
     * @return number of calls to generate
     */
    public int getGenerateCount() {
        return getIntProperty("generator.default.count", 10000);
    }

    /**
     * Gets VIP priority bonus from configuration.
     * 
     * @return VIP bonus points
     */
    public int getVipBonus() {
        return getIntProperty("priority.vip.bonus", 50);
    }

    /**
     * Gets repeat call priority multiplier from configuration.
     * 
     * @return repeat multiplier
     */
    public int getRepeatMultiplier() {
        return getIntProperty("priority.repeat.multiplier", 10);
    }

    /**
     * Gets aging algorithm threshold in milliseconds.
     * 
     * @return threshold in ms
     */
    public int getAgingThresholdMs() {
        return getIntProperty("aging.threshold.ms", 30000);
    }

    /**
     * Gets aging algorithm boost points.
     * 
     * @return boost points
     */
    public int getAgingBoost() {
        return getIntProperty("aging.boost", 5);
    }

    /**
     * Gets VIP percentage for data generation.
     * 
     * @return VIP percentage (0-100)
     */
    public int getVipPercentage() {
        return getIntProperty("generator.vip.percentage", 15);
    }

    /**
     * Gets maximum repeat calls for data generation.
     * 
     * @return max repeat calls
     */
    public int getMaxRepeatCalls() {
        return getIntProperty("generator.max.repeat.calls", 10);
    }
}
