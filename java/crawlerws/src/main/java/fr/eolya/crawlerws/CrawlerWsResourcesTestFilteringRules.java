package fr.eolya.crawlerws;

import com.codahale.metrics.annotation.Timed;

import fr.eolya.utils.CrawlerUtilsCommon;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;


@Path("/testfilteringrules")
@Produces(MediaType.APPLICATION_JSON)
public class CrawlerWsResourcesTestFilteringRules extends CrawlerWsResources {
	
    public CrawlerWsResourcesTestFilteringRules(CrawlerWsConfiguration conf) {
		super(conf);
	}

	@GET
    @Timed
    public CrawlerWsSayingTestFilteringRules doGet(@QueryParam("page") String page, 
    		@QueryParam("rules") String rules) {
		
        if (!"".equals(page)) {
            return new CrawlerWsSayingTestFilteringRules(1, CrawlerUtilsCommon.getUrlMode(page, rules, "a"), 0, "");
        }
    	return new CrawlerWsSayingTestFilteringRules(0, "", 10, "page missing");
	}
}