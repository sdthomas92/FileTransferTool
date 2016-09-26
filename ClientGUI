package ftp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.nio.charset.Charset;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * This class represents the GUI of the client portion of the program.
 */
public class ClientGUI extends JFrame implements ActionListener, WindowListener {
    private static final long serialVersionUID = 1L;
    private JLabel hashLabel, asciiLabel;
    private JTextField textField, hashField;
    private JPasswordField passwordField;
    private JTextField textFieldServer, textFieldPort;
    public JButton	login, logout, uploadFile, uploadKey;
    private JTextArea clientDisplayScreen;
    private boolean	connected, keySelected = false;
    private Client client;
    private int portNumber;
    private String defaultHost;
    private JCheckBox hash, ascii;

    ClientGUI(String host, int port) {
        super("Client");
        portNumber = port;
        defaultHost = host;
        JPanel northPanel = new JPanel(new GridLayout(3, 1));
        JPanel serverAndPortPanel = new JPanel(new GridLayout(1, 5, 1, 3));
        textFieldServer = new JTextField(host);
        textFieldPort = new JTextField("" + port);
        textFieldPort.setHorizontalAlignment(SwingConstants.RIGHT);
        serverAndPortPanel.add(new JLabel("Server Address:  "));
        serverAndPortPanel.add(textFieldServer);
        serverAndPortPanel.add(new JLabel("Port Number:  "));
        serverAndPortPanel.add(textFieldPort);
        serverAndPortPanel.add(new JLabel(""));
        northPanel.add(serverAndPortPanel);
        textField = new JTextField("Username");
        northPanel.add(textField);
        add(northPanel, BorderLayout.NORTH);
        passwordField = new JPasswordField("Password");
        northPanel.add(passwordField);
        add(northPanel, BorderLayout.NORTH);
        clientDisplayScreen = new JTextArea("Login to the server.", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1, 1));
        centerPanel.add(new JScrollPane(clientDisplayScreen));
        clientDisplayScreen.setEditable(false);
        clientDisplayScreen.setBackground(Color.black);
        clientDisplayScreen.setForeground(Color.green);
        add(centerPanel, BorderLayout.CENTER);
        login = new JButton("Login");
        login.addActionListener(this);
        login.setEnabled(false);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false);
        uploadFile = new JButton("Upload File");
        uploadFile.addActionListener(this);
        uploadFile.setEnabled(false);
        uploadKey = new JButton("Select Key");
        uploadKey.addActionListener(this);
        hash = new JCheckBox();
        JPanel southPanel = new JPanel();
        southPanel.add(login);
        southPanel.add(logout);
        southPanel.add(uploadFile);
        southPanel.add(uploadKey);
        hashField = new JTextField("0  ");
        hashField.setEnabled(false);
        hashLabel = new JLabel("Failed hash");
        hash = new JCheckBox();
        hash.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                hashField.setEnabled(!hashField.isEnabled());
            }
        });
        asciiLabel = new JLabel("ASCII armor");
        ascii = new JCheckBox();
        ascii.setSelected(true);
        southPanel.add(hashLabel);
        southPanel.add(hash);
        southPanel.add(hashField);
        southPanel.add(asciiLabel);
        southPanel.add(ascii);
        add(southPanel, BorderLayout.SOUTH);
        setSize(650, 500);
        this.setLocationRelativeTo(null);
        this.addWindowListener(this);
        setVisible(true);
    }
    
    void displayToClientScreen(String str) {
        clientDisplayScreen.append(str);
    }
    
    void connectionFailed() {
        login.setEnabled(true);
        logout.setEnabled(false);
        uploadFile.setEnabled(false);
        textFieldPort.setText("" + portNumber);
        textFieldServer.setText(defaultHost);
        textField.removeActionListener(this);
        passwordField.removeActionListener(this);
        connected = false;
    }
    public void actionPerformed(ActionEvent e) {
        Object object = e.getSource();
        //If the "Logout" button is pressed, the client and server disconnect.
        if (object == logout) {
            this.setStateOfLabelsTo(true);
            client.sendMessage(new NetworkMessage(NetworkMessage.LOGOUT));
            login.setEnabled(true);
            return;
        }
        //If the "Upload File" button is pressed, the user selects a file and
        //the file is uploaded by the client to the server.
        if (object == uploadFile) {
            try {
                client.uploadFile((hash.isSelected() ? Integer.parseInt(hashField.getText().trim()) : 0), ascii.isSelected(), true);
            } catch (Exception ee) {
                client.uploadFile(0, ascii.isSelected(), true);
            }
            return;
        }
        //If the "Upload Key" button is pressed, the user selects a file and
        //the keyfile is set (in FTP.java).
        if (object == uploadKey) {
            FTP.uploadKey();
            keySelected = true;
            if(FTP.keyFile != null)
                login.setEnabled(true);
        }
        if (connected) {
            client.sendMessage(new NetworkMessage(NetworkMessage.MESSAGE, textField.getText()));
            return;
        }
        //If the "Login" button is pressed, the username and password is used
        //to try and connect to the server. The server can either accept or
        //reject the username and password.
        if (object == login) {
            String username = textField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.length() == 0 || password.length() == 0) 
                return;
            String server = textFieldServer.getText().trim();
            if (server.length() == 0) 
                return;
            String portNumber = textFieldPort.getText().trim();
            if (portNumber.length() == 0) 
                return;
            try {
                this.portNumber = Integer.parseInt(portNumber);
            } catch (Exception ee) {
                return;
            }
            client = authenticate(username, password, client, server);
            if(client == null) {
                displayToClientScreen("\n> Login failed.");
                return;
            }
            if (!client.start()) 
                return;
            setStateOfLabelsTo(false);
            connected = true;
            login.setEnabled(false);
            logout.setEnabled(true);
            if(keySelected)
                uploadFile.setEnabled(true);
            defaultHost = textFieldServer.getText();
            portNumber = textFieldPort.getText();
        }
    }

    /**
     * This method creates a new client based on what the user puts in the
     * username and password fields. The username and password are concatenated,
     * encrypted, and sent to the server for further authentication.
     * @param username
     * @param password
     * @param client
     * @param server
     * @return 
     */
    private Client authenticate(String username, String password, Client client, String server) {
        try {
            byte[] usernameAndPass = (username + ":" + password).getBytes(Charset.forName("UTF-8"));
            byte[] decrypt = FTP.encryptDecrypt(usernameAndPass, FTP.fileToByteArray(FTP.keyFile));
            String send = FTP.encodeBase64(decrypt);
            return new Client(server, this.portNumber, send, this);
        }
        catch(Exception e) {
            return null;
        }
    }

    /**
     * This method sets the status of several fields and buttons to be boolean
     * b. This means several buttons and fields can either be set to all be
     * active or inactive.
     * @param b 
     */
    public void setStateOfLabelsTo(boolean b) {
        this.textField.setVisible(b);
        this.passwordField.setVisible(b);
        textFieldServer.setEditable(b);
        textFieldPort.setEditable(b);
        textField.setEditable(b);
        passwordField.setEditable(b);
    }
    
    /**
     * When the user closes the window, the connection disconnects.
     * @param e 
     */
    public void windowClosing(WindowEvent e) {
        if (client != null) {
            try {
                client.disconnect(); // close the connection
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            client = null;
        }
        FTP.main(null);
    }
    
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    
    public static void main(String[] args) {
            ClientGUI c = new ClientGUI("localhost", 65535);
    }
}
