package fr.eolya.indexer;

import java.util.Date;

import fr.eolya.utils.Logger;

public abstract class EngineAbstract {
    
    protected Logger logger;
    protected boolean outputStackTrace = false;
    protected String lastExceptionCauseName = "";
    
    protected void log(String msg) {
        if (logger!=null) logger.log(msg);
    }
    
    public void terminate(boolean optimize) {
        commit(true);
        if (optimize) optimize(true);
    }
    
    protected abstract void internalOptimize();
    protected abstract void internalCommit();
    protected abstract void internalDeleteByQuery(String query);
    
    public void setOutputStackTrace(boolean outputStackTrace) {
        this.outputStackTrace = outputStackTrace;
    }
    
    public String getLastExceptionCauseName() {
        return lastExceptionCauseName;
    }
    
    
    protected int optimizeDocsCount = 0;
    protected int commitDocsCount = 0;
    protected int optimizeEach = 0;
    protected int commitWithin = 0;
    protected int commitEach = 0;
    protected long lastCommitTimestamp = 0;
    
    public boolean commit(boolean force) {
        //if (server==null) return false;
        if (commitDocsCount==0) return true;
        try {       
            Date now = new Date();
            if ( (optimizeEach>0) && (optimizeDocsCount >= optimizeEach)) {
                log("Commiting index");
                internalCommit();
                log("Optimizing index");
                internalOptimize();
                optimizeDocsCount = 0;
                commitDocsCount = 0;
                lastCommitTimestamp = now.getTime();
            } else {
                long lastCommitDelay = (now.getTime() - lastCommitTimestamp) / 1000;
                if ((commitDocsCount >= commitEach && commitEach > 0) || force || (lastCommitDelay > 300  && commitEach > 0)) {
                    log("Commiting index");
                    internalCommit();
                    commitDocsCount = 0;
                    lastCommitTimestamp = now.getTime();
                }
            }
            return true;
        }
        catch (Exception e) {
            if (outputStackTrace) e.printStackTrace();
            return false;
        }
    }
    
    public boolean optimize(boolean force) {
        //if (server==null) return false;
        if (optimizeDocsCount==0) return true;
        try {       
            if ((optimizeEach>0) && (optimizeDocsCount >= optimizeEach || force)) {
                log("Optimizing index");
                internalCommit();
                internalOptimize();
                optimizeDocsCount = 0;
                commitDocsCount = 0;
            }
            return true;
        }
        catch (Exception e) {
            if (outputStackTrace) e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteByQuery(String query) {
        //if (server==null) return false;
        try {
            internalDeleteByQuery(query);
            commitDocsCount++;
            optimizeDocsCount++;
        } catch (Exception e) {
            if (outputStackTrace) e.printStackTrace();
            return false;
        }
        //commit(true);
        return true;
    }
    
    public boolean resetIndex() {
        //if (server==null) return false;
        try {
            internalDeleteByQuery("");
            internalCommit();
            internalOptimize();
            return true;
        } catch (Exception e) {
            log("failed to reset index");
            if (outputStackTrace) e.printStackTrace();
            return false;
        }
    }
    
}
