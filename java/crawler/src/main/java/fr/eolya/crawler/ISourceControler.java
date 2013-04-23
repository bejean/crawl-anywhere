package fr.eolya.crawler;

public interface ISourceControler {
	
	public void incrementProcessedItemCount(int count);
	public long getProcessedItemCount();
	public boolean stopRun();
}
