package fr.eolya.crawlerws;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class CrawlerWsConfiguration extends Configuration {
	
    @NotEmpty
    private String userAgent;
    private String proxyHost;
    private String proxyPort;
    private String proxyExclude;
    private String proxyUserName;
    private String proxyPassword;

    @JsonProperty
    public String getUserAgent() {
        return userAgent;
    }
 
    @JsonProperty
    public String getProxyHost() {
        return proxyHost;
    }

    @JsonProperty
    public String getProxyPort() {
        return proxyPort;
    }

    @JsonProperty
    public String getProxyExclude() {
        return proxyExclude;
    }

    @JsonProperty
    public String getProxyUserName() {
        return proxyUserName;
    }

    @JsonProperty
    public String getProxyPassword() {
        return proxyPassword;
    }
}