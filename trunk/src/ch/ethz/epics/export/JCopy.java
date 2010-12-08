package ch.ethz.epics.export;

import java.nio.channels.*;
import java.io.*;
/**
 * 
 * @author see http://www.rgagnon.com/javadetails/java-0064.html
 *
 */
public class JCopy {
  public static void main(String args[]){
    try {
      JCopy j = new JCopy();
      j.copyFile(new File(args[0]),new File(args[1]));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
 }

  public static void copyFile(File in, File out) throws Exception {
     FileChannel sourceChannel = new
          FileInputStream(in).getChannel();
     FileChannel destinationChannel = new
          FileOutputStream(out).getChannel();
     sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
     // or
     //  destinationChannel.transferFrom
     //       (sourceChannel, 0, sourceChannel.size());
     sourceChannel.close();
     destinationChannel.close();
     }
}