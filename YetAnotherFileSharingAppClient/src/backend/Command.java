/*
 * YetAnotherFileSharingAppClient - The client side.
 */
package backend;

/**
 *
 * @author Robert Baronescu, <baronescu.robert@gmail.com>
 */
public class Command {
    
    String commandType;
    String[] arguments;
    
    public Command(String commandType, String[] arguments) {
        this.commandType = commandType;
        this.arguments = arguments;
    }
}
