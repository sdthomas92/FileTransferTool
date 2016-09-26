package ftp;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
    
    /*
    * These String arrays contain the usernames that are accepted by the server.
    * The username at index i has the corresponding salt and hashed password
    * in SALTS[i] and HASHES[i] respectively.
    */
    public static String[] SALTS = new String[]{"CIw8fA==", "6feC9A==", "qwRvGA==", "Fy6Tg9=="};
    public static String[] USERNAMES = new String[]{"stephen", "gabriel", "ervin", "david"};
    public static String[] HASHES = new String[]{"L6SMvA==", "hqbA/A==", "N8j0/A==", "uWOsvA=="};
    
    private static String welcomeMessage = 
            "> This is a file transfer program written by Gabriel Martinez, Ervin Maulas, and Stephen Thomas for"
            + " David\nGershman's CS 380 Class. This message is printed as a verification that the client and"
            + " server have\nestablished a connection, and the client has been authorized by the server.";
    
    public static ArrayList<String> GUIDs;
    
    private static int uniqueId;
    private ArrayList<ClientThread> clientThreadArrayList;
    private ServerGUI serverGUI;
    private SimpleDateFormat sdateFormat;
    private int port;
    private boolean keepGoing;
    private ServerSocket serverSocket;
    public static boolean asciiArmored, aunthenticated;
    
    public Server(int port) {
	this(port, null);
    }
    
    public Server(int port, ServerGUI sg) {
	this.serverGUI = sg;
	this.port = port;
	sdateFormat = new SimpleDateFormat("HH:mm:ss");
	clientThreadArrayList = new ArrayList<ClientThread>();
        aunthenticated = false;
        GUIDs = new ArrayList<>();
        asciiArmored = false;
    }
    
    /**
     * The server begins to listen for a client.
     */
    public void startServer() {
	keepGoing = true;
	try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                //System.out.println(socket.getInetAddress());
		if (!keepGoing) 
                    break;
		ClientThread client = new ClientThread(socket);
		clientThreadArrayList.add(client);
		client.start();
            }
            try {
		disconnectClients(serverSocket);
            } catch (Exception e) {
		e.printStackTrace();
            }
	} catch (IOException e) {}
    }
    
    /**
     * Disconnects from the clients.
     */
    public void disconnectClients() {
	this.disconnectClients(serverSocket);
    }
    
    /**
     * Allows the server to disconnect from the clients.
     * @param serverSocket 
     */
    private void disconnectClients(ServerSocket serverSocket) {
        try {
            this.keepGoing = false;
            for ( int i = 0 ; i < clientThreadArrayList.size() ; ++i) {
                ClientThread ct = clientThreadArrayList.get(i);
		try {
                    serverGUI.displayServerScreen("> Server disconnceted with " + ct.username + ".");
                    ct.sendToClient(FTP.stringToByteArray("Server has disconnected."), 
                                    NetworkMessage.STRING);
                    ct.closeStreams();
		} catch (Exception e) {
                    
                    serverGUI.displayToEventLog("Exception closing the server and clients: " + e);
		}
            }
            serverSocket.close();
            serverGUI.displayServerScreen("> Sever stopped listening.");
	} catch (Exception ee) {}
    }
    
    /**
     * Display an event (not a message) to the console or the GUI.
     */
    public void displayToServerLog(String msg) {
        String time = sdateFormat.format(new Date()) + " " + msg;
	if (serverGUI == null) 
            System.out.println(time);
	else 
            serverGUI.displayToEventLog(time);
    }
    
    /**
     * This connects to a client and authorizes the client given an entered
     * username and password. It also listens for messages that are sent from
     * the client. The server can also disconnect from the client.
     */
    public class ClientThread extends Thread {
	private Socket socket;
	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private int id;
	private String username;
	private NetworkMessage networkMessage;
	private String date;
	
        /**
         * Connects to a client and authorizes the client to send messages.
         * If a client cannot be authorized, they cannot log in.
         * @param socket 
         */
        ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
		sInput = new ObjectInputStream(socket.getInputStream());
		try {
                    username = (String) sInput.readObject();
                    byte[] encrypt = FTP.decodeBase64(username);
                    byte[] usernameBytes = FTP.encryptDecrypt(encrypt, FTP.fileToByteArray(FTP.keyFile));
                    
                    
                    username = new String(usernameBytes, "UTF-8");
                }
                catch(Exception ee) {
                    username = null;
                }
                if(username == null || !authorize(username)) {
                    sendToClient(FTP.stringToByteArray("Invalid username/password"), NetworkMessage.STRING);
                    closeStreams();
                    return;
                }
                username = username.substring(0, username.indexOf(":"));
		if (serverGUI != null) 
                    serverGUI.displayServerScreen("> " + username + " just connected.");
		else 
                    System.out.println("> " + username + " just connected.");
		sendToClient(FTP.stringToByteArray("Server says: Hello " + username + "!" + "\n" + welcomeMessage), 
                             NetworkMessage.STRING);
            } catch (IOException e) {
                serverGUI.displayToEventLog("Exception creating new input/output Streams: " + e);
                return;
            }
            date = new Date().toString();
	}
        
        /**
         * Given a string that contains both the username and password (in the
         * format: <username>:<password>, the method returns true if they are
         * authorized to log in. The username is first checked if it exists in
         * the USERNAMES String array. If so, the index of where it is located
         * in USERNAMES is recorded ("index"). The password is then hashed with
         * SALTS[index] and compared to HASHES[index]. If they match, the method
         * returns true and the user is authorized to sign in. Otherwise, if
         * they are not authorized, it returns false.
         * @param usernameAndPass
         * @return 
         */
        private boolean authorize(String usernameAndPass) {
            String username = usernameAndPass.substring(0, 
                                                        usernameAndPass.indexOf(":"));
            String pass = usernameAndPass.substring(usernameAndPass.indexOf(":") + 
                                                    1);
            int index = -1;
            for(int i = 0; i < USERNAMES.length; i++) {
                if(username.equals(USERNAMES[i])) {
                    index = i;
                    break;
                }
            }
            if(index == -1)
                return false;
            try {            
                if(HASHES[index].equals(FTP.encodeBase64(FTP.hash(pass.getBytes("UTF-8"), 
                                                                  SALTS[index].getBytes("UTF-8"), 
                                                                  true)))) {
                    aunthenticated = true;
                    return true;
                }
                return false;
            } catch (Exception e) {return false;}
        }
        
        public void run() {
            listenForClients();
	}
        
	/**
         * This method listens for any messages sent by the connected client. If
         * the message has a LOGOUT flag, the server disconnects from the 
         * client. If the message has a LISTEN flag, it knows whether or not
         * incoming chunks are ASCII-armored. If the message has an UPLOADFILE
         * flag, then it knows the message contains a chunk of the file it is
         * supposed to receive, and the byte array sent with it is passed to
         * the method "recieveFinishedDataPiece". If the method returns a null
         * array, this means the chunk received does not match what was sent,
         * and the server tells the client to re-upload it, otherwise, the
         * returned byte array is added to the byteList array list, which will
         * later turn that data into a file. If the message has a LASTPACKETSENT
         * flag, it knows to not listen for any more messages. All the
         * received chunks are then converted into a file using the 
         * "byteListToFile" method and saved.
         */
	public void listenForClients() {
            ArrayList<byte[]> byteList = new ArrayList<byte[]>();
            while (true) {                       
                try {
                    networkMessage = (NetworkMessage) sInput.readObject();                   
		} catch (IOException e) {
                    if (this.socket.isClosed() && aunthenticated) {
                        displayToServerLog(username + " connection has been terminated. " + e);
                    } 
                    else 
                        displayToServerLog(username + " File transfer was unsuccesful: " + e);
                    break;
		} catch (ClassNotFoundException e2) {
                    break;
		}
		switch (networkMessage.getType()) {
                    case NetworkMessage.LOGOUT:
                        displayToServerLog(username + " disconnected.");
                        sendToClient(FTP.stringToByteArray("Sever says: Goodbye " + username + "!"), NetworkMessage.STRING);
                        this.closeStreams();
                        return;
                    case NetworkMessage.LISTEN:
                        asciiArmored = (networkMessage.getByteArray()[0] == (byte)1);
                        serverGUI.displayServerScreen("> " + username + " is attempting to send a file");
                        break;
                    case NetworkMessage.UPLOADFILE:
			//If the GUID in the message sent is in the GUIDs list,
                        //the message is ignored.
                        if(!GUIDs.contains(networkMessage.getGUID())) {
                            byte[] fileByteArray = networkMessage.getByteArray();
                            try {
                                byte[] add = recieveFinishedDataPiece(fileByteArray, FTP.keyFile);
                                if(add == null) {
                                    //The GUID sent with the message is added
                                    //to a list of GUIDs. If any future
                                    //messages are sent with this GUID, they
                                    //are not interpreted by the server. This
                                    //allows the server to only listen for
                                    //messages from the client after retrying
                                    //the transmission.
                                    GUIDs.add(networkMessage.getGUID());
                                    if (serverGUI != null) {
                                        serverGUI.displayServerScreen("> There was an error transferring the file.");
                                    }
                                    else
                                        System.out.println("> There was an error transferring the file.");
                                    byteList.clear();
                                    sendToClient(new byte[0], NetworkMessage.FAILED_TRANSMISSION);
                                    break;
                                }
                                byteList.add(add);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (serverGUI != null) 
                                serverGUI.displayServerScreen("> File byte array: " + Arrays.toString(fileByteArray));
                            else 
                                System.out.println("> File byte array: " + Arrays.toString(fileByteArray));
                            break;
                        }
                        break;
                    case NetworkMessage.LASTPACKETSENT:
                        if(!GUIDs.contains(networkMessage.getGUID())) {
                            byte[] fByteArray = networkMessage.getByteArray();
                            if (serverGUI != null) 
                                serverGUI.displayServerScreen("> File byte array: " + Arrays.toString(fByteArray));
                            else {
                                System.out.println("> File byte array: " + Arrays.toString(fByteArray));
                                System.out.println("> File has been succesfully received.");
                            }
                            sendToClient(FTP.stringToByteArray(sdateFormat.format(new Date()) + " File successfully transferred."), 
                                         NetworkMessage.STRING);
                            sendToClient(new byte[0], NetworkMessage.SUCCESSUL_TRANSMISSION);
                            try {
                                //The program knows that this is the last packet to be received for this file
                                //transfer. This packet contains an ecrypted byte array that represents the
                                //file name of the file uploaded from the client.
                                String fileName = byteListToFile(byteList, 
                                                                 new String(FTP.encryptDecrypt(fByteArray, 
                                                                                               FTP.fileToByteArray(FTP.keyFile)), 
                                                                            "UTF-8"));
                                byteList.clear();
                                serverGUI.displayServerScreen("> \"" + fileName +  "\" has been succesfully received.");
                                
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                        
		}
            }
            closeStreams();
	}
		
        /**
         * Disconnects the server from the client.
         */
        private void closeStreams() {
            aunthenticated = false;
            try {
                if (sOutput != null) {
                    sOutput.writeBoolean(true);
                    sOutput.close();
                }
            } 
            catch (Exception e) {}
            try {
		if (sInput != null) 
                    sInput.close();
	    } catch (Exception e) {}
            try {
                if (socket != null) 
                    socket.close();
            } catch (Exception e) {
		serverGUI.displayToEventLog("Connection to sever closed.");
            }
	}
        
        /**
         * Sends a message to the client with a flag "type".
         * @param msg
         * @param type
         * @return 
         */
        private boolean sendToClient(byte[] msg, int type) {

            if (!socket.isConnected()) {
                    closeStreams();
                    return false;
            }
            
            try {
                    sOutput.writeObject(new NetworkMessage(type, msg));
                    this.sOutput.flush();
            }
            
            catch (IOException e) {
                    displayToServerLog("Error sending message to " + username);
                    displayToServerLog(e.toString());
            }
            return true;
        }
    }
    
    /**
     * This method takes a chunk that was sent by the client and returns the
     * original data chunk made by the client before sending. If the message
     * is ASCII-armored, the method decodes the String and makes it into a
     * byte array. The byte array is decrypted given a key, and the hash is
     * taken from the last 4 bytes of the chunk. The method then hashes the
     * rest of the chunk, and if the 4 bytes at the end match the result, the
     * chunk transferred with no problems, and is returned. If it doesn't match,
     * null is returned.
     * @param data
     * @param key
     * @return
     * @throws Exception 
     */
    public static byte[] recieveFinishedDataPiece(byte[] data, File key) throws Exception {       
        
        try {
            //Converts the sent byte array to a string, which is decoded and turned into
            //a new byte array. The byte array is then decrypted given the key.
            byte[] decrypt, decode;
            if(asciiArmored) {           
                String string = new String(data, "UTF-8");
                decode = FTP.decodeBase64(string);
                decrypt = FTP.encryptDecrypt(decode, FTP.fileToByteArray(key));
            }
            else {
                decrypt = FTP.encryptDecrypt(data, FTP.fileToByteArray(key));
            } 

            //This is the byte array that will store the originally sent data in the
            //chunk.
            byte[] finishedData = new byte[decrypt.length - 4];

            //This stores the hash that came with the chunk.
            byte[] hash = new byte[4];

            //Fills both of these arrays with their proper values.
            for(int i = 0; i < finishedData.length; i++)
                finishedData[i] = decrypt[i];
            for(int i = 0; i < hash.length; i++)
                hash[i] = decrypt[(decrypt.length-4) + i];

            //If the hash included with the chunk does not equal the hashed version
            //of the finished data, then the file was sent incorrectly, and null
            //is returned.
            if(!FTP.compareByteArrays(FTP.hash(finishedData, FTP.fileToByteArray(key), true), 
                                      hash)) 
                return null;
            //The data originally put in the chunk is returned.
            return finishedData;
        }
        catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Turns a byte array list into a file. It first makes a byte list with all
     * of the bytes in the given byte array list. Then with these bytes, a byte
     * array is created, and all of the bytes are inserted into the array. The
     * array is then written to a file with the name "fileName" (which is what
     * is sent in the LASTPACKET message from the client). The extension is
     * converted to lower case, since writing files with all uppercase
     * extensions could not be done.
     * @param byteList
     * @param fileName
     * @throws Exception 
     */
    public static String byteListToFile(ArrayList<byte[]> byteList, String fileName) throws Exception  {
        String name = "", ext = "";
        if(fileName.contains(".")) {
            name = fileName.substring(0, fileName.lastIndexOf("."));
            ext = fileName.substring(fileName.lastIndexOf(".")+1);
        }
        ArrayList<Byte> bytes = new ArrayList<Byte>();
        for(int i = 0; i < byteList.size(); i++) {
            if(byteList.get(i) != null) {
                for(int j = 0; j < byteList.get(i).length; j++)
                    bytes.add(byteList.get(i)[j]);
            }
        }
        
        byte[] finalBytes = new byte[bytes.size()];
        
        for(int i = 0; i < finalBytes.length; i++)
            finalBytes[i] = bytes.get(i);
        
        FileOutputStream fos = new FileOutputStream(fileName.contains(".") ? name + "." + ext.toLowerCase() : fileName);
        fos.write(finalBytes);
        fos.close();
        
        return fileName.contains(".") ? name + "." + ext.toLowerCase() : fileName;
    }
}
