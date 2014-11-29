package fr.eolya.crawlerws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrawlerWsSayingTestFilteringRules {
    
	private int status;
	private String mode;
	private int lastErrorCode;
	private String lastErrorMessage;

    public CrawlerWsSayingTestFilteringRules() {
        // Jackson deserialization
    }

    public CrawlerWsSayingTestFilteringRules(int status, String mode, int lastErrorCode, String lastErrorMessage) {
        this.status = status;
        this.mode = mode;
        this.lastErrorCode = lastErrorCode;
        this.lastErrorMessage = lastErrorMessage;
    }

	@JsonProperty
    public int getLastErrorCode() {
		return lastErrorCode;
	}

	@JsonProperty
	public String getLastErrorMessage() {
		return lastErrorMessage;
	}
	
	@JsonProperty
	public int getStatus() {
		return status;
	}

    @JsonProperty
	public String getMode() {
		return mode;
	}
}