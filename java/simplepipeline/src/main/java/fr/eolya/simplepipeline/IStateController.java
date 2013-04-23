package fr.eolya.simplepipeline;

import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.utils.Logger;

public interface IStateController {

	public PipelineConfig getConfig();
	public boolean getVerbose();
	public boolean getOnce();
	public boolean stopRequested();
	public Logger getLogger();
}
