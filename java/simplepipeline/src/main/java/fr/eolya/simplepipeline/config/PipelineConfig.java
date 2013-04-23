package fr.eolya.simplepipeline.config;

import java.util.Properties;

import fr.eolya.utils.XMLConfig;

/**
 * 	<pipeline>
 * 		<connector classname="">
 * 			<param name="">
 * 			</param>
 * 		</connector>
 * 		<stages>
 * 			<stage position="0" enabled="yes" classname="">
 * 				<param name="">
 * 				</param>
 * 			</stage>
 * 			<stage position="1" classname="">
 * 				<param name="">
 * 				</param>
 * 			</stage>
 * 		</stages>
 * 	</pipeline>
 * 
 *  Where a stage is enabled if "enabled" attribute = "" or "1" or "y" ou "yes" or "on" or is missing
 */

public class PipelineConfig extends XMLConfig {
	
	private int stageCount = -1;

	public PipelineConfig() {}
	
	public Properties getConnectorProperties() {
		return getProperties("/pipeline/connector");
	}
	
	public String getConnectorClassName() {
		return getPropertyAttribute("/pipeline/connector", "classname");
	}
	
	public Properties getStageProperties(int index) {
		return getProperties("/pipeline/stages/stage[" + Integer.toString(index+1) +"]");
	}
	
	public String getStageClassName(int index) {
		return getPropertyAttribute("/pipeline/stages/stage[" + Integer.toString(index+1) +"]", "classname");
	}
	
	public boolean isStageEnabled(int index) {
		String enabled = getPropertyAttribute("/pipeline/stages/stage[" + Integer.toString(index+1) +"]", "enabled");
		if (enabled==null) return true;
		enabled = enabled.toLowerCase().trim();
		if (isEnabled(enabled))
			return true;
		else
			return false;
	}
	
	public int getStageCount() {
		if (stageCount==-1)
			stageCount = getElementCount("/pipeline/stages/stage");
		return stageCount;
	}
	
	public static boolean isEnabled(String value) {
		return (value==null || "".equals(value) || "1".equals(value)  || "y".equals(value) || "yes".equals(value) || "on".equals(value));
	}

}
