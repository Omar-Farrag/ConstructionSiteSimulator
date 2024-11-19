import java.util.ArrayList;

public class SlaveNode extends SimulationObject{

    final int BLE_transmission_rate = 100; //kbps

    protected uController localController;
    protected int RTT_to_Control_Node; //ms
    private ControlNode controlNode;

    public SlaveNode(String object_name, int runTimeStep, int RTT_to_Zone_Controller, uController locaController){
        super(object_name, runTimeStep);
        this.RTT_to_Control_Node = RTT_to_Zone_Controller;
        this.localController = locaController;
        localController.setParentNode(this);
    }

    public void initFields(){
        localController.initFields();
    }
    
    public void setControlNode(ControlNode controlNode){
        this.controlNode = controlNode;
    }

    public void publishPacket(DataPacket... packets){
        exportState(String.format("Started Publishing (%d) packets to Control Node [%s]",packets.length, controlNode.getObject_name()));
        controlNode.update(this,packets);
        exportState(String.format("Done Publishing (%d) packets to Control Node [%s]",packets.length, controlNode.getObject_name()));
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
            int time_delay= RTT_to_Control_Node/2 + (result.isSuccess() ? result.getReturnedPacket().getSize() * 8 / (BLE_transmission_rate) : 0);
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;

    }
    
    public ExecutionResult setField(ControlNode setter, String objectName, String field, String value, int size){

        try {
            int time_delay= RTT_to_Control_Node /2 + size * 8 / BLE_transmission_rate;
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

    
    public ExecutionResult updateSwitch(ControlNode setter, String objectName, String position, String switchStatus) {
        
        try {
            int time_delay= RTT_to_Control_Node /2 + 8 / BLE_transmission_rate;
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        exportState(String.format("Node [%s] attempted updating switch position [%s] in object [%s]",
            setter.getObject_name(),
            position, 
            objectName));

        ExecutionResult result;
        synchronized(localController){
            result = localController.updateSwitch(objectName, position, switchStatus);
        }

        try {
            int time_delay= RTT_to_Control_Node/2;
            Thread.sleep(time_delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;       
    }

    public boolean isPermittedToEnter(String id){
        exportState(String.format("Asked Control Node [%s] about ID [%s]'s permission",controlNode.getObject_name(),id));
        boolean permitted = controlNode.isPermittedToEnter(this, id);
        exportState(String.format("ID [%s]'s permission: [%s]", id, permitted? "ALLOWED": "DENIED"));
        return permitted;
    }

    public int getRTT_to_Control_Node() {
        return RTT_to_Control_Node;
    }

    public ArrayList<String> getOfferedFields(){
        return localController.getOfferedFields();
    }

    public ControlNode getControlNode() {
        return controlNode;
    }
    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }

    @Override
    public void start() {
        localController.start();
        super.start();
    }

    @Override
    public void terminate() {
        localController.terminate();
        super.terminate();
    }
    
}
