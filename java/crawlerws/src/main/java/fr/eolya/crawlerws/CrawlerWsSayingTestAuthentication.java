package fr.eolya.crawlerws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrawlerWsSayingTestAuthentication {
    
	private int status;
	private String page;
	private int lastErrorCode;
	private String lastErrorMessage;

    public CrawlerWsSayingTestAuthentication() {
        // Jackson deserialization
    }

    public CrawlerWsSayingTestAuthentication(int status, String page, int lastErrorCode, String lastErrorMessage) {
        this.status = status;
        this.page = page;
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
	public String getPage() {
		return page;
	}
}