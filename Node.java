import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Queue;

public class Node extends SimulationObject{

    private uController localController;
    private int RTT_to_Control_Node; //ms
     private HashMap<String, Node> connectedNodes;
    private Queue<DataPacket> receivedDataPackets;

    public Node(String object_name, String outputFileName, int runTimeStep, int RTT_to_Zone_Controller, uController locaController){
        super(object_name, outputFileName, runTimeStep);
        this.RTT_to_Control_Node = RTT_to_Zone_Controller;
        this.localController = locaController;
        connectedNodes = new HashMap<>();
        localController.setParentNode(this);
    }
    
    public void connectTo(Node... nodesToConnect){
        for(Node node : nodesToConnect){
            connectedNodes.put(node.getObject_name(),node);
        }
    }

    public void update(Node sender, DataPacket receivedDataPacket){
        synchronized(receivedDataPacket){
            receivedDataPackets.add(receivedDataPacket);
            exportState(String.format("Received new packet from Node %s", sender.getObject_name()));
        }
    }

    public void publishPacket(DataPacket packet){
        for (Entry<String,Node> nodeEntry : connectedNodes.entrySet()) {
            try {
                Thread.sleep(RTT_to_Control_Node);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nodeEntry.getValue().update(this, packet);
        }
    }

    public ExecutionResult getFieldFrom(String targetNodeName, String targetObjectName, String field){
        ExecutionResult result;
         
        
        Node targetNode = connectedNodes.get(targetNodeName);
        if(targetNode!= null) {
            result = targetNode.getField(this, targetObjectName, field);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Requested field [%s] from object [%s] in node [%s]. Value [%s]",successStatus, field, targetObjectName, targetNodeName, result.isSuccess()? result.getReturnedPacket().getValue():"Null"));
            return result;
        }
        else{
            exportState(String.format("[FAILURE] Requested field [%s] from object [%s] in node [%s]. Value [Null]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
    }

    public ExecutionResult setFieldIn(String targetNodeName, String targetObjectName, String field, String value){
        Node targetNode = connectedNodes.get(targetNodeName);
        if(targetNode != null){
            ExecutionResult result = targetNode.setField(this, targetObjectName, field, value);
            String successStatus = result.isSuccess() ? "SUCCESS" : "FAILURE";
            exportState(String.format("[%s] Set field [%s] in object [%s] in node [%s]", successStatus, field, targetObjectName, targetNodeName));
            return result;
        }       
        else{
            exportState(String.format("[FAILURE] Set field [%s] in object [%s] in node [%s]",field, targetObjectName, targetNodeName));
            return new ExecutionResult(false, null);
        }
        
    }

    public ExecutionResult getField(Node requester, String objectName, String field){
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
    
    public ExecutionResult setField(Node setter, String objectName, String field, String value){
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
