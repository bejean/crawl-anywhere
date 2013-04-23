package fr.eolya.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

public class Logger {

	private String fileName;
	private int rotatesize = 100000000;

	public Logger(String fileName) {
		this.fileName = fileName;
	}
	
	public int getRotatesize() {
		return rotatesize;
	}

	public void setRotatesize(int rotatesize) {
		this.rotatesize = rotatesize;
	}

	public synchronized void log(String message) {
		Date d = new Date();

		File f = new File(fileName);
		if (f != null && f.length() > 100000000) {
			File f_dest = new File(fileName + "." + String.valueOf(d.getTime()));
			f.renameTo(f_dest);
		}
		message = d.toString() + " - " + message + "\n";

		if (fileName == null) {
			System.out.println(message);
		}
		else {
			try {
				BufferedWriter writer = null;
				FileWriter fw = new FileWriter(fileName, true);
				writer = new BufferedWriter(fw);
				writer.write(message);
				writer.close();
				fw.close();
			} catch (Exception e) {
			}
		}
	}

	public void logStackTrace(Exception e) {
		logStackTrace(e, true);
	}

	public synchronized void logStackTrace(Exception e, boolean toConsole) {
		try {
			StackTraceElement[] element = e.getStackTrace();

			log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			log(">>>> Error = " + e.getMessage());
			log("           = " + element[element.length - 1].toString());
			for (int i = 0; i < element.length; i++) {
				log("                       " + element[i].toString());
			}
			log(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		} catch (Exception e2) {
		}

		if (toConsole) {
			Date d = new Date();
			System.out.println(d.toString());
			e.printStackTrace();
		}
	}

}
