package ftp;

import java.io.*;

/**
 * This class represents the NetworkMessage object, which is used to transfer
 * information between the client and server. This object has a flag (type),
 * byte array, message String, and GUID String (transmissionUUID).
 */
public class NetworkMessage implements Serializable {
	
    protected static final long serialVersionUID = 1112122200L;

    //Represents the different possible flags the NetworkMessage can have.
    static final int ERROR = -1, 
                     UPLOADFILE = 0, 
                     MESSAGE = 1, 
                     LOGOUT = 2, 
                     LASTPACKETSENT = 3,
                     LOGIN = 4,
                     STRING = 5,
                     FAILED_TRANSMISSION = 6,
                     LISTEN = 7,
                     SUCCESSUL_TRANSMISSION = 8;
    private int type;
    private byte[] fileArray;
    private String message;
    private String transmissionUUID;
    
    //This constructor is used for assigning just a flag to a NetworkMessage.
    NetworkMessage(int type){
        this.type = type;
    }
    
    //This constructor is used for assigning a flag and attaching a message.
    NetworkMessage(int type, String message) {
        this.type = type;
        this.message = message;
    }
    
    //This constructor is used for assigning a flag and attaching a byte array.
    NetworkMessage(int type,byte[] filearray){
        this.type=type;
        this.fileArray=filearray;
    }
    
    //This constructor is used for assigning a flag and attaching a byte array
    //and a GUID (used for sending chunks from the client to the server).
    NetworkMessage(int type, byte[] filearray, String transmissionUUID) {
        this.type = type;
        this.fileArray = filearray;
        this.transmissionUUID = transmissionUUID;
    }
    
    //Returns the NetworkMessage flag.
    int getType() {
        return type;
    }
    
    //Returns the NetworkMessage byte array.
    public byte[] getByteArray(){
        return fileArray;
    } 
    
    //Returns the NetworkMessage String message.
    String getMessage() {
        return message;
    }
    
    //Returns the NetworkMessage GUID.
    public String getGUID() {
        return transmissionUUID;
    }
}
