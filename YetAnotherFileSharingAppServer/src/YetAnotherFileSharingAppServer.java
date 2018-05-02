/*
 * YetAnotherFileSharingAppServer - The server side.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class YetAnotherFileSharingAppServer {

    static private final int SERVER_PORT = 5151;
    static private ServerSocket listeningSocket;
    static private int clientId = 0;

    public static void main(String[] args) {

        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO, "server started");

        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO, "reading config files..");
        SharedFile.fillSharedFilesList();
        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO, "read config files");


        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO,
                "Waiting for connections...");

        try {
            listeningSocket = new ServerSocket(SERVER_PORT);
            do {
                Socket newClientSocket = listeningSocket.accept();
                Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO,
                        "Client " + clientId + " from " + newClientSocket.getInetAddress().getHostName());
                Thread handleNewConnection = new NewConnectionHandler(newClientSocket, clientId);
                handleNewConnection.start();
                clientId++;
            } while (true);
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
