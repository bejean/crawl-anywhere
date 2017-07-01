package fr.eolya.utils;

import java.io.IOException;
import java.net.URL;

import fr.eolya.utils.ConfigHelper;
import org.junit.Assert;
import org.junit.Test;

import fr.eolya.utils.XMLConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigHelperTest {

    @Test
    public void isBypassRobotsFile_noOverride_false() throws IOException {
        // given
        XMLConfig config = new XMLConfig();
        config.loadFile("src/test/resources/bypass-robots-crawler-config.xml");

        // when
        boolean bypass = ConfigHelper.isBypassRobotsFile(config, new URL("http://any-url.com"));

        // then
        assertFalse(bypass);
    }

    @Test
    public void isBypassRobotsFile_override_true() throws IOException {
        // given
        XMLConfig config = new XMLConfig();
        config.loadFile("src/test/resources/bypass-robots-crawler-config.xml");

        // when
        boolean bypass = ConfigHelper.isBypassRobotsFile(config, new URL("http://bypass-robots-for-this.com/context"));

        // then
        assertTrue(bypass);
    }
}
