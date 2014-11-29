package fr.eolya.crawlerws;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class CrawlerWsApplication extends Application<CrawlerWsConfiguration> {
	
	public static void main(String[] args) throws Exception {
		new CrawlerWsApplication().run(args);
	}

	@Override
	public String getName() {
		return "crawlerws";
	}

	@Override
	public void initialize(Bootstrap<CrawlerWsConfiguration> bootstrap) {
		// nothing to do yet
	}

	@Override
	public void run(CrawlerWsConfiguration configuration,
			Environment environment) {

		final CrawlerWsResourcesTestAuthentication resourceTestAuthentication = new CrawlerWsResourcesTestAuthentication(
				configuration
				);
		environment.jersey().register(resourceTestAuthentication);
		
		final CrawlerWsResourcesTestFilteringRules resourceTestFilteringRules = new CrawlerWsResourcesTestFilteringRules(
				configuration
				);
		environment.jersey().register(resourceTestFilteringRules);

		final CrawlerWsResourcesTestCleaning resourceTestCleaning = new CrawlerWsResourcesTestCleaning(
				configuration
				);
		environment.jersey().register(resourceTestCleaning);
	}

}