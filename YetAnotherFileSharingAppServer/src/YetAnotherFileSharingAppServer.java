/*
 * YetAnotherFileSharingAppServer - The server side.
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class YetAnotherFileSharingAppServer {

    private static final int SERVER_PORT = 5151;
    private static ServerSocket listeningSocket;
    private static int clientId = 0;

    private static final String DATA_ROOT_PATH = "data";
    static private final String SHARED_FILES_PATH = DATA_ROOT_PATH + "/sharedFiles.dat";
    static private final String SHARED_FILES_INVITES_PATH = DATA_ROOT_PATH + "/sharedFileInvites.dat";
    static private final String SHARED_FILES_KICKS_PATH = DATA_ROOT_PATH + "/sharedFileKicks.dat";

    public static void main(String[] args) {

        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO, "server started");

        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO, "reading config files..");
        SharedFile.loadSerializedSharedFiles(SHARED_FILES_PATH);
        SharedFile.loadSerializedInvites(SHARED_FILES_INVITES_PATH);
        SharedFile.loadSerializedKicks(SHARED_FILES_KICKS_PATH);
        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO, "read config files");

        Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO,
                "Waiting for connections...");

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO,
                        "saving config to files ...");
                SharedFile.serializeSharedFilesToBinaryFile(SHARED_FILES_PATH);
                SharedFile.serializeInvitessToBinaryFile(SHARED_FILES_INVITES_PATH);
                SharedFile.serializeKicksToBinaryFile(SHARED_FILES_KICKS_PATH);
                Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO,
                        "done saving configs");

            }
        };
        timer.schedule(task,0, 10000);

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
