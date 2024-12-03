
/**
 * This class stores the results of a command executed against a device
 */
public class ExecutionResult {

    // Success status of the executed command
    private boolean success;

    // If the command was successful, this is the DataPacket
    // returned by the device upon executing the command 
    private DataPacket returnedPacket;
    
    /**
     * Constructor
     * @param success
     * @param returnedPacket
     */
    public ExecutionResult(boolean success, DataPacket returnedPacket) {
        this.success = success;
        this.returnedPacket = returnedPacket;
    }

    /**
     * Getter
     * @return success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Getter
     * @return returnedPacket
     */
    public DataPacket getReturnedPacket() {
        return returnedPacket;
    }

    
}
