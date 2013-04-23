package fr.eolya.simplepipeline.stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.lang.StringEscapeUtils;

import fr.eolya.simplepipeline.document.Doc;
import fr.eolya.utils.Utils;

/*
 * Configuration snippet sample :
 * 
 *	<stage position="0" classname="fr.eolya.simplepipeline.stage.BpiDb">
 *		<param name="jdbcdriver">com.mysql.jdbc.Driver</param>
 *		<param name="jdbcurl">jdbc:mysql://$DBHOST$:$DBPORT$/$DBNAME$?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true</param>
 *
 *		<param name="jdbcdriver">org.postgresql.Driver</param>
 *		<param name="jdbcurl">jdbc:postgresql://$DBHOST$:$DBPORT$/$DBNAME$</param>
 *
 *		<param name="dbhost">localhost</param>
 *		<param name="dbport">3306</param>
 *		<param name="dbname">crawler</param>
 *		<param name="dbuser">crawler</param>
 *		<param name="dbpassword">crawler</param>
 *
 *		<param name="source">source_tags</param>
 *		<param name="sourcecapture">bpi_theme:"([^"]*)"</param>
 *		<param name="groupcapture">1</param>
 *
 *		<param name="dbquery">select label from theme where reference = '$CRIT$'</param>
 *		<param name="target">bpi_theme</param>
 *
 *		<param name="dbquery">select label, parent from theme where reference = '$CRIT$'</param>
 *		<param name="target">bpi_theme, bpi_parent</param>
 *
 *		<param name="dbquery">select label,parent from theme where reference = '$CRIT$'</param>
 *		<param name="hierarchical">parent</param>
 *		<param name="hierarchical_direction">parent</param>
 *		<param name="hierarchical_sep"> &gt; </param>
 *		<param name="target">bpi_theme</param>
 *
 *	</stage>
 */

public class BpiDb extends Stage {

	private Connection con = null;
	private String dbQuery;
	private String sourceElement = null;
	private String targetElement = null;
	private String sourceCapture = null;
	private String groupCapture = null;

	private String hierarchical = null;
	private String hierarchicalDirection = null;
	private String hierarchicalSep = null;

	/**
	 * Perform initialization.
	 */
	public void initialize() {
		super.initialize();

		String jdbcDriver = props.getProperty("jdbcdriver");
		String jdbcUrl = props.getProperty("jdbcurl");
		String dbHost = props.getProperty("dbhost");
		String dbPort = props.getProperty("dbport");
		String dbName = props.getProperty("dbname");
		String dbUser = props.getProperty("dbuser");
		String dbPassword = props.getProperty("dbpassword");

		if (logger!=null) logger.log("    BpiDb - DB connection = " + dbHost + " " + dbPort + " " + dbName + " " + dbUser + " " + dbPassword);

		try {
			Class.forName(jdbcDriver);
			String url = getJdbcUrlConnection(dbHost, dbPort, dbName, jdbcUrl);
			con = DriverManager.getConnection(url, dbUser, dbPassword);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		dbQuery = props.getProperty("dbquery", "");
		sourceElement = props.getProperty("source", "");
		sourceCapture = props.getProperty("sourcecapture", "");
		groupCapture = props.getProperty("groupcapture", "");
		targetElement = props.getProperty("target", "");

		hierarchical = props.getProperty("hierarchical", "");
		hierarchicalDirection = props.getProperty("hierarchical_direction", "");
		hierarchicalSep = props.getProperty("hierarchical_sep", "");

	}

	@Override
	public void processDoc(Doc doc) throws Exception {

		// Check onaction
		if (!doProcess(doc)) {
			if (nextStage != null)
				nextStage.processDoc(doc);	
			return;
		}

		java.util.Date startTime = new java.util.Date();

		if (logger!=null) logger.log("    BpiDb");

		// Input
		String sourceValue = "";
		String targetValue = "";

		if (sourceElement != null && !"".equals(sourceElement))
			sourceValue = doc.getElementText("//" + sourceElement);

		int group = 1;

		if (groupCapture!=null && !"".equals(groupCapture))
			group = Integer.parseInt(groupCapture);

		String capturedValue = Utils.regExpExtract(sourceValue, sourceCapture, group);
		if (capturedValue!=null) {
			sourceValue = capturedValue;
			if (logger!=null) logger.log("    bpidb - match");
		}
		else {
			sourceValue = "";
			if (logger!=null) logger.log("    bpidb - no match");
			if (nextStage != null) {
				nextStage.processDoc(doc);
			}		
		}

		if (hierarchical==null || "".equals(hierarchical)) {
			String sqlStatement = dbQuery;
			sqlStatement = sqlStatement.replace("$CRIT$", sourceValue);
			if (logger!=null) logger.log("    bpidb - " + sqlStatement);

			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery(sqlStatement);
			if (rs.next()) {
				String[] aTargetElements = targetElement.split(",");
				for (int i=0; i<aTargetElements.length; i++) {
					targetValue = rs.getString(i+1);
					//targetValue = StringEscapeUtils.escapeHtml(targetValue);
					doc.addElement("/job", aTargetElements[i].trim(), targetValue);
				}
			}
			stmt.close();
		}
		else {
			targetValue = "";
			String value = null;
			String key = sourceValue;
			//for (int i=0; i<aHierarchical.length; i++) {
			while (!"".equals(key)) {
				String sqlStatement = dbQuery;
				sqlStatement = sqlStatement.replace("$CRIT$", key);
				if (logger!=null) logger.log("    bpidb - " + sqlStatement);

				Statement stmt = con.createStatement();
				ResultSet rs = stmt.executeQuery(sqlStatement);
				if (rs.next()) {
					value = rs.getString(1);
					key = rs.getString(2);
					if (key==null) key = "";
					if ("parent".equals(hierarchicalDirection)) {
						if (!"".equals(targetValue))
							targetValue = hierarchicalSep + targetValue;	
						targetValue = value + targetValue;
					}
					else {
						if (!"".equals(targetValue))
							targetValue += hierarchicalSep;
						targetValue += value;
					}
				}
				stmt.close();
			}	
			//targetValue = StringEscapeUtils.escapeHtml(targetValue);
			doc.addElement("/job", targetElement, targetValue);
		}

		java.util.Date endTime = new java.util.Date();
		processingTime += (endTime.getTime() - startTime.getTime());

		if (nextStage != null) {
			nextStage.processDoc(doc);
		}		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	static private String getJdbcUrlConnection(String dbHost, String dbPort, String dbName, String url) {
		return url.replace("$DBHOST$", dbHost).replace("$DBPORT$", dbPort).replace("$DBNAME$", dbName);
	}
}
