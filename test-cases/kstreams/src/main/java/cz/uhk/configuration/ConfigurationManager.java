package cz.uhk.configuration;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Log4j2
public class ConfigurationManager {

    // Prefix for the configuration path within the JAR
    private static final String CONFIG_PATH_PREFIX = "/config";
    // Name of the YAML configuration file
    private static final String CONFIG_FILE_YAML = "config.yaml";

    // Holds the configuration object
    @Getter
    private final BaseHierarchicalConfiguration config;

    // Singleton instance
    private static ConfigurationManager instance;

    // Private constructor to enforce singleton pattern
    private ConfigurationManager() throws ConfigurationException {
        // Read configuration from YAML file
        InputStream inputStream = getResourceAsStream(CONFIG_FILE_YAML);
        YAMLConfiguration yamlConfiguration = new YAMLConfiguration();
        yamlConfiguration.read(inputStream);
        this.config = yamlConfiguration;
    }

    // Method to get the singleton instance of ConfigurationManager
    public static synchronized ConfigurationManager getInstance() throws ConfigurationException {
        if (null == instance) {
            instance = new ConfigurationManager();
        }
        return instance;
    }

    // Method to get an input stream for a specified resource
    public InputStream getResourceAsStream(String resource) {
        try {
            // Try to read the file from the filesystem
            File configFile = new File(CONFIG_PATH_PREFIX, resource);
            if (configFile.canRead()) {
                log.info("Reading file from filesystem. Filename: {}", resource);
                return new FileInputStream(configFile);
            }
        } catch (Exception ignored) {
        }
        // If the file cannot be read from the filesystem, read it from the packaged JAR
        if (resource.startsWith("."))
            resource = resource.substring(1);
        if (!resource.startsWith("/") && !resource.startsWith("\\"))
            resource = "/" + resource;
        log.info("Cannot read file from filesystem, so reading file from packaged jar. Filename: {}", resource);
        return this.getClass().getResourceAsStream(resource);
    }

    // Method to get the absolute path of a specified resource
    public static String getResourcePath(String resource) {
        try {
            // Try to get the absolute path of the file from the filesystem
            File configFile = new File(CONFIG_PATH_PREFIX, resource);
            if (configFile.canRead()) {
                return configFile.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}