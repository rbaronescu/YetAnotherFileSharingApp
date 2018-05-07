/*
 * YetAnotherFileSharingAppServer - The server side.
 */
package backend;

import java.io.Serializable;
import java.util.Comparator;

public class FileInfo implements Comparable<FileInfo>, Serializable {

    private String fileName;
    private String owner;
    private String tokenHolder;
    private String type;

    public FileInfo(String fileName, String owner, String tokenHolder, String type) {
        this.fileName = fileName;
        this.owner = owner;
        this.tokenHolder = tokenHolder;
        this.type = type;
    }

    public FileInfo(String fileName, String owner, String tokenHolder) {
        this.fileName = fileName;
        this.owner = owner;
        this.tokenHolder = tokenHolder;
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

    public boolean isInvite() {
        return type.equals("invite");
    }

    public boolean isKick() {
        return type.equals("kick");
    }

    public int compareTo(FileInfo fileInfo) {
        return this.getFileName().compareTo(fileInfo.getFileName());
    }

    public static Comparator<FileInfo> FileInfoNameComparator
            = new Comparator<FileInfo>() {
        public int compare(FileInfo fileInfo1, FileInfo fileInfo2) {
            return fileInfo1.getFileName().compareToIgnoreCase(fileInfo2.getFileName());
        }
    };
}
