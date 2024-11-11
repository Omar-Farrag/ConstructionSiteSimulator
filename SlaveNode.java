import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class SlaveNode extends SimulationObject{

    final int BLE_transmission_rate = 100; //kbps

    protected uController localController;
    private int RTT_to_Control_Node; //ms
    private HashMap<String, ControlNode> subscribers;

    public SlaveNode(String object_name, String outputFileName, int runTimeStep, int RTT_to_Zone_Controller, uController locaController){
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

        exportState(String.format("Started Publishing packet to %d subscribers",subscribers.size()));
        for (Entry<String,ControlNode> nodeEntry : subscribers.entrySet()) {
            nodeEntry.getValue().update(this, packet);
        }
        exportState(String.format("Done Publishing packet to %d subscribers",subscribers.size()));
    }

    public ExecutionResult getField(ControlNode requester, String objectName, String field){

        try {
            int time_delay= RTT_to_Control_Node /2;
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exportState(String.format("Node [%s] requested field [%s] in object [%s]",
            requester.getObject_name(),
            field, 
            objectName));

        ExecutionResult result;
        synchronized(localController){
            result = localController.getField(objectName, field);
        }

        try {
            int time_delay= RTT_to_Control_Node/2 + (result.isSuccess() ? result.getReturnedPacket().getSize() / (BLE_transmission_rate) : 0);
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;

    }
    
    public ExecutionResult setField(ControlNode setter, String objectName, String field, String value, int size){

        try {
            int time_delay= RTT_to_Control_Node /2 + size / BLE_transmission_rate;
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exportState(String.format("Node [%s] attempted updating field [%s] in object [%s]",
            setter.getObject_name(),
            field, 
            objectName));

        ExecutionResult result;
        synchronized(localController){
            result = localController.setField(objectName, field, value);
        }

        try {
            int time_delay= RTT_to_Control_Node/2;
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;

    }

    public int getRTT_to_Control_Node() {
        return RTT_to_Control_Node;
    }

    public ArrayList<String> getOfferedFields(){
        return localController.getOfferedFields();
    }

    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }
    
}
