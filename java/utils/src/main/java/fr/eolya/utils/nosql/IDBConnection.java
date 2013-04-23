package fr.eolya.utils.nosql;

public interface IDBConnection {
	public String getType();
	public void close();
}
