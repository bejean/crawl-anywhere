package fr.eolya.crawler.connectors.web.robots;

import java.net.*;

import org.osjava.norbert.NoRobotClient;
import org.osjava.norbert.NoRobotException;

public class Robots implements iRobots {

	protected NoRobotClient robot;

	public Robots(URL srcUrl, String robotsTxtContent, String userAgent, boolean wildcardsAllowed) 
	{
		robot = new NoRobotClient(userAgent);
		robot.setWildcardsAllowed(wildcardsAllowed);
		try {
			if (srcUrl!=null && robotsTxtContent==null) {
				robot.parse(srcUrl);
			}
			else {
				if (robotsTxtContent==null || "".equals(robotsTxtContent)) {
					robot = null;
					return;
				}
				robot.parseText(srcUrl, robotsTxtContent);
			}
		} catch (NoRobotException e) {
			robot = null;
			System.out.println(e.getMessage());
			//e.printStackTrace();
		}
	}

	public boolean isUrlAllowed(URL url)
	{
		if (robot!=null)
			return robot.isUrlAllowed(url);
		else
			return true;
	}
	
//    public static void main(String[] args) {
//        String home = "http://www.homeaway.co.nz/";
//        String url = "http://www.homeaway.co.nz/search/refined/thailand/pattaya/region:30905/Suitability:wheelchair+accessible?view=g";
//   
//        Robots robots = null;
//        try {
//            robots = new Robots(new URL(home), null, "CaBot", true);
//            if (robots.isUrlAllowed(new URL(url))) 
//                System.out.println("Allowed");
//            else
//                System.out.println("Disallowed");
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//            robots = null;
//        } 
//    }
}

