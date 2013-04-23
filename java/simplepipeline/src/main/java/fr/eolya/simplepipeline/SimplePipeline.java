package fr.eolya.simplepipeline;

import fr.eolya.simplepipeline.config.PipelineConfig;
import fr.eolya.simplepipeline.connector.Connector;
import fr.eolya.simplepipeline.connector.ConnectorFactory;
import fr.eolya.utils.Logger;
import fr.eolya.utils.Utils;

import gnu.getopt.Getopt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SimplePipeline implements IStateController {

	private PipelineConfig config = null;
	private Logger logger = null;
	private boolean verbose = false;
	private boolean once = false;
	private String witnessFilesPath = null;

	public static void main(String[] args) {

		PipelineConfig config;

		if (args.length == 0) {
			usage();
			System.exit(-1);
		}

		Getopt g = new Getopt("Indexer", args, "p:ov");
		g.setOpterr(false);
		int c;

		boolean once = false;
		boolean verbose = false;

		String xmlConfigFileName = "";

		while ((c = g.getopt()) != -1) {
			switch (c) {
			case 'p':
				xmlConfigFileName = g.getOptarg();
				break;

			case 'o':
				once = true;
				break;

			case 'v':
				verbose = true;
				break;
			}
		}

		if ("".equals(xmlConfigFileName)) {
			System.out.println("Error: no configuration file specified");
			System.exit(-1);
		}
		xmlConfigFileName = Utils.getValidPropertyPath(xmlConfigFileName, null, "HOME");

		System.out.println("Configuration file = " + xmlConfigFileName);
		
		try {
			File configFile =new File(xmlConfigFileName);
			if (!configFile.exists()) {
				System.out.println("Error configuration file not found [" + xmlConfigFileName + "]");
				System.exit(-1);
			}
		} catch (Exception e) {
			// e.printStackTrace();
			System.out.println("Error while reading configuration file");
			e.printStackTrace();
			System.exit(-1);
		}

		config = new PipelineConfig();
		try {
			config.loadFile(xmlConfigFileName);
		} catch (IOException e) {
			// e.printStackTrace();
			System.out.println("Error while reading configuration file");
			e.printStackTrace();
			System.exit(-1);
		}

		String witnessFilesPath = config.getProperty("/pipeline/param[@name='witnessfilespath']");
		witnessFilesPath = Utils.getValidPropertyPath(witnessFilesPath, null, "HOME");
		if (witnessFilesPath == null || "".equals(witnessFilesPath)) {
			System.out.println("Error : missing witness_files_path propertie");
			System.exit(-1);
		}

		File filePid = new File(witnessFilesPath + "/pipeline.pid");
		if (filePid.exists()) {
			System.out.println("A pipeline instance is already running");
			System.exit(-1);
		}

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filePid.getAbsolutePath()));
			out.write(Utils.getProcessId());
			out.close();
		} catch (Exception e) {
			System.out.println("Error while creating file : " + filePid.getAbsolutePath());
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			SimplePipeline pipeline = new SimplePipeline();
			pipeline.run(config, once, verbose);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (filePid.exists()) {
				filePid.delete();
			}
		}
	}

	private static void usage() {
		System.out.println("Usage : java SimplePipeline -p <xml config file> [-o] [-v]");
		System.out.println("    -o : once");
		System.out.println("    -v : verbose");
	}

	public PipelineConfig getConfig() {
		return this.config;
	}

	public boolean getVerbose() {
		return this.verbose;
	}

	public boolean getOnce() {
		return this.once;
	}

	public Logger getLogger() {
		return this.logger;
	}
	
	public boolean stopRequested() {
		if (witnessFilesPath==null) {
			witnessFilesPath = config.getProperty("/pipeline/param[@name='witnessfilespath']");
			witnessFilesPath = Utils.getValidPropertyPath(witnessFilesPath, null, "HOME");
		}
		File filePid = new File(witnessFilesPath + "/pipeline.pid");
		File fileStop = new File(witnessFilesPath + "/pipeline.stop");
		return (fileStop.exists() || !filePid.exists());
	}

	public void run(PipelineConfig config, boolean once, boolean verbose) {

		if (config == null) {
			throw new IllegalStateException("setConfig() was not called");
		}
		this.config = config;
		this.once = once;
		this.verbose = verbose;

		try {
			String logFilesPath = config.getProperty("/pipeline/param[@name='logfilename']");
			logFilesPath = Utils.getValidPropertyPath(logFilesPath, null, "HOME");
			logger = new Logger(logFilesPath);
			//int onceModeTimeOut = Integer.parseInt(config.getProperty("/pipeline/param[@name='oncemodetimeout']", "300"));

			logger.log("=================================");
			logger.log("Pipeline starting");
			if (once)
				logger.log("    mode once");
			if (verbose)
				logger.log("    mode verbose");
			logger.log("=================================");
			logger.log("");

			Connector connector = ConnectorFactory.getConnector(config.getConnectorClassName());
			connector.setStateController(this);

			connector.start();
			connector.execute();


			//			boolean stopRequested = false;
			//			int waitingSince = 0;
			//			while (!stopRequested) {
			//				//connector.execute(stageList);
			//				connector.execute();
			//				
			//				stopRequested = this.stopRequested();
			//
			//				if (connector.getLastExecuteItemCount()>0) {
			//					waitingSince = 0;
			//				}
			//				else {
			//					if (waitingSince==0)
			//						waitingSince += 5000;
			//					else
			//						waitingSince += 15000;
			//					if (once && waitingSince > onceModeTimeOut * 1000)
			//						stopRequested = true;
			//				}
			//
			//				if (!stopRequested) {
			//					if (connector.getLastExecuteItemCount()>0) {
			//						Utils.sleep(5000);
			//					}
			//					else {
			//						Utils.sleep(15000);
			//					}
			//				}
			//			}


			// Wait for threads end
			//			int loopCount = 0;
			//			while (connector.isAlive()) {
			////				if (!stopRequested) {				
			////					stopRequested = fileStop.exists() || !filePid.exists();
			////					if (stopRequested) {
			////						sq.clear();
			////						ctc.stopThreads();
			////					}
			////				}
			//				if (loopCount==6) {
			//					logger.log("Waiting all crawls terminate !");
			//					loopCount=0;
			//				}
			//				loopCount++;
			//				Utils.sleep(5000);
			//			}

			//logger.log("Fin du traitement");
			//if (stageList!=null)
			//	stageList.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.log("Pipeline ending");
	}
}
