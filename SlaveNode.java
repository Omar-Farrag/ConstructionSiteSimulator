import java.util.HashMap;
import java.util.Map.Entry;

public class Node extends SimulationObject{

    protected uController localController;
    private int RTT_to_Control_Node; //ms
    private HashMap<String, ControlNode> subscribers;

    public Node(String object_name, String outputFileName, int runTimeStep, int RTT_to_Zone_Controller, uController locaController){
        super(object_name, outputFileName, runTimeStep);
        this.RTT_to_Control_Node = RTT_to_Zone_Controller;
        this.localController = locaController;
        subscribers = new HashMap<>();
        localController.setParentNode(this);
    }
    
    public void addSubscriber(ControlNode... nodesToSubscribe){
        for(ControlNode node : nodesToSubscribe){
            subscribers.put(node.getObject_name(),node);
        }
    }

    public void publishPacket(DataPacket packet){
        for (Entry<String,ControlNode> nodeEntry : subscribers.entrySet()) {
            try {
                Thread.sleep(RTT_to_Control_Node);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nodeEntry.getValue().update(this, packet);
        }
        exportState(String.format("Published packet to %d subscribers",subscribers.size()));
    }

    public ExecutionResult getField(ControlNode requester, String objectName, String field){
        try {
            Thread.sleep(RTT_to_Control_Node);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(localController){
            ExecutionResult result = localController.getField(objectName, field);
            exportState(String.format("[%s] Node [%s] requested field [%s] in object [%s]. Value [%s]",
                result.isSuccess() ? "SUCCESS" : "FAILURE",
                requester.getObject_name(),
                field, 
                objectName, 
                result.isSuccess() ? result.getReturnedPacket().getValue() : "Null"));
            return result;
        }
    }
    
    public ExecutionResult setField(ControlNode setter, String objectName, String field, String value){
        try {
            Thread.sleep(RTT_to_Control_Node);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(localController){
        ExecutionResult result = localController.setField(objectName, field, value);
            exportState(String.format("[%s] Node [%s] attempted updating field [%s] in object [%s]",
                result.isSuccess() ? "SUCCESS" : "FAILURE",
                setter.getObject_name(),
                field, 
                objectName));
            return result;
        }
    }

    @Override
    protected void runTimeFunction() {
        // Do Nothing
        // Node is an encapsulation of the microcontroller and its connected devices
        // The microcontroller runs a runTimeFunction
        // Node is just a bridge between other nodes and the local node controller
        // Think of it as if its a server running a REST API with two supported operations GET and POST
    }
    
}
