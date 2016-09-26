package ftp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * This class represents the GUI of the server portion of the program.
 */
public class ServerGUI extends JFrame implements ActionListener, WindowListener {
    private static final long serialVersionUID = 1L;
    private JButton connectButton, addKey;
    private JTextArea serverTextField, event;
    private JTextField tPortNumber;
    private Server server;
    private boolean inBG;

    ServerGUI(int port, boolean visible) {
        super("Server");
        server = new Server(port);
        inBG = !visible;
        new ServerRunning().start();
        setVisible(false);
    }
    
    ServerGUI(int port) {
        super("Server");
        JPanel north = new JPanel();
        north.add(new JLabel("Port number: "));
        tPortNumber = new JTextField("" + port);
        north.add(tPortNumber);
        connectButton = new JButton("Start");
        connectButton.addActionListener(this);
        connectButton.setEnabled(false);
        addKey = new JButton("Select Key");
        addKey.addActionListener(this);
        north.add(connectButton);
        north.add(addKey);
        add(north, BorderLayout.NORTH);
        JPanel center = new JPanel(new GridLayout(2, 1));
        serverTextField = new JTextArea(80, 80);
        serverTextField.setEditable(false);
        displayServerScreen("> Server is not currently listening.");
        center.add(new JScrollPane(serverTextField));
        event = new JTextArea(80, 80);
        event.setEditable(false);
        displayToEventLog("Events log.");
        center.add(new JScrollPane(event));
        add(center);
        addWindowListener(this);
        setSize(450, 600);
        this.setLocationRelativeTo(null);
        setVisible(true);
    }

    void displayServerScreen(String str) {
        serverTextField.append(str + "\n");
    }
    void displayToEventLog(String str) {
        event.append(str + "\n");
    }

    public void actionPerformed(ActionEvent e) {
        Object object = e.getSource();
        //If the "Upload Key" button is pressed, the user selects a file and
        //the keyfile is set (in FTP.java).        
        if (object == addKey) {
            FTP.uploadKey();
            connectButton.setEnabled(true);
        }
        //If the button is active and says "Stop" (the server is running), then
        //pressing the "Stop" button will disconnect the connection.
        if (e.getSource() == this.connectButton && this.connectButton.getText().equalsIgnoreCase("Stop") && server != null) {
            server.disconnectClients();
            server = null;
            tPortNumber.setEditable(true);
            connectButton.setText("Start");
        //Otherwise, the server will start up.
        } else {
            int port;
            try {
                    port = Integer.parseInt(tPortNumber.getText().trim());
            } catch (Exception ee) {
                    displayToEventLog("Invalid port number.");
                    return;
            }
            server = new Server(port, this);
            new ServerRunning().start();
            this.displayServerScreen("> Server is listening on port: " + port);
            connectButton.setText("Stop");
            tPortNumber.setEditable(false);
        }
    }
    
    /**
     * When the user closes the window, the connection disconnects.
     * @param e 
     */
    public void windowClosing(WindowEvent e) {
        if (server != null) {
            try {
                server.disconnectClients();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
            server = null;
        }
        FTP.main(null);
    }

    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    
    class ServerRunning extends Thread {
        public void run() {
            if (inBG) server.startServer();
            else {
                server.startServer();
            }
        }
    }
}
