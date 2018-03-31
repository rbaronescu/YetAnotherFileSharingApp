/*
 * YetAnotherFileSharingAppClient - The client side.
 */
package backend;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class YetAnotherFileSharingAppClient {
    
    /* 10 Mebibytes (the new Megabyte) */
    static final int transferLength = 8192;
    
    String username = "";
    Socket socket;
    InputStream serverInputStream;
    OutputStream serverOutputStream;
    ObjectInputStream serverObjectInputStream;
    ObjectOutputStream serverObjectOutputStream;
    
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
            }
            
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ret;
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
            
        } catch (IOException ex) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return fileNames;
    }

    public String[] getLocalFilesInfo() {

        String localUserHomePath = "data/" + username;
        File localUserHome = new File(localUserHomePath);
        
        if (!localUserHome.exists())
            return new String[0];
        
        ArrayList<String> localFileNames = new ArrayList<>(Arrays.asList(localUserHome.list()));
        String[] fileNamesArray = new String[localFileNames.size()];

        for (int i = 0; i < localFileNames.size(); i++) {
            fileNamesArray[i] = localFileNames.get(i);
        }

        Arrays.sort(fileNamesArray);

        return fileNamesArray;
    }
    
    public boolean downloadFile(String fileInName) {
        
        String response = "";
        boolean booleanRet = false;
        
        /* Amount of data transfered between client and server. */
        byte[] receivedData = new byte[transferLength];
        
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
                    "File " + fileInName + "received!");
            
        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException e) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, e);
        }
        
        return booleanRet;
    }
    
    public boolean uploadFile(String fileName) {
        
        boolean booleanRet = false;
        
         /* Amount of data transfered between client and server. */
        byte[] buf = new byte[transferLength];
        
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
            
        } catch (IOException ex) {
            Logger.getLogger(YetAnotherFileSharingAppClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return booleanRet;
    }
}
