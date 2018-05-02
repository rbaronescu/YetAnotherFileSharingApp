/*
 * YetAnotherFileSharingAppServer - The server side.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import backend.FileInfo;

public class SharedFile {

    private static final String DATA_ROOT_PATH = "data";
    static private final String SHARED_FILES_CSV_FILE_PATH = DATA_ROOT_PATH + "/sharedFiles.csv";
    static private List<SharedFile> sharedFiles = Collections.synchronizedList(new ArrayList<SharedFile>());
    static private List<SharedFile> sharedFileInvites = Collections.synchronizedList(new ArrayList<SharedFile>());

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

    public static void fillSharedFilesList() {

        BufferedReader br;
        String line, splitter = ",";

        try {

            br = new BufferedReader(new FileReader(SHARED_FILES_CSV_FILE_PATH));
            while ((line = br.readLine()) != null) {

                String[] tokens = line.split(splitter);
                SharedFile sharedFile = new SharedFile(tokens[0], tokens[1], tokens[2]);

                for (int i = 3; i < tokens.length; i++) {
                    sharedFile.addReadUser(tokens[i]);
                }

                /* Adding this new Shared File to the list. */
                addNewSharedFile(sharedFile);
            }

        } catch (IOException e) {
            Logger.getLogger(YetAnotherFileSharingAppServer.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public static void addNewSharedFile(SharedFile sharedFile) {
        sharedFiles.add(sharedFile);
    }

    public static void addSharedFileInvite(SharedFile sharedFile) {
        sharedFileInvites.add(sharedFile);
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
}
