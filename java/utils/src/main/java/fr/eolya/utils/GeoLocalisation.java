package fr.eolya.utils;

import fr.eolya.utils.http.HttpLoader;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Properties;

// TODO : junit

public class GeoLocalisation {

	private final String ipCheckFrUrl = "http://www.ipcheck.fr/api.php";
	private String host = "";
	private String knowedIP = "";
	private String geo_ip = "";
	private String country_code ="";
	private String country_name = "";
	private String area = "";
	private String city = "";
	private String latitude = "";
	private String longitude = "";
	
	private HttpLoader httpLoader = null;

	public GeoLocalisation (String host, String knowedIP, Properties prop) throws URISyntaxException
	{
		this.host = host;
		this.knowedIP = knowedIP;
		httpLoader = new HttpLoader();
		if (prop!=null && !"".equals(prop.getProperty("proxy.host", "")) && !"".equals(prop.getProperty("proxy.port", ""))) {
			httpLoader.setProxyHost(prop.getProperty("proxy.host", ""));
			httpLoader.setProxyPort(prop.getProperty("proxy.port", ""));
			if (!"".equals(prop.getProperty("proxy.exclude", ""))) httpLoader.setProxyExclude(prop.getProperty("proxy.exclude", ""));
			if (!"".equals(prop.getProperty("proxy.username", "")) && !"".equals(prop.getProperty("proxy.password", ""))) {
				httpLoader.setProxyUserName(prop.getProperty("proxy.username", ""));
				httpLoader.setProxyPassword(prop.getProperty("proxy.password", ""));
			}
		}
	}

	public boolean resolve(String method)
	{
		String newIp = "";

		// 1. get the ip
		try
		{
			InetAddress inetAddress = InetAddress.getByName(this.host);
			newIp = inetAddress.getHostAddress();
		}
		catch (UnknownHostException e)
		{
			//e.printStackTrace();
			return false;
		}

		// 2. if ip changed, get geo info
		if (!newIp.equals(this.knowedIP))
		{
			String url = "";
			try
			{
				if ("ipcheck".equals(method)) {
					// get geo code pays
					url = ipCheckFrUrl + "?objet=pays&ip=" + newIp + "&choix=2";
					this.country_code = getPage (url);

					if (!this.country_code.equals(""))
					{
						// get geo code nom
						url = ipCheckFrUrl + "?objet=pays&ip=" + newIp + "&choix=1";
						this.country_name = getPage (url);

						// get geo region
						url = ipCheckFrUrl + "?objet=region&ip=" + newIp;
						this.area = getPage (url);

						// get geo ville
						url = ipCheckFrUrl + "?objet=ville&ip=" + newIp;
						this.city = getPage (url);

						// get geo latitude
						url = ipCheckFrUrl + "?objet=latitude&ip=" + newIp;
						this.latitude = getPage (url);

						// get geo longitude
						url = ipCheckFrUrl + "?objet=longitude&ip=" + newIp;
						this.longitude = getPage (url);
					}
				}
				if ("geoiptool".equals(method)) {
					url = "http://www.geoiptool.com/fr/?IP=" + newIp;
					String page = getPage (url);

					int start = 0;
					int end = 0;
					start = page.indexOf("<span class=\"arial\">Pays:</span>");
					start = page.indexOf("target=\"_blank\">", start);
					end = page.indexOf("<", start);
					this.country_name = page.substring(start+"target=\"_blank\">".length(), end).trim();

					start = page.indexOf("<span class=\"arial\">Code de pays:</span>");
					start = page.indexOf("class=\"arial_bold\">", start);
					end = page.indexOf("<", start);
					this.country_code = page.substring(start+"class=\"arial_bold\">".length(), end).trim();

					start = page.indexOf("<span class=\"arial\">Ville:</span>");
					start = page.indexOf("class=\"arial_bold\">", start);
					end = page.indexOf("<", start);
					this.city = page.substring(start+"class=\"arial_bold\">".length(), end).trim();

					start = page.indexOf("<span class=\"arial\">R&eacute;gion:</span>");
					start = page.indexOf("target=\"_blank\">", start);
					end = page.indexOf("<", start);
					this.area = page.substring(start+"target=\"_blank\">".length(), end).trim();

					start = page.indexOf("<span class=\"arial\">Latitude:</span>");
					start = page.indexOf("class=\"arial_bold\">", start);
					end = page.indexOf("<", start);
					this.latitude = page.substring(start+"class=\"arial_bold\">".length(), end).trim();

					start = page.indexOf("<span class=\"arial\">Longitude:</span>");
					start = page.indexOf("class=\"arial_bold\">", start);
					end = page.indexOf("<", start);
					this.longitude = page.substring(start+"class=\"arial_bold\">".length(), end).trim();

					start = 0;
				}
				if ("geoplugin".equals(method)) {
					url = "http://www.geoplugin.net/xml.gp?ip=" + newIp;
					String page = getPage (url);

					XMLConfig config = new XMLConfig();
					config.loadString(page);

					this.country_name = config.getProperty("/geoPlugin/geoplugin_countryName");
					this.area = config.getProperty("/geoPlugin/geoplugin_regionName");
					this.city = config.getProperty("/geoPlugin/geoplugin_city");
					this.latitude = config.getProperty("/geoPlugin/geoplugin_latitude");
					this.longitude = config.getProperty("/geoPlugin/geoplugin_longitude");
				}
				//http://www.geody.com/geoip.php
				//http://www.geody.com/geoip.php?ip=$(curl -s icanhazip.com)" | sed '/^IP:/!d;s/<[^>][^>]*>//g'
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
			this.geo_ip = newIp;
		}

		return true;
	}

	public boolean hasChanged()
	{
		if (!geo_ip.equals("") && !geo_ip.equals(knowedIP))
			return true;
		else
			return false;
	}

	private String getPage (String url)
	{
		try
		{
			httpLoader.open(url);
			if(httpLoader.getResponseStatusCode() < 300)
			{
				return httpLoader.load();
			}
			return "";
		}
		catch (Exception e)
		{
			return "";
		}
	}

	public String getIp()
	{
		return geo_ip;
	}

	public String getCountryCode()
	{
		return country_code;
	}

	public String getCountryName()
	{
		return country_name;
	}

	public String getArea()
	{
		return area;
	}

	public String getCity()
	{
		return city;
	}

	public String getLatitude()
	{
		return latitude;
	}

	public String getLongitude()
	{
		return longitude;
	}

}
