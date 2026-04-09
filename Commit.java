package gitlet;

import java.io.Serializable;
import java.util.Date; 
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  @author areeya
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private String message;
    private Date timestamp;

    // Something that keeps track of what files
    // this commit is tracking
    public HashMap<String, byte[]> trackedFileContents;
    public HashMap<String, byte[]> filesRemoved;
    String parentId;
    // Some other stuff ?

    public Commit (String message, String parentId, Date timestamp,
                   HashMap<String,byte[]> trackedFileContents, HashMap<String,byte[]> filesRemoved) {
        this.message = message;
        this.parentId = parentId;
        this.timestamp = timestamp;
        this.trackedFileContents = trackedFileContents;
        this.filesRemoved = filesRemoved;
        /*for (String s: trackedFileContents.keySet()) {
            if(filesRemoved.containsKey(s)) {
                trackedFileContents.remove(s);
            }
        }*/
    }
    public String getMessage() {
        return this.message;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getParentId() {
        return this.parentId;
    }

    public HashMap<String, byte[]> getTrackedFileContents() {
        return trackedFileContents;
    }

    public void setTrackedFileContents(HashMap<String, byte[]> trackedFileContents) {
        this.trackedFileContents = trackedFileContents;
    }



}
