package fr.eolya.crawlerws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CrawlerWsSayingTestCleaning {
    
	private int status;
	private String page_0;
	private String title_0; 
	private String page_1;
	private String title_1; 
	private String page_2;
	private String title_2; 
	private String page_3;
	private String title_3; 
	private String page_4;
	private String title_4; 
	private int lastErrorCode;
	private String lastErrorMessage;

    public CrawlerWsSayingTestCleaning() {
        // Jackson deserialization
    }

    public CrawlerWsSayingTestCleaning(int status, 
    		String page_0,  String title_0, 
    		String page_1,  String title_1, 
    		String page_2,  String title_2, 
    		String page_3,  String title_3, 
    		String page_4,  String title_4, 
    		int lastErrorCode, String lastErrorMessage) {
        this.status = status;
        this.page_0 = page_0;
        this.title_0 = title_0;
        this.page_1 = page_1;
        this.title_1 = title_1;
        this.page_2 = page_2;
        this.title_2 = title_2;
        this.page_3 = page_3;
        this.title_3 = title_3;
        this.page_4 = page_4;
        this.title_4 = title_4;
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
	public String getPage0() {
		return page_0;
	}
    @JsonProperty
 	public String getTitle0() {
 		return title_0;
 	}
    @JsonProperty
	public String getPage1() {
		return page_1;
	}
    @JsonProperty
 	public String getTitle1() {
 		return title_1;
 	}
    @JsonProperty
	public String getPage2() {
		return page_2;
	}
    @JsonProperty
 	public String getTitle2() {
 		return title_2;
 	}
    @JsonProperty
	public String getPage3() {
		return page_3;
	}
    @JsonProperty
 	public String getTitle3() {
 		return title_3;
 	}
    @JsonProperty
	public String getPage4() {
		return page_4;
	}
    @JsonProperty
 	public String getTitle4() {
 		return title_4;
 	}
}