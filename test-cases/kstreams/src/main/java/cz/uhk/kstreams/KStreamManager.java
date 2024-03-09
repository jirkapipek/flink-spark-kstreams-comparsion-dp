package cz.uhk.kstreams;

import cz.uhk.configuration.ConfigurationManager;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Log4j2
public class KStreamManager {

    private static final String KSTREAMS_CONFIG_FILE = "kStreams.properties";

    /**
     * static singleton instance of KafkaManager.
     */
    private static volatile KStreamManager instance;

    /**
     * Private constructor for singleton instance of KafkaManager.
     */
    public KStreamManager() {
    }

    /**
     * Return a singleton instance of KafkaManager.
     */
    public static synchronized KStreamManager getInstance() {
        if (null == instance) {
            instance = new KStreamManager();
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
