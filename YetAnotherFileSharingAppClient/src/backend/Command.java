/*
 * YetAnotherFileSharingAppServer - The server side.
 */

package backend;

import java.io.Serializable;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class Command implements Serializable {

    private String commandType;
    private String[] arguments;

    public Command(String commandType) {
        this.commandType = commandType;
    }

    public Command(String commandType, String[] arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }

    public boolean isRegisterNewUser() {
        return commandType.equals("registerNewUser");
    }

    public boolean isDownload() {
        return commandType.equals("download");
    }

    public boolean isUpload() {
        return commandType.equals("upload");
    }

    public boolean isRemoveFile() {
        return commandType.equals("removeFile");
    }

    public boolean isNewEmptyFile() {
        return commandType.equals("newEmptyFile");
    }

    public boolean isGetListOfRemoteFiles() {
        return commandType.equals("getListOfRemoteFiles");
    }

    public boolean isGetListOfUsers() {
        return  commandType.equals("getListOfUsers");
    }

    public boolean isShareFileWith() {
        return commandType.equals("shareFileWith");
    }

    public boolean isCheckForNotifications() {
        return  commandType.equals("checkForNotifications");
    }

    public String getArgument(int index) {
        return arguments[index];
    }
}
