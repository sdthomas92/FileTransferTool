package ftp;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class Client {
        
    private final int ATTEMPT_CAP = 3;
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;
    private ClientGUI clientGUI;
    private String server, username;
    private int port;
    private int attempts, fail;
    private boolean asciiArmor, failed, abort;
    private File sendingFile;
    

    Client(String server, int port, String username) {
        this(server, port, username, null);
    }

    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        this.clientGUI = cg;
        this.attempts = 0;
        failed = false;
        abort = true;
    }
    
    Client(String server, int port) {
        this(server, port, null, null);
    }

    /**
     * This method starts the client, and tries to establish a connection with
     * the server (given a username/password combination). If a connection 
     * cannot be established, the method returns false, otherwise, it returns
     * true.
     * @return 
     */
    public boolean start() {
        try {
            socket = new Socket(server, port);
        } catch (Exception e) {
            e.printStackTrace();
            displayToClientScreen("Cannot establish a connection to server: " + 
                                  server + " on port: " + port + "\n" + e);
            return false;
        }
        String msg = "Connection established " + socket.getInetAddress() + 
                     ":" + socket.getPort();
        //displayToClientScreen(msg);
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
            this.sOutput.flush();
        } catch (IOException e) {
            displayToClientScreen("Error. Data stream could not be established.\n" + e);
            return false;
        }
        new ListenFromServer().start();
        try {
            sOutput.writeObject(username);
            this.sOutput.flush();
        } catch (IOException er) {
            displayToClientScreen("Exception doing login : " + er);
            disconnect();
            return false;
        }
        displayToClientScreen(msg);
        return true;
    }
    
    /**
     * Displays a message "msg" to the client screen.
     * @param msg 
     */
    private void displayToClientScreen(String msg) {
        if (clientGUI == null) 
            System.out.println(msg);
        else 
            clientGUI.displayToClientScreen("\n" + msg);
    }
    
    /**
     * Attempts to send a file to the server once connected. The method allows
     * a user to select a file (if "first" is true), and tries to send the file
     * using the "sendFile" method.
     * @param failAttempts
     * @param ascii
     * @param first 
     */
    public void uploadFile(int failAttempts, boolean ascii, boolean first) {
        if(first) {
            attempts = 0;
            abort = false;
        }
        asciiArmor = ascii;
        fail = failAttempts;
        try {
            File file;
            if(first) {
                String fileName = null;
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(null) != JFileChooser.CANCEL_OPTION) 
                    fileName = fc.getSelectedFile().getAbsolutePath();
                else 
                    return;
                file = new File(fileName);
                if(file != null)
                    clientGUI.uploadFile.setEnabled(false);
                sendingFile = file;
                displayToClientScreen("Transferring \"" + file.getName() + "\"...");
            }
            else
                file = sendingFile;
            failed = false;
            byte[] fileArray = new byte[(int) file.length()];
            if (fileArray.length > Math.pow(2, 30)) {
                JOptionPane.showMessageDialog(null, "File too large. Must be less than or equal to 1 GB.");
                return;
            }
            sendFile(file, FTP.keyFile, fail--, ascii);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a String "msg" to the server.
     * @param msg 
     */
    void sendMessage(NetworkMessage msg) {
        try {
            sOutput.writeObject(msg);
            this.sOutput.flush();
        } catch (Exception e) {
            displayToClientScreen("Exception writing to server: " + e);
        }
    }
    
    /**
     * Disconnects from the server and logs the user out.
     */
    public void disconnect() {
        try {
            if (sInput != null) 
                sInput.close();
        } catch (Exception e) {}
        try {
            if (sOutput != null) 
                sOutput.close();
        } catch (Exception e) {}
        try {
            if (socket != null) 
                socket.close();
        } catch (Exception e) {}
        if (clientGUI != null) 
            clientGUI.connectionFailed();
    }

    /**
     * This method catches any messages that are sent from the server to the
     * client. If the message is a String, it is displayed on the client
     * display. If the message has a FAILED_TRANSMISSION flag, the client
     * attempts to send the file to the server again. If the message has a
     * SUCCESSFUL_TRANSMISSION flag, the client enables the user to upload
     * another file.
     */
    private class ListenFromServer extends Thread {
        public void run() {
            while (true) {
                try {
                    NetworkMessage nm = (NetworkMessage) sInput.readObject();
                    if(nm.getType() == NetworkMessage.STRING) {
                        String msg = new String(nm.getByteArray(), "UTF-8");
                        if (clientGUI == null) {
                            System.out.println(msg);
                            System.out.print("> ");
                        } 
                        else {
                            clientGUI.displayToClientScreen("\n> " + msg);
                        }
                    }
                    else if(nm.getType() == NetworkMessage.FAILED_TRANSMISSION) {
                        failedTransmission();
                    }
                    else if(nm.getType() == NetworkMessage.SUCCESSUL_TRANSMISSION) {
                        clientGUI.uploadFile.setEnabled(true);
                    }
                } catch (IOException e) {
                    clientGUI.setStateOfLabelsTo(true);
                    if (clientGUI != null) 
                        clientGUI.connectionFailed();
                    else disconnect();
                        break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * This method turns a given file "inputFile" into a byte array, splits it
     * into chunks, and sends the chunks one by one. Before sending, the byte
     * array is appended with a hash of the data (4 bytes in length), the whole
     * chunk is encrypted given the "keyFile" file, and if "ascii" is true, the
     * chunk is ASCII-armored.
     * 
     * If "failAttempts" is greater than zero, the hash function is altered to
     * generate a different hash than what the server generates. This is used
     * for demonstration purposes only, and demonstrates what a failed file
     * transmission does.
     * 
     * Before sending any packets, a message is sent to the server with the
     * LISTEN flag. This tells the server whether or not the message will be
     * ASCII-armored or not. After all the packets are sent, a LASTPACKETSENT
     * flag is sent to the server, with the file name information. This is used
     * to write the file when it has all of the received chunks on the server
     * end.
     * 
     * Before doing anything, the method generates a random GUID, which is
     * appended to each chunk. This is used by the server to either listen
     * or not listen to any packets with this GUID (used if a file transfer 
     * re-attempt is made).
     * 
     * @param inputFile
     * @param keyFile
     * @param failAttempts
     * @param ascii 
     */
    public void sendFile(File inputFile, File keyFile, int failAttempts, boolean ascii) {       
        //Assigns a GUID to this particular file transfer.
        String GUID = UUID.randomUUID().toString();
        //Lets the server know if the file will be ASCII-armored.
        try {
            this.sOutput.writeObject(new NetworkMessage(NetworkMessage.LISTEN, new byte[]{(byte)(ascii ? 1 : 0)}));
            this.sOutput.flush();
        } catch (Exception e) {}
        
        byte[] file, key;
        try {
            //Turns both the file and key into byte arrays.
            file = FTP.fileToByteArray(inputFile);
            key = FTP.fileToByteArray(keyFile);
            
            //For every kilobyte in the file, a chunk is made, hashed, and these
            //are both merged into a byte array of length 1028, encrypted, encoded 
            //in Base 64 (if "ascii" is true), and sent.
            for(int i = 0; i < file.length; i += 1024) {
                if(!failed) {
                    //Creates a kilobyte byte array for the current file chunk. If
                    //this is the last iteration and this file chunk is less than a
                    //kilobyte in length (the remander of the file), the byte array
                    //length is adjusted accordingly.
                    byte[] send = new byte[(file.length - i) < 1024 ? (file.length - i) + 4 : 1028];

                    //The new byte array is filled with the chunk's contents.
                    byte[] sendHash = new byte[send.length - 4]; 
                    for(int j = 0; j < sendHash.length; j++) {
                        send[j] = file[i+j];
                        sendHash[j] = file[i+j];
                    }

                    //The hash is generated (4 bytes in length). If failAttempts
                    //is greater than zero, an alternate algorithm is used to
                    //generate the hash.
                    byte[] hash = FTP.hash(sendHash, key, (failAttempts <= 0));


                    //The hash is put in the last remaining 4 bytes of the byte array to be sent.
                    for(int j = 0; j < hash.length; j++)
                        send[((file.length - i) < 1024 ? (file.length - i): 1024) + j] = hash[j];

                    //The byte array is encrypted.
                    byte[] encrypt = FTP.encryptDecrypt(send, key);
                    if(ascii) {

                        //The byte array is encoded in Base64
                        String encode = FTP.encodeBase64(encrypt);

                        //The encoded string is converted into a byte array representation. This
                        //is what will be sent to the receiver.
                        byte[] finalArray = encode.getBytes(Charset.forName("UTF-8"));

                        //finalArray is sent to the receiver.
                        this.sOutput.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, finalArray, GUID));
                        this.sOutput.flush();
                    }
                    else {
                        //The encrypted byte array is sent to the receiver.
                        this.sOutput.writeObject(new NetworkMessage(NetworkMessage.UPLOADFILE, encrypt, GUID));
                        this.sOutput.flush();
                    }
                }
            }
            if(!failed)
                //When all the chunks are sent successfully, a message is sent
                //to the receiver with the LASTPACKETSENT flag. This message
                //contains the original file's name/extension.
                this.sOutput.writeObject(new NetworkMessage(NetworkMessage.LASTPACKETSENT, 
                                                            FTP.encryptDecrypt(inputFile.getName().getBytes(Charset.forName("UTF-8")), 
                                                                               key), 
                                                            GUID));
                this.sOutput.flush();
        }
        catch (Exception e){
            if(abort)
                return;
            e.printStackTrace();
            failedTransmission();
        }
    }
    
    /**
     * This method is called when the client's transmission failed. This can
     * be if a sending packet throws an error, or if the server sends a
     * FAILED_TRANSMISSION flag. The method reduces the number of attempts it
     * has left, and attempts to send the file again (using a different GUID).
     */
    private void failedTransmission() {
        failed = true;
        attempts++;
        if(attempts < ATTEMPT_CAP) {
            displayToClientScreen("Failed transmission, attempting to send again...");
            uploadFile(fail, asciiArmor, false);
        }
        else {
            abort = true;
            displayToClientScreen("File failed to upload.");
            disconnect();  
        }
    }
}
