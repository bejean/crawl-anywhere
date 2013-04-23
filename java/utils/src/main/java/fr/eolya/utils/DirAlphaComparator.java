package fr.eolya.utils;

import java.io.File;
import java.util.Comparator;

//////////////////////////////////////////////////DirAlphaComparator
//To sort directories before files, then alphabetically.
class DirAlphaComparator implements Comparator<File> {

 // Comparator interface requires defining compare method.
 public int compare(File filea, File fileb) {
     //... Sort directories before files,
     //    otherwise alphabetical ignoring case.
     if (filea.isDirectory() && !fileb.isDirectory()) {
         return -1;

     } else if (!filea.isDirectory() && fileb.isDirectory()) {
         return 1;

     } else {
         return filea.getName().compareToIgnoreCase(fileb.getName());
     }
 }
}
