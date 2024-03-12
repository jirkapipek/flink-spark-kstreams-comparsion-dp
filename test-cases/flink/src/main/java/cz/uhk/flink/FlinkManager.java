package cz.uhk.flink;

import cz.uhk.configuration.ConfigurationManager;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Log4j2
public class FlinkManager {

    private static final String KSTREAMS_CONFIG_FILE = "kStreams.properties";

    /**
     * static singleton instance of KafkaManager.
     */
    private static volatile FlinkManager instance;

    /**
     * Private constructor for singleton instance of KafkaManager.
     */
    public FlinkManager() {
    }

    /**
     * Return a singleton instance of KafkaManager.
     */
    public static synchronized FlinkManager getInstance() {
        if (null == instance) {
            instance = new FlinkManager();
        }
        return instance;
    }

    public Properties loadProperties(String configFile) throws ConfigurationException, IOException {
        InputStream kStreamsConfig = ConfigurationManager.getInstance().getResourceAsStream(configFile);
        Properties streamsConfiguration = new Properties();
        streamsConfiguration.load(kStreamsConfig);
        return  streamsConfiguration;
    }
}
