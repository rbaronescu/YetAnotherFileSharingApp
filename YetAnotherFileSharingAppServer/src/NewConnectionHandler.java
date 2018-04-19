/*
 * YetAnotherFileSharingAppServer - The server side.
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

    /**/
    static final int transferLength = 8192;

    /* All data on the server. */
    private static final String DATA_ROOT_PATH = "data";
    static final String USERS_CSV_FILE_PATH = DATA_ROOT_PATH + "/users.csv";

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

            Command userCommand;
            do {
                userCommand = (Command) clientObjectInputStream.readObject();
                executeUserCommand(userCommand);
            } while (userCommand != null);

        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void executeUserCommand(Command userCommand) {

        if (userCommand.isDownload()) {
            executeDownload(userCommand);
        } else if (userCommand.isUpload()) {
            executeUpload(userCommand);
        } else if (userCommand.isGetListOfRemoteFiles()) {
            sendRemoteFilesOwnedByThisUser();
        } else if (userCommand.isRemoveFile()) {
            removeFile(userCommand);
        } else if (userCommand.isNewEmptyFile()) {
            newEmptyFile(userCommand);
        }
    }

    private boolean login(String username, String password) {

        BufferedReader br = null;
        String line = "";
        String splitter = ",";
        boolean ret = false;

        try {

            br = new BufferedReader(new FileReader(USERS_CSV_FILE_PATH));
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

        File userHome = new File(DATA_ROOT_PATH, username);
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

    private void executeDownload(Command command) {

        byte[] buf = new byte[transferLength];
        File userHome = new File(DATA_ROOT_PATH, username);
        File f = new File(userHome, command.getArgument(0));

        try {
            if (!f.exists()) {
                clientObjectOutputStream.writeObject("file not found");
            } else {
                clientObjectOutputStream.writeObject(String.valueOf(f.length()));
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

    private void executeUpload(Command command) {

        /* Amount of data transfered between client and server. */
        byte[] receivedData = new byte[transferLength];

        /* User home must exist, it is created upon registration. */
        File userHome = new File(DATA_ROOT_PATH, username);

        try {

            /* Start receveing the file. The received data is written to fileOut. */
            File fileOut = new File(userHome, command.getArgument(0));
            if (!fileOut.exists()) {
                fileOut.createNewFile();
            }

            InputStream in = clientInputStream;
            FileOutputStream out = new FileOutputStream(fileOut);

            long totalBytesToReceive = Long.parseLong(command.getArgument(1));
            long totalBytesReceived = 0;
            int bytesReceived = 0;
            while (totalBytesReceived < totalBytesToReceive) {
                bytesReceived = in.read(receivedData);
                out.write(receivedData, 0, bytesReceived);
                totalBytesReceived += bytesReceived;
            }

            out.close();
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.INFO,
                    "File " + command.getArgument(0) + "received!");

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void removeFile(Command command) {

        /* User home must exist, it is created upon registration. */
        File userHome = new File(DATA_ROOT_PATH, username);

        /* Opening the file to remove. */
        File f = new File(userHome, command.getArgument(0));

        try {
            if (!f.exists()) {
                clientObjectOutputStream.writeObject("file not found");
                return;
            }

            f.delete();
            clientObjectOutputStream.writeObject("success");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void newEmptyFile(Command command) {

        /* User home must exist, it is created upon registration. */
        File userHome = new File(DATA_ROOT_PATH, username);
        File f = new File(userHome, command.getArgument(0));

        try {

            if (f.createNewFile()) {
                clientObjectOutputStream.writeObject("success");
            } else {
                clientObjectOutputStream.writeObject("file exists");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}