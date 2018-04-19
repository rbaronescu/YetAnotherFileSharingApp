/*
 * YetAnotherFileSharingAppServer - The server side. jaja
 */

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.Command;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class NewConnectionHandler extends Thread {

    /* 10 Mebibytes (the new Megabyte) */
    static final int transferLength = 8192;

    int clientId;
    Socket clientSocket;
    String username = "";
    InputStream clientInputStream;
    OutputStream clientOutputStream;
    ObjectInputStream clientObjectInputStream;
    ObjectOutputStream clientObjectOutputStream;

    /**
     *
     * @param newClientSocket
     * @param clientId
     */
    public NewConnectionHandler(Socket newClientSocket, int clientId) {
        this.clientSocket = newClientSocket;
        this.clientId = clientId;
    }

    public void run() {
        try {

            clientInputStream = clientSocket.getInputStream();
            clientOutputStream = clientSocket.getOutputStream();
            clientObjectOutputStream = new ObjectOutputStream(clientOutputStream);
            clientObjectInputStream = new ObjectInputStream(clientInputStream);

            /* Recieving credentials. */
            String user = (String) clientObjectInputStream.readObject();
            String password = (String) clientObjectInputStream.readObject();

            /* Testing credentials. */
            boolean result = login(user, password);
            clientObjectOutputStream.writeObject(String.valueOf(result));
            if (!result) {
                return;
            }

            /* Sending list of his remote files. */
            sendRemoteFilesOwnedByThisUser();

            Command userCommand;
            do {
                userCommand = (Command) clientObjectInputStream.readObject();
                executeUserCommand(userCommand);
            } while (userCommand != null);

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void executeUserCommand(Command userCommand) {

        if (userCommand.isDownload()) {
            executeDownload(userCommand.getArgument(0));
        }
    }

    private boolean login(String username, String password) {

        String usersCsvFile = "data/users.csv";
        BufferedReader br = null;
        String line = "";
        String splitter = ",";
        boolean ret = false;

        try {

            br = new BufferedReader(new FileReader(usersCsvFile));
            while ((line = br.readLine()) != null) {

                String[] credentials = line.split(splitter);

                if (username.equals(credentials[0])) {
                    if (password.equals(credentials[1])) {
                        this.username = username;
                        ret = true;
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }

        return ret;
    }

    private void sendRemoteFilesOwnedByThisUser() {

        String userHomePath = "data/" + username;
        File userHome = new File(userHomePath);
        ArrayList<String> fileNames = new ArrayList<>(Arrays.asList(userHome.list()));

        try {

            /* Sending list size. */
            clientObjectOutputStream.writeObject(String.valueOf(fileNames.size()));

            for (String fileName : fileNames) {
                clientObjectOutputStream.writeObject(fileName);
            }

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void executeDownload(String fileName) {

        byte[] buf = new byte[transferLength];
        File f = new File(username, fileName);

        try {
            if (!f.exists()) {
                clientObjectOutputStream.writeObject("file not found");
            } else {
                clientObjectOutputStream.writeObject("download command received");
            }

            InputStream in = new FileInputStream(f);
            OutputStream out = clientOutputStream;

            int bytesRead;
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }

            in.close();

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}