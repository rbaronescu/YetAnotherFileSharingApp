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
    
    public String[] getRemoteFilesInfo() {
        
        int numberOfFiles;
        String[] fileNames = null;
        
        try {
            
            /* Sending the command to the server. */
            String commandType = "getListOfRemoteFiles";
            serverObjectOutputStream.writeObject(new Command(commandType, null));
            
            /* Receiving number of remote files. */
            numberOfFiles = Integer.parseInt((String) serverObjectInputStream.readObject());
            
            fileNames = new String[numberOfFiles];
            for (int i = 0; i < numberOfFiles; i++) {
                fileNames[i] = (String) serverObjectInputStream.readObject();
            }
            
            Arrays.sort(fileNames);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return fileNames;
    }
    
    public boolean downloadFile(String fileInName) {
        
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
            String commandType = "download";
            String[] arguments = {fileInName};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (response.equals("file not found")) {
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
                    "File {0}received!", fileInName);
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return booleanRet;
    }
    
    public boolean uploadFile(File f) {
        
        boolean booleanRet = false;
        
         /* Amount of data transfered between client and server. */
        byte[] buf = new byte[TRANSFER_LENGTH];
        
        try {
            
            /* Sending command to server. */
            String commandType = "upload";
            String[] arguments = {f.getName(), String.valueOf(f.length())};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            InputStream in = new FileInputStream(f);
            OutputStream out = serverOutputStream;
            
            int bytesRead;
            while((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
            
            in.close();
            booleanRet = true;
            
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return booleanRet;
    }
    
    public boolean uploadFile(String fileName) {
        
        boolean booleanRet = false;
        
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
            String commandType = "upload";
            String[] arguments = {fileName, String.valueOf(f.length())};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            InputStream in = new FileInputStream(f);
            OutputStream out = serverOutputStream;
            
            int bytesRead;
            while((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
            
            in.close();
            booleanRet = true;
            
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return booleanRet;
    }
    
    public boolean editRemoteFile(String fileName) {
        
        /* Downloading file for editing. */
        if (!downloadFile(fileName)) {
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
        } catch (IOException | InterruptedException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        /* Uploading modifications in file. */
        if (!this.uploadFile(file.getName()))
            return false;
        
        /* Deleting local file. */
        return file.delete();
    }
    
    public boolean removeFile(String fileName) {
        
        String response;
        
        try {
            
            /* Telling the server, the command that the client wants. */
            String commandType = "removeFile";
            String[] arguments = {fileName};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (!response.equals("file not found")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return false;
    }
    
    public boolean newEmptyFile(String fileName) {
        
        String response;
        
        try {
            
            String commandType = "newEmptyFile";
            String[] arguments = {fileName};
            serverObjectOutputStream.writeObject(new Command(commandType, arguments));
            
            response = (String) serverObjectInputStream.readObject();
            if (response.equals("success"))
                return true;
            
        } catch (IOException | ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return false;
    }
    
    public void closeConnection() {
        try {
            this.socket.close();
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
