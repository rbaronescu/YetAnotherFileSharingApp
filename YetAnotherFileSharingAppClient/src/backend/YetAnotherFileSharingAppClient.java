/*
 * YetAnotherFileSharingAppClient - The client side.
 */
package backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class YetAnotherFileSharingAppClient {
    
    /* 10 Mebibytes (the new Megabyte) */
    private static final int TRANSFER_LENGTH = 8192;
    
    private static final String USERNAME_NEW_USER_REQUEST = "newUserUsername";
    private static final String PASSWORD_NEW_USER_REQUEST = "newUserPassword";
    
    private String username = "";
    private Socket socket;
    private InputStream serverInputStream;
    private OutputStream serverOutputStream;
    private ObjectInputStream serverObjectInputStream;
    private ObjectOutputStream serverObjectOutputStream;
    
    /* Used to synchronize requests to server. */
    private ReentrantLock lock = new ReentrantLock();    
    
    public boolean handleClientLogin(String username, String password) {
        
        boolean ret = false;
        
        if (username.isEmpty())
            return false;
        
        try {
            
            /* Initiationg communication with the server. */
            socket = new Socket("localhost", 5151);
            serverInputStream = socket.getInputStream();
            serverOutputStream = socket.getOutputStream();
            serverObjectInputStream = new ObjectInputStream(serverInputStream);
            serverObjectOutputStream = new ObjectOutputStream(serverOutputStream);
            
            /* Sneding credentials. */
            serverObjectOutputStream.writeObject(username);
            serverObjectOutputStream.writeObject(password);
            
            /* Receiving response to login request. */
            String result = (String) serverObjectInputStream.readObject();
            if (result.equals("true")) {
                this.username = username;
                ret = true;
            } else {
                this.closeConnection();
            }
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        if (ret == true) {
            
        }
        
        return ret;
    }
    
    public static boolean handleRegisterNewUser(String username, String password) {
        
        Socket tempSocket;
        InputStream tempServerInputStream;
        OutputStream tempServerOutputStream;
        ObjectInputStream tempServerObjectInputStream;
        ObjectOutputStream tempServerObjectOutputStream;
        String result = "fail";
        
        if (username.isEmpty())
            return false;
        
        try {
            
            /* Initiatinig communication with the server. */
            tempSocket = new Socket("localhost", 5151);
            tempServerInputStream = tempSocket.getInputStream();
            tempServerOutputStream = tempSocket.getOutputStream();
            tempServerObjectInputStream = new ObjectInputStream(tempServerInputStream);
            tempServerObjectOutputStream = new ObjectOutputStream(tempServerOutputStream);
            
            /* Sneding credentials. */
            tempServerObjectOutputStream.writeObject(USERNAME_NEW_USER_REQUEST);
            tempServerObjectOutputStream.writeObject(PASSWORD_NEW_USER_REQUEST);
            
            /* Sending the command to the server. */
            String commandType = "registerNewUser";
            String[] arguments = {username, password};
            tempServerObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            /* Receiving response to login request. */
            result = (String) tempServerObjectInputStream.readObject();
            
            tempSocket.close();
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return result.equals("success");
    }
    
    public FileInfo[] getRemoteFilesInfo() {
        
        int numberOfFiles;
        FileInfo[] files = null;

        try {
            
            /* Sending the command to the server. */
            lock.lock();
            String commandType = "getListOfRemoteFiles";
            serverObjectOutputStream.writeObject(new Command(commandType, null));
            
            /* Receiving number of remote files. */
            numberOfFiles = Integer.parseInt((String) serverObjectInputStream.readObject());
            
            files = new FileInfo[numberOfFiles];
            for (int i = 0; i < numberOfFiles; i++) {
                files[i] = (FileInfo) serverObjectInputStream.readObject();
            }
            
            Arrays.sort(files, FileInfo.FileInfoNameComparator);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return files;
    }
    
    public boolean downloadFile(String fileInName, String fileOwner) {
        
        String response;
        boolean booleanRet = false;
        
        /* Amount of data transfered between client and server. */
        byte[] receivedData = new byte[TRANSFER_LENGTH];
        
        /* Opening and creating user home if it does not exist. */
        File userHome = new File("data", username);
        if (!userHome.exists()) {
            userHome.mkdir();
        }
        
        try {
            
            /* Telling the server, the command that the client wants. */
            lock.lock();
            String commandType = "download";
            String[] arguments = {fileInName, fileOwner};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (response.equals("file not found")) {
                //lock.unlock();
                return false;
            }
            
            /* Start receveing the file. The downloaded data is written to fileOut. */
            File fileOut = new File(userHome, fileInName);
            if (!fileOut.exists()) {
                fileOut.createNewFile();
            }
            
            InputStream in = serverInputStream;
            FileOutputStream out = new FileOutputStream(fileOut);
            
            long totalBytesToReceive = Long.parseLong(response);
            long totalBytesReceived = 0;
            int bytesReceived = 0;
            while (totalBytesReceived < totalBytesToReceive) {
                bytesReceived = in.read(receivedData);
                out.write(receivedData, 0, bytesReceived);
                totalBytesReceived += bytesReceived;
            }
            
            out.close();
            booleanRet = true;
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.INFO,
                    "file {0} received", fileInName);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return booleanRet;
    }
    
    public boolean uploadFile(File f) {
        
        boolean booleanRet = false;
        String response;
        
         /* Amount of data transfered between client and server. */
        byte[] buf = new byte[TRANSFER_LENGTH];
        
        try {
            
            /* Sending command to server. */
            lock.lock();
            String commandType = "upload";
            String[] arguments = {f.getName(), String.valueOf(f.length()), username};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (response.equals("denied")) {
                //lock.unlock();
                return false;
            }
            
            InputStream in = new FileInputStream(f);
            OutputStream out = serverOutputStream;
            
            int bytesRead;
            while((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
            
            in.close();
            booleanRet = true;
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return booleanRet;
    }
    
    public boolean uploadFile(String fileName, String fileOwner) {
        
        boolean booleanRet = false;
        String response;
        
         /* Amount of data transfered between client and server. */
        byte[] buf = new byte[TRANSFER_LENGTH];
        
        /* Opening and creating user home if it does not exist. */
        File userHome = new File("data", username);
        if (!userHome.exists()) {
            return false;
        }
        
        /* Opening the file to upload. */
        File f = new File(userHome, fileName);
        if (!f.exists()) {
            return false;
        }
        
        try {
            
            /* Sending command to server. */
            lock.lock();
            String commandType = "upload";
            String[] arguments = {fileName, String.valueOf(f.length()), fileOwner};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (response.equals("denied")) {
                //lock.unlock();
                return false;
            }
            
            InputStream in = new FileInputStream(f);
            OutputStream out = serverOutputStream;
            
            int bytesRead;
            while((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
            
            in.close();
            booleanRet = true;
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.INFO,
                    "file {0} uploaded", fileName);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return booleanRet;
    }
    
    public boolean editRemoteFile(String fileName, String fileOwner) {
        
        /* Downloading file for editing. */
        if (!downloadFile(fileName, fileOwner)) {
            return false;
        }
        
        /* Opening user home directory. */
        File userHome = new File("data", username);
        if (!userHome.exists()) {
            return false;
        }
        
        /* Opening download file. */
        File file = new File(userHome, fileName);
        if (!file.exists()) {
            return false;
        }
        
        /* Opening file in editor and wait for it to be closed. */
        try {
            String[] cmd = {"cmd.exe", "/C", "start /wait \"", file.getPath(), "\""};
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.INFO,
                    "file {0} closed", file.getName());
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        /* Uploading modifications in file. */
        if (!this.uploadFile(file.getName(), fileOwner))
            return false;
        
        return file.delete();
    }
    
    public boolean removeFile(String fileName) {
        
        String response;
        
        try {
            
            /* Telling the server, the command that the client wants. */
            lock.lock();
            String commandType = "removeFile";
            String[] arguments = {fileName};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (!response.equals("file not found")) {
                //lock.unlock();
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return false;
    }
    
    public boolean newEmptyFile(String fileName) {
        
        String response;
        
        try {
            
            lock.lock();
            String commandType = "newEmptyFile";
            String[] arguments = {fileName};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (response.equals("success")) {
                //lock.unlock();
                return true;
            }
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return false;
    }
    
    public String[] getListOfUsers(String fileName) {
        
        int numberOfUsers;
        String[] users = null;

        try {
            
            /* Sending the command to the server. */
            lock.lock();
            String commandType = "getListOfUsers";
            String[] arguments = {fileName};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            /* Receiving number of users. */
            numberOfUsers = Integer.parseInt((String) serverObjectInputStream.readObject());
            
            users = new String[numberOfUsers];
            for (int i = 0; i < numberOfUsers; i++) {
                users[i] = (String) serverObjectInputStream.readObject();
            }
            
            Arrays.sort(users);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return users;
    }
    
    public boolean ShareFileWith(String fileName, String username) {
        
        String response = "";
        
        try {
            
            /* Sending the command to the server. */
            lock.lock();
            String commandType = "shareFileWith";
            String[] arguments = {fileName, username};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            /* Receiving response. */
            response = (String) serverObjectInputStream.readObject();
        
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();

        }
        
        return response.equals("success");
    }
    
    public int hasNotifications() {
        
        String response = "";
        int ret = -1;
        boolean lockAcquired = false;
        
        try {
            if (lock.tryLock()) {
                
                lockAcquired = true;
                /* Sending the command to the server. */
                String commandType = "checkForNotifications";
                serverObjectOutputStream.writeObject(new Command(commandType, null));
            
                /* Receiving response. */
                response = (String) serverObjectInputStream.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (lockAcquired) {
                lock.unlock();
            }
        }
        
        if (response.equals("yes")) {
            ret = 1;
        } else if (response.equals("no")) {
            ret = 0;
        }
        
        return ret;
    }
    
    public FileInfo[] getUserInvitations() {
        
        int numberOfInvitations;
        FileInfo[] files = null;

        try {
            
            /* Sending the command to the server. */
            lock.lock();
            String commandType = "getUserInvitations";
            serverObjectOutputStream.writeObject(new Command(commandType, null));
            
            /* Receiving number of remote files. */
            numberOfInvitations = Integer.parseInt((String) serverObjectInputStream.readObject());
            
            files = new FileInfo[numberOfInvitations];
            for (int i = 0; i < numberOfInvitations; i++) {
                files[i] = (FileInfo) serverObjectInputStream.readObject();
            }
            
            Arrays.sort(files, FileInfo.FileInfoNameComparator);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
        
        return files;
    }
    
    /* @operation may be "accept" or "decline". */
    public void respondToNotification(String fileName, String owner, String operation) {
        
        try {
            /* Sending the command to the server. */
            lock.lock();
            String commandType = "respondToNotify";
            String[] arguments = {fileName, owner, operation};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            lock.unlock();
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public void closeConnection() {
        try {
            this.socket.close();
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
