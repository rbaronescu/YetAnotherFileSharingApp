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
import backend.FileInfo;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class NewConnectionHandler extends Thread {

    /**/
    private static final int transferLength = 8192;

    /* All data on the server. */
    private static final String DATA_ROOT_PATH = "data";
    private static final String USERS_CSV_FILE_PATH = DATA_ROOT_PATH + "/users.csv";

    private static final String USERNAME_NEW_USER_REQUEST = "newUserUsername";
    private static final String PASSWORD_NEW_USER_REQUEST = "newUserPassword";

    private int clientId;
    private Socket clientSocket;
    private String username = "";
    private InputStream clientInputStream;
    private OutputStream clientOutputStream;
    private ObjectInputStream clientObjectInputStream;
    private ObjectOutputStream clientObjectOutputStream;

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

            /* Testing if this is a "register new user" request. */
            if (user.equals(USERNAME_NEW_USER_REQUEST)) {
                if (password.equals(PASSWORD_NEW_USER_REQUEST)) {
                    registerNewUser((Command) clientObjectInputStream.readObject());
                    clientSocket.close();
                    return;
                }
            }

            /* Testing credentials. */
            boolean result = login(user, password);
            clientObjectOutputStream.writeObject(String.valueOf(result));
            if (!result) {
                clientSocket.close();
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
            sendRemoteFiles();
        } else if (userCommand.isRemoveFile()) {
            removeFile(userCommand);
        } else if (userCommand.isNewEmptyFile()) {
            newEmptyFile(userCommand);
        } else if (userCommand.isGetListOfUsers()) {
            sendListOfUsers(userCommand);
        } else if (userCommand.isShareFileWith()) {
            shareFileWith(userCommand);
        } else if (userCommand.isCheckForNotifications()) {
            checkForNotification();
        } else if (userCommand.isGetUserNotifications()) {
            sendUserNotifications();
        } else if (userCommand.isRespondToNotification()) {
            processResponseToNotification(userCommand);
        } else if (userCommand.isgetListOfUsersWithAccessTo()) {
            sendListOfUsersWithAccessTo(userCommand);
        } else if (userCommand.isKickUserFrom()) {
            kickUserFrom(userCommand);
        } else if (userCommand.isChangeTokenHolder()) {
            changeTokenHolder(userCommand);
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

    private void registerNewUser(Command command) {

        BufferedReader br = null;
        String line = "";
        String splitter = ",";
        String response = "success";

        String _username = command.getArgument(0);
        String _password = command.getArgument(1);

        try {

            br = new BufferedReader(new FileReader(USERS_CSV_FILE_PATH));
            while ((line = br.readLine()) != null) {

                String[] credentials = line.split(splitter);
                if (_username.equals(credentials[0])) {
                    response = "fail";
                    break;
                }
            }

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
            response = "fail";
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
                }
            }
        }

        if (response.equals("success")) {

            try {

                Writer output = new BufferedWriter(new FileWriter(USERS_CSV_FILE_PATH, true));
                output.append(_username + "," + _password + "\n");
                output.close();

                File userHome = new File(DATA_ROOT_PATH, _username);
                if (!userHome.exists()) {
                    userHome.mkdir();
                }

            } catch (IOException e) {
                Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
            }
        }

        try {

            clientObjectOutputStream.writeObject(response);

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void sendRemoteFiles() {

        File userHome = new File(DATA_ROOT_PATH, username);
        ArrayList<String> fileNames = new ArrayList<>(Arrays.asList(userHome.list()));
        ArrayList<FileInfo> files = new ArrayList<>();

        for (String fileName : fileNames) {
            FileInfo fileInfo = new FileInfo(fileName, username, username);
            if (SharedFile.isShared(fileInfo))
                continue;
            files.add(fileInfo);
        }
        files.addAll(SharedFile.getFilesSharedUser(username));

        try {
            /* Sending list size. */
            clientObjectOutputStream.writeObject(String.valueOf(files.size()));
            for (FileInfo fileInfo : files) {
                clientObjectOutputStream.writeObject(fileInfo);
            }
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void executeDownload(Command command) {

        byte[] buf = new byte[transferLength];
        FileInfo fileInfo = new FileInfo(command.getArgument(0), command.getArgument(1), "");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);
        File ownerHome, f;

        /*
         * If file is shred with/by this user, then I'll send it,
         * else it means the file owner has to be the logged in user.
         */
        if ((sharedFile != null) && (sharedFile.hasOwner(username) || sharedFile.isSharedWith(username))) {
            ownerHome = new File(DATA_ROOT_PATH, sharedFile.getOwner());
        } else {
            ownerHome = new File(DATA_ROOT_PATH, username);
        }
        f = new File(ownerHome, fileInfo.getFileName());

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

        FileInfo fileInfo = new FileInfo(command.getArgument(0), command.getArgument(2), "");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);
        File ownerHome = null;

        if (sharedFile != null) {
            if (sharedFile.hasTokenHolder(username)) {
                ownerHome = new File(DATA_ROOT_PATH, sharedFile.getOwner());
            }
        } else {
            ownerHome = new File(DATA_ROOT_PATH, username);
        }

        try {

            if (ownerHome == null) {
                clientObjectOutputStream.writeObject("denied");
                return;
            }
            clientObjectOutputStream.writeObject("granted");

            /* Start receveing the file. The received data is written to fileOut. */
            File fileOut = new File(ownerHome, command.getArgument(0));
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
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
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
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /* It sends list of users that doesn't share a specified file. */
    private void sendListOfUsers(Command command) {

        BufferedReader br = null;
        String line = "";
        String splitter = ",";
        ArrayList<String> users = new ArrayList<>();

        FileInfo fileInfo = new FileInfo(command.getArgument(0), username, "");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);
        if (sharedFile == null) {
            sharedFile = new SharedFile("", "");
        }

        try {

            br = new BufferedReader(new FileReader(USERS_CSV_FILE_PATH));
            while ((line = br.readLine()) != null) {
                String[] userInfo = line.split(splitter);
                if (!userInfo[0].equals(username) && !sharedFile.isSharedWith(userInfo[0]) && SharedFile.getUserInvite(fileInfo, userInfo[0]) == null) {
                    users.add(userInfo[0]);
                }
            }

            /* Sending list size. */
            clientObjectOutputStream.writeObject(String.valueOf(users.size()));
            for (String userName : users) {
                clientObjectOutputStream.writeObject(userName);
            }
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /* It sends list of users that doesn't or does share a specified file. */
    private void sendListOfUsersWithAccessTo(Command command) {

        BufferedReader br = null;
        String line = "";
        String splitter = ",";
        ArrayList<String> users = new ArrayList<>();
        String toExclude = username;

        FileInfo fileInfo = new FileInfo(command.getArgument(0), username, "");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);
        if (sharedFile == null) {
            sharedFile = new SharedFile("", "");
        }

        System.out.println(sharedFile.getTokenHolder());
        if (command.getArgument(1).equals("yes") && !sharedFile.getTokenHolder().isEmpty()) {
            toExclude = sharedFile.getTokenHolder();
        }

        try {

            br = new BufferedReader(new FileReader(USERS_CSV_FILE_PATH));
            while ((line = br.readLine()) != null) {
                String[] userInfo = line.split(splitter);
                if (!userInfo[0].equals(toExclude) && ((sharedFile.isSharedWith(userInfo[0]) || sharedFile.hasOwner(userInfo[0])) || (SharedFile.getUserInvite(fileInfo, userInfo[0]) != null) && !command.getArgument(1).equals("yes"))) {
                    users.add(userInfo[0]);
                }
            }

            /* Sending list size. */
            clientObjectOutputStream.writeObject(String.valueOf(users.size()));
            for (String userName : users) {
                clientObjectOutputStream.writeObject(userName);
            }
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void changeTokenHolder(Command command) {

        FileInfo fileInfo = new FileInfo(command.getArgument(0), username, "");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);
        String response = "";

        if (sharedFile != null) {
            sharedFile.setTokenHolder(command.getArgument(1));
            response = "success";
        } else {
            response = "fail";
        }

        try {
            clientObjectOutputStream.writeObject(response);
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void shareFileWith(Command command) {

        File userHome = new File(DATA_ROOT_PATH, username);
        File f = new File(userHome, command.getArgument(0));
        SharedFile sharedFile = null;

        if (f.exists()) {

            FileInfo fileInfo = new FileInfo(f.getName(), username, "");
            sharedFile = SharedFile.getSharedFile(fileInfo);

            if (sharedFile == null || !sharedFile.isSharedWith(command.getArgument(1))) {
                sharedFile = new SharedFile(command.getArgument(0), username);
                sharedFile.addReadUser(command.getArgument(1));
                ArrayList<SharedFile> userInvites = SharedFile.getUserInvites(command.getArgument(1));
                if (!userInvites.contains(sharedFile)) {
                    SharedFile.addSharedFileInvite(sharedFile);
                } else {
                    sharedFile = null;
                }
            }
        }

        try {
            if (sharedFile != null) {
                clientObjectOutputStream.writeObject("success");
                System.out.println("sent success");
            } else {
                clientObjectOutputStream.writeObject("file not found");
                System.out.println("sent file not found");
            }
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void kickUserFrom(Command command) {

        FileInfo fileInfo = new FileInfo(command.getArgument(0), username, "", "kick");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);

        if (sharedFile != null) {
            sharedFile.removeReadUser(command.getArgument(1));
            SharedFile sf = new SharedFile(sharedFile.getFileName(), sharedFile.getOwner());
            sf.addReadUser(command.getArgument(1));
            SharedFile.addSharedFileKick(sf);
        }

        if (SharedFile.removeUserInvite(fileInfo, username)) {
            SharedFile.addSharedFileKick(sharedFile);
            SharedFile sf = new SharedFile(sharedFile.getFileName(), sharedFile.getOwner());
            sf.addReadUser(command.getArgument(1));
            SharedFile.addSharedFileKick(sf);
        }
    }

    private void checkForNotification() {

        try {
            if (!SharedFile.getUserInvites(username).isEmpty()) {
                clientObjectOutputStream.writeObject("yes");
            } else if (!SharedFile.getUserKicks(username).isEmpty()) {
                clientObjectOutputStream.writeObject("yes");
            } else {
                clientObjectOutputStream.writeObject("no");
            }
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void sendUserNotifications() {


        ArrayList<SharedFile> invitations = SharedFile.getUserInvites(username);
        ArrayList<SharedFile> kicks = SharedFile.getUserKicks(username);

        try {
            /* Sending list size. */
            clientObjectOutputStream.writeObject(String.valueOf(invitations.size() + kicks.size()));
            for (SharedFile sharedFile : invitations) {
                clientObjectOutputStream.writeObject(
                        new FileInfo(sharedFile.getFileName(), sharedFile.getOwner(), "", "invite")
                );
            }
            for (SharedFile sharedFile : kicks) {
                clientObjectOutputStream.writeObject(
                        new FileInfo(sharedFile.getFileName(), sharedFile.getOwner(), "", "kick")
                );
                SharedFile.removeUserKick(sharedFile);
            }
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void processResponseToNotification(Command command) {

        FileInfo fileInfo = new FileInfo(command.getArgument(0), command.getArgument(1), "");
        SharedFile invite = SharedFile.getUserInvite(fileInfo, username);
        String response = command.getArgument(2);

        if (response.equals("accept")) {
            SharedFile.acceptInvite(invite);
        } else {
            SharedFile.declineInvite(invite);
        }
    }
}