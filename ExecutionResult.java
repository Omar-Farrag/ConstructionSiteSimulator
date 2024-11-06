public class ExecutionResult {

    private boolean success;
    private DataPacket returnedPacket;
    
    public ExecutionResult(boolean success, DataPacket returnedPacket) {
        this.success = success;
        this.returnedPacket = returnedPacket;
    }

    public boolean isSuccess() {
        return success;
    }
    public DataPacket getReturnedPacket() {
        return returnedPacket;
    }

    
}
