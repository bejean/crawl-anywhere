package fr.eolya.simplepipeline.connector;

import fr.eolya.simplepipeline.IStateController;

public interface Connector {

	public boolean start() throws Exception;
	
	/**
	 * Run this Connector.
	 */
	public void execute() throws Exception;
	
	/**
	 * Configure this connector.
	 */
	public void setStateController(IStateController sc);
	
}
