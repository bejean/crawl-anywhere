package fr.eolya.utils;

import java.net.URL;
import java.util.Objects;

import fr.eolya.utils.XMLConfig;

public final class ConfigHelper {

    private ConfigHelper() {
    }

    /**
     * Checks if the robots.txt file should be bypassed by the crawler.<br>
     * The base setting is the bypass_robots_file param, with overrides nested in a special element:
     * <p>
     * <pre>
     * &lt;bypassRobotsFile&gt;
     *   &lt;param name=&quot;overriden.host.com&quot;&gt;1&lt;/param&gt;
     * &lt;/bypassRobotsFile&gt;
     * </pre>
     *
     * @param config
     * @param url    optional parameter. If null, no overrides are applied
     * @return
     */
    public static boolean isBypassRobotsFile(XMLConfig config, URL url) {
        Objects.requireNonNull(config, "Parameter config is missing");

        if (url != null) {
            String perHostBypassValue = config.getProperty("/crawler/bypassRobotsFile/param[@name='" + url.getHost() + "']");
            return "1".equals(perHostBypassValue);
        }

        String globalBypassValue = config.getProperty("/crawler/param[@name='bypass_robots_file']", "0");
        return "1".equals(globalBypassValue);
    }
}
