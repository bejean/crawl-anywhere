package fr.eolya.crawler.utils;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fr.eolya.utils.Utils;

public class CrawlerUtils {

	/**
	 * Return if a country is accepted
	 * 
	 * @param 
	 * @return
	 */	
	public static boolean isAcceptedCountry(String country, List<String> countryInclude, List<String> countryExclude) {
		if (country == null || "".equals(country))
			return true;

		country = country.toLowerCase();

		if (countryInclude != null && countryInclude.size() > 0 && !"".equals(countryInclude.get(0))) {
			if (countryInclude.contains(country))
				return true;
			return false;
		}

		if (countryExclude != null && countryExclude.size() > 0 && !"".equals(countryExclude.get(0))) {
			if (countryExclude.contains(country))
				return false;
			return true;
		}

		return true;
	}

	//public static boolean isQueueCacheEnabled(XMLConfig config) {
	//	return "1".equals(config.getProperty("/crawler/cache_enabled", "0"));
	//}


	public static String getSourceCrawlMode(int crawlMode, boolean reScan, boolean reset, boolean deeper, boolean resetFromCache) {

		/*
		 * crawl_mode
		 * 	0 = normal
		 * 	1 = rescan
		 * 	2 = reset
		 * 	3 = deeper
		 *  4 = reset from cache
		 *  5 = clear
		 */

		String tempMode = String.valueOf(crawlMode);

		boolean tempReScan = reScan;
		if (!tempReScan)
			tempReScan = (crawlMode==1);
		if (tempReScan)
			tempMode = "1";

		boolean tempReset = reset;
		if (!tempReset)
			tempReset = (crawlMode==2);
		if (tempReset)
			tempMode = "2";

		boolean tempDeeper = deeper;
		if (!tempDeeper)
			tempDeeper = (crawlMode==3);
		if (tempDeeper)
			tempMode = "3";

		boolean tempResetFromCache = resetFromCache;
		if (!tempResetFromCache)
			tempResetFromCache = (crawlMode==4);
		if (tempResetFromCache)
			tempMode = "4";

		return tempMode;
	}


	public static void executorShutdownAndWait(ThreadPoolExecutor executor, int sleepBefore, long timeout, TimeUnit unit) {
		if (sleepBefore>0) Utils.sleep(sleepBefore);
		executor.shutdown();
		try {
			executor.awaitTermination(timeout, unit);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
