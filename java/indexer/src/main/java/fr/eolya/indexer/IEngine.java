package fr.eolya.indexer;

import java.util.Collection;

public interface IEngine {
    public boolean connect();
    public void close();
    public String getUrl();
    public int addDocuments(Collection<InputDocument> docs);
    public void terminate(boolean optimize);
    public boolean resetIndex();
    public boolean deleteByQuery(String query);
    public boolean optimize(boolean force);
    public boolean commit(boolean force);
    public boolean ping();
    public void setOutputStackTrace(boolean outputStackTrace);
    public String getLastExceptionCauseName();   
}
