package fr.eolya.crawler;

import fr.eolya.utils.Logger;
import fr.eolya.utils.XMLConfig;

public abstract class Processor {
	
	protected ICrawlerController crawlerController;
	protected Logger logger;
	protected XMLConfig config;

	public Processor(XMLConfig config, Logger logger, ICrawlerController crawlerController) {
		this.config = config;
		this.logger = logger;
		this.crawlerController = crawlerController;
	}
}
