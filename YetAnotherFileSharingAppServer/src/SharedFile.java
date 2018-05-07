/*
 * YetAnotherFileSharingAppServer - The server side.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.FileInfo;

public class SharedFile implements Serializable {

    static private List<SharedFile> sharedFiles = Collections.synchronizedList(new ArrayList<SharedFile>());
    static private List<SharedFile> sharedFileInvites = Collections.synchronizedList(new ArrayList<SharedFile>());
    static private List<SharedFile> sharedFileKicks = Collections.synchronizedList(new ArrayList<SharedFile>());

    private String fileName;
    private String owner;
    private String tokenHolder;
    private ArrayList<String> readUsers;

    public SharedFile(String fileName, String owner, String tokenHolder) {
        this.fileName = fileName;
        this.owner = owner;
        this.tokenHolder = tokenHolder;
        readUsers = new ArrayList<String>();
    }

    public  SharedFile(String fileName, String owner) {
        this(fileName, owner, owner);
    }

    public void addReadUser(String readUser) {
        readUsers.add(readUser);
    }

    public void removeReadUser(String readUser) {
        readUsers.remove(readUser);
    }

    public String getFileName() {
        return fileName;
    }

    public String getOwner() {
        return owner;
    }

    public String getTokenHolder() {
        return tokenHolder;
    }

    public boolean hasOwner(String username) {
        return username.equals(owner);
    }

    public boolean isSharedWith(String username) {
        return readUsers.contains(username);
    }

    public boolean hasTokenHolder(String username) {
        return username.equals(tokenHolder);
    }

    public void setTokenHolder(String newTokenHolder) {
        this.tokenHolder = newTokenHolder;
    }

    public static void loadSerializedSharedFiles(String filePath) {
        loadSerializedDataToList(sharedFiles, filePath);
    }

    public static void loadSerializedInvites(String filePath) {
        loadSerializedDataToList(sharedFileInvites, filePath);
    }

    public static void loadSerializedKicks(String filePath) {
        loadSerializedDataToList(sharedFileKicks, filePath);
    }

    public static void loadSerializedDataToList(List<SharedFile> list, String filePath) {

        FileInputStream fis;
        ObjectInputStream objectInputStream;

        try {

            fis = new FileInputStream(filePath);
            objectInputStream = new ObjectInputStream(fis);

            while (fis.available() != -1) {
                SharedFile sf = (SharedFile) objectInputStream.readObject();
                list.add(sf);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void serializeSharedFilesToBinaryFile(String filePath) {
        serializeListToBinaryFile(sharedFiles, filePath);
    }

    public static void serializeInvitessToBinaryFile(String filePath) {
        serializeListToBinaryFile(sharedFileInvites, filePath);
    }

    public static void serializeKicksToBinaryFile(String filePath) {
        serializeListToBinaryFile(sharedFileKicks, filePath);
    }

    public static void serializeListToBinaryFile(List<SharedFile> list, String filePath) {

        FileOutputStream fos;
        ObjectOutputStream objectOutputStream;

        try {

            fos = new FileOutputStream(filePath);
            objectOutputStream = new ObjectOutputStream(fos);

            for (SharedFile sf : list) {
                objectOutputStream.writeObject(sf);
                objectOutputStream.reset();
            }
            objectOutputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addNewSharedFile(SharedFile sharedFile) {
        sharedFiles.add(sharedFile);
    }

    public static void addSharedFileInvite(SharedFile sharedFile) {
        sharedFileInvites.add(sharedFile);
    }

    public static void addSharedFileKick(SharedFile sharedFile) {
        sharedFileKicks.add(sharedFile);
    }

    public static boolean isShared(FileInfo fileInfo) {
        for (SharedFile sharedFile : sharedFiles) {
            if (fileInfo.getFileName().equals(sharedFile.getFileName())) {
                if (fileInfo.getOwner().equals(sharedFile.getOwner())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static SharedFile getSharedFile(FileInfo fileInfo) {
        for (SharedFile sharedFile : sharedFiles) {
            if (fileInfo.getFileName().equals(sharedFile.getFileName())) {
                if (fileInfo.getOwner().equals(sharedFile.getOwner())) {
                    return sharedFile;
                }
            }
        }

        return null;
    }

    public static SharedFile getUserInvite(FileInfo fileInfo, String username) {
        ArrayList<SharedFile> invites = SharedFile.getUserInvites(username);
        for (SharedFile invite : invites) {
            if (fileInfo.getFileName().equals(invite.getFileName())) {
                if (fileInfo.getOwner().equals(invite.getOwner())) {
                    return invite;
                }
            }
        }

        return null;
    }

    public static boolean removeUserInvite(FileInfo fileInfo, String username) {

        boolean booleanRet = false;
        SharedFile invite = new SharedFile(fileInfo.getFileName(), fileInfo.getOwner());

        invite.addReadUser(username);
        if (sharedFileInvites.contains(sharedFileInvites)) {
            sharedFileInvites.remove(invite);
            booleanRet = true;
        }

        return booleanRet;
    }

    public static void removeUserKick(SharedFile sharedFile) {
        sharedFileKicks.remove(sharedFile);
    }

    public static void acceptInvite(SharedFile invite) {
        FileInfo fileInfo = new FileInfo(invite.getFileName(), invite.getOwner(), "");
        SharedFile sharedFile = SharedFile.getSharedFile(fileInfo);

        if (sharedFile == null) {
            sharedFile = new SharedFile(fileInfo.getFileName(), fileInfo.getOwner());
            SharedFile.addNewSharedFile(sharedFile);
        }
        sharedFile.addReadUser(invite.readUsers.get(0));
        sharedFileInvites.remove(invite);
    }

    public static void declineInvite(SharedFile invite) {
        sharedFileInvites.remove(invite);
    }

    /* Returns a list of all the files shared with or by the user - username. */
    public static ArrayList<FileInfo> getFilesSharedUser(String username) {

        ArrayList<FileInfo> filesSharedUser = new ArrayList<>();

        for (SharedFile sharedFile : sharedFiles) {
            if (sharedFile.hasOwner(username) || sharedFile.isSharedWith(username)) {
                FileInfo fileInfo =
                        new FileInfo(sharedFile.getFileName(), sharedFile.getOwner(), sharedFile.getTokenHolder());
                filesSharedUser.add(fileInfo);
            }
        }

        return filesSharedUser;
    }

    public static ArrayList<SharedFile> getUserInvites(String username) {

        ArrayList<SharedFile> userInvites = new ArrayList<>();

        for (SharedFile sharedFileInvite : sharedFileInvites) {
            if (sharedFileInvite.isSharedWith(username)) {
                userInvites.add(sharedFileInvite);
            }
        }

        return userInvites;
    }

    public static ArrayList<SharedFile> getUserKicks(String username) {

        ArrayList<SharedFile> userKicks = new ArrayList<>();

        for (SharedFile sharedFileKick : sharedFileKicks) {
            if (sharedFileKick.isSharedWith(username)) {
                userKicks.add(sharedFileKick);
            }
        }

        return userKicks;
    }


}
