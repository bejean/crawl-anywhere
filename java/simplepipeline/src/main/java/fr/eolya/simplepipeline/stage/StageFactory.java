package fr.eolya.simplepipeline.stage;

public class StageFactory {

	@SuppressWarnings("rawtypes")
	public static Stage getStage(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        if ("fr.eolya.simplepipeline.stage.SolrIndexerQueueWriter".equals(className)) className = "fr.eolya.simplepipeline.stage.IndexerQueueWriter";
		Class classRef = Class.forName(className);
		return (Stage) classRef.newInstance();
	}
	
}
