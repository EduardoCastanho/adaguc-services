package nl.knmi.adaguc.services.esgfsearch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Calendar;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.adaguc.tools.Debug;

public class DiskCache  {
 
  /**
   * Returns the stored message, null if not available, null if too old
   * @param diskCacheLocation
   * @param uniqueId
   * @param mustbeYoungerThanNSeconds
   * @return
   */
  public static String get( String diskCacheLocation,String uniqueId, int mustbeYoungerThanNSeconds) {
    uniqueId = uniqueId.replaceAll("\\?", "_");
    uniqueId = uniqueId.replaceAll("&", "_");
    uniqueId = uniqueId.replaceAll(":", "_");
    uniqueId = uniqueId.replaceAll("/", "_");
    uniqueId = uniqueId.replaceAll("=", "_");
   
    
    //Debug.println("Getting from diskcache: "+uniqueId);

    try {
      
      if(mustbeYoungerThanNSeconds!=0){
        Path fileCacheId = new File(diskCacheLocation+uniqueId).toPath();
        
        
        BasicFileAttributes attributes = Files.readAttributes(fileCacheId, BasicFileAttributes.class);
        FileTime creationTime = attributes.creationTime();
        long createdHowManySecondsAgo = ( Calendar.getInstance().getTimeInMillis()-creationTime.toMillis())/1000;
        //DebugConsole.println("Created:"+createdHowManySecondsAgo);
        if(createdHowManySecondsAgo>mustbeYoungerThanNSeconds)
        {
//          Debug.println("Ignoring "+uniqueId+"Because too old.");
          Tools.rm(diskCacheLocation+"/"+uniqueId);
          return null;
        }
        //else{
//          Debug.println(fileCacheId.toString()+"("+createdHowManySecondsAgo+"<"+mustbeYoungerThanNSeconds+")");
//        }
      }
//      Debug.println("Found "+diskCacheLocation+"/"+uniqueId);
      return Tools.readFile(diskCacheLocation+"/"+uniqueId);
    } catch (IOException e) {
    }
    return null;
  }
  
  /**
   * Store a string in the diskcache system identified with an id
   * @param data The data to store
   * @param identifier The identifier of this string
   */
  public static void set(String diskCacheLocation ,String identifier, String data){
    identifier = identifier.replaceAll("\\?", "_");
    identifier = identifier.replaceAll("&", "_");
    identifier = identifier.replaceAll(":", "_");
    identifier = identifier.replaceAll("/", "_");
    identifier = identifier.replaceAll("=", "_");
  
    //Debug.println("Storing to diskcache: "+identifier);
    try{
       try {
        Tools.mksubdirs(diskCacheLocation);
        Tools.writeFile(diskCacheLocation+"/"+identifier, data);
      } catch (IOException e) {

        e.printStackTrace();
      }
    }catch(Exception e){
      Debug.errprintln("Unable to write to cachelocation "+diskCacheLocation+" with identifier "+identifier);
      
    }
  }
  

}
