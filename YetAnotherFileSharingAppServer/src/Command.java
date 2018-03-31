/*
 * YetAnotherFileSharingAppClient - The client side.
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

    public boolean isDownload() {
        return commandType.equals("download");
    }

    public boolean isUpload() {
        return commandType.equals("upload");
    }

    public boolean isGetListOfRemoteFiles() {
        return commandType.equals("getListOfRemoteFiles");
    }

    public String getArgument(int index) {
        return arguments[index];
    }
}
