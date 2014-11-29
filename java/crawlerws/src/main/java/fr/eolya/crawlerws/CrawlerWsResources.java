package fr.eolya.crawlerws;

public class CrawlerWsResources {
	
	protected CrawlerWsConfiguration conf;

    public CrawlerWsResources(CrawlerWsConfiguration conf) {
        this.conf = conf;
    }
    
    protected CrawlerWsSayingTestAuthentication getResponse() {
    	return new CrawlerWsSayingTestAuthentication();
    }
}