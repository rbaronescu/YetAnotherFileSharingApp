/*
 * YetAnotherFileSharingAppServer - The server side.
 */

import java.util.ArrayList;

public class SharedFile {

    private String fileName;
    private String owner;
    private String writeUser;
    private ArrayList<String> readUsers;

    public SharedFile(String owner, String writeUser) {
        this.owner = owner;
        this.writeUser = writeUser;
    }

    public void addReadUser(String readUser) {
        readUsers.add(readUser);
    }
}
