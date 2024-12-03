import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class represents a slave zone on a construction site
 */
public class SlaveZone extends Zone {

    // Gate node representing the gate of the zone
    protected SlaveNode gateNode;

    // Actuation node representing the zone's actuation node
    protected SlaveNode actuationNode;

    // Buzzer node representing the zone's buzzer node
    protected SlaveNode buzzerNode;

    // Speaker node representing the zone's speaker node
    protected SlaveNode speakerNode;

    // Reference to the actuationNode's internal relay.
    private Relay relay;

    // Round trip time in ms between the zone's slave nodes and master node.
    // It is assumed that all slave nodes have the same RTT to the master node
    private int RTT_to_Master_Node;

    // List of other slave nodes in the zone in addition to the nodes mentioned above.
    // This allows the user to add new slave nodes to the slave zone to customize it. 
    protected ArrayList<SlaveNode> otherSlaveNodes;

    /** 
     * Constructor
     * @param object_name Name of the slave zone
     * @param runTimeStep Timestep in ms of the zone and its internal components's simulation lifetimes
     * @param masterNodeLoop Control algorithm to run on the zone's master node uController
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     */
    public SlaveZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, int RTT_to_Master_Node){
        super(object_name, runTimeStep, masterNodeLoop, null);
        this.RTT_to_Master_Node = RTT_to_Master_Node;
        this.otherSlaveNodes = new ArrayList<>();
        init();
        
    }

    /** 
     * Constructor
     * @param object_name Name of the slave zone
     * @param runTimeStep Timestep in ms of the zone and its internal components's simulation lifetimes
     * @param masterNodeLoop Control algorithm to run on the zone's master node uController
     * @param masterNodeSetup Setup algorithm that runs once on the zone's master node uController at the beginning
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     */
    public SlaveZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, ProcessingAlgorithm masterNodeSetup, int RTT_to_Zone_Controller){
        super(object_name, runTimeStep,masterNodeLoop, masterNodeSetup);
        this.RTT_to_Master_Node = RTT_to_Zone_Controller;
        this.otherSlaveNodes = new ArrayList<>();
        init();
    }

    /**
     * Function to initailize the nodes of the zone
     */
    private void init() {

        // Create the nodes
        gateNode = createGateNode();
        actuationNode = createActuationNode();
        buzzerNode = createBuzzerNode();
        speakerNode = createSpeakerNode();
        masterNode = createMasterNode();

        // Subscribe the master node to each of the other slave nodes.
        // This ensures that the slave nodes will regularly send status update
        // packets to the master node
        masterNode.subscribeTo(gateNode);
        masterNode.subscribeTo(actuationNode);
        masterNode.subscribeTo(buzzerNode);
        masterNode.subscribeTo(speakerNode);
    }


    /**
     * Function to connect a high power device to one of the switches on the actuationNode's relay 
     * @param dev HighPowerDevice to be connected
     * @param position Position in the actuationNode's relay to which the device will be connected
     * @return true if the device was connected successfully. False if the position was invalid or if there is a device
     *         connected at that position.
     */
    public boolean connectToActuationNode(HighPowerDevice dev, int position){
        return relay.connectTo(dev, position);
    }

    /**
     * Function to add a new slave node to the zone
     * @param otherSlaveNode New slave node to add to the zone
     */
    public void addSlaveNode(SlaveNode otherSlaveNode){
        // Add the node to the list of other slave nodes in the zone
        otherSlaveNodes.add(otherSlaveNode);

        // Subscribe the master node to the newly added slave node
        masterNode.subscribeTo(otherSlaveNode);

    }
    /**
     * Function to add multipel slave nodes to the zone
     * @param otherSlaveNodes Array of slave nodes to add to the zone
     */
    public void addAllSlaveNodes(ArrayList<SlaveNode> otherSlaveNodes){
        for (SlaveNode node : otherSlaveNodes) addSlaveNode(node);
    }

    /**
     * Function to add the ID of a worker who is permitted to enter the zone
     * @param id ID of worker to be added to list of workers permitted to enter the zone
     */
    public void addPermittedId(String id){
        masterNode.addPermittedId(id);
    }
    
    /**
     * Function to set up the gate node
     * @return Fully created gate node
     */
    private SlaveNode createGateNode(){
    
        // Get full name for gate node 
        String nodeName = getFullName("GateNode");
        
        // Initialize a new gate node as a SlaveNode
        SlaveNode gateNode = new Gate(nodeName, runTimeStep, RTT_to_Master_Node); 
        
        // Set the master node of the gate node to be the master node in this class
        gateNode.setMasterNode(masterNode);
        
        // return the gate node
        return gateNode;
        
    }

    /**
     * Function to set up the actuation node
     * @return Fully created actuation node
     */
    private SlaveNode createActuationNode(){

        // Get full name of the actuator node
        String nodeName = getFullName("ActuatorNode");

        // Prepare full name of the actuator device to be part of the actuator node
        String relayName = nodeName + "_relay";

        // Prepare full name of the uController to be part of the actuator node
        String controllerName = nodeName + "_controller";

        // Initialize the actuator relay
        relay = new Relay(relayName, runTimeStep, 4);

        // Initialize the node's uController including the algorithm running on it
        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{
            //uController continuous algorithm.

            // Controller should continuously query all fields. Since we connected the uController will be connected to 
            // the actuator only, getLocalOfferedFields will return fields belonging to the actuator relay alone
            ArrayList<ExecutionResult> results = new ArrayList<>();
            for(String field: cont.getLocalOfferedFields()) results.add(cont.getField(relayName, field));

            // Add the data packets of successfull results to an array to be published
            ArrayList<DataPacket> toPublish = new ArrayList<>();
            for (ExecutionResult result : results) if(result.isSuccess()) toPublish.add(result.getReturnedPacket());

            // Publish the data packets of successful results to the master node
            cont.publishPacket(toPublish.toArray(new DataPacket[0]));

        });

        // Connect the uController to the actuator
        controller.connectTo(relay);

        // Initialize the actuator node
        SlaveNode node = new SlaveNode(nodeName, runTimeStep, RTT_to_Master_Node, controller);
        
        // Set the node's master node to be the master node in the class
        gateNode.setMasterNode(masterNode);

        // Return the fully created actuationNode
        return node;

    }
    
    /**
     * Function to set up the buzzer node
     * @return fully created buzzer node
     */
    private SlaveNode createBuzzerNode(){

        // Get full name of buzzer node
        String nodeName = getFullName("BuzzerNode");

        // Prepare names of the relay, uController, and buzzer in the node
        String relayName = nodeName + "_relay";
        String controllerName = nodeName + "_controller";
        String buzzerName =relayName + "_buzzer";

        // Create a buzzer
        HighPowerDevice buzzer = new HighPowerDevice(buzzerName, runTimeStep);
        
        // Initialize a relay with a single switch 
        Relay relay = new Relay(relayName, runTimeStep, 1);

        // Connect the relay to the buzzer
        relay.connectTo(buzzer, 0);

        // Initialize a new uController
        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{
            // uController continuous algorithm
            
            // Prepare packets containing fields and their values of the relay to be published to the master node.
            ArrayList<DataPacket> packets = new ArrayList<>();
            
            // For every field in the all the devices connected to uController (only relay is connected)
            for(String field : cont.getLocalOfferedFields()) {
                // Get the value of the field
                ExecutionResult result = cont.getField(relayName, field);
                
                // Add the returned data packet to the list of packets to publish if the result was successful
                if(result.isSuccess()) packets.add(result.getReturnedPacket());
            }

            // Publish the prepared packets to the master node
            cont.publishPacket(packets.toArray(new DataPacket[0]));

        });

        // Connect the uController to the relay
        controller.connectTo(relay);

        // Initialize the buzzer node
        SlaveNode node = new SlaveNode(nodeName,runTimeStep, RTT_to_Master_Node, controller);
        
        // Set the master node of the buzzer node to be the master node in this class
        gateNode.setMasterNode(masterNode);

        // Returned the created buzzer node
        return node;
    }

    
    
    private SlaveNode createSpeakerNode(){
        String nodeName = getFullName("SpeakerNode");

        String controllerName = nodeName + "_controller";
        String speakerName = nodeName + "_speaker";

        if(!new File(Device.getInputFileName(speakerName)).exists())
        { 
            try (FileWriter fileWriter = new FileWriter(Device.getInputFileName(speakerName));
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("Played Message");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        LowPowerDevice speaker = new LowPowerDevice(speakerName, runTimeStep,100);

        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{
            ExecutionResult result1 = cont.getField(speakerName, "Played Message");
            if(result1.isSuccess()) cont.publishPacket(result1.getReturnedPacket());
        });
        controller.connectTo(speaker);

        SlaveNode node = new SlaveNode(nodeName, runTimeStep, RTT_to_Master_Node, controller);
        gateNode.setMasterNode(masterNode);

        return node;

    }

    @Override
    protected MasterNode createMasterNode(){
        String nodeName = getFullName("MasterNode");

        String controllerName = nodeName + "_controller";
        String cameraName = nodeName + "_camera";
        String gatewayName = nodeName + "_gateway";

        if(!new File(Device.getInputFileName(cameraName)).exists())
        { 
            try (FileWriter fileWriter = new FileWriter(Device.getInputFileName(cameraName));
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("Video");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LowPowerDevice camera = new LowPowerDevice(cameraName, runTimeStep,3500000);
        gateway = new Gateway(gatewayName, runTimeStep);

        uController controller;
        if(masterNodeSetup!= null) controller = new uController(controllerName, runTimeStep, masterNodeLoop, masterNodeSetup);
        else controller = new uController(controllerName, runTimeStep, masterNodeLoop);

        controller.connectTo(camera);
        controller.connectTo(gateway);

        MasterNode node = new MasterNode(nodeName, runTimeStep, controller);

        return node;
    }

    @Override
    public void initFields(){
        gateNode.initFields();
        actuationNode.initFields();
        buzzerNode.initFields();
        speakerNode.initFields();

        for(SlaveNode node : otherSlaveNodes) node.initFields();

        super.initFields();
    }

    @Override
    public void start() {
        super.start();
        gateNode.start();
        actuationNode.start();
        buzzerNode.start();
        speakerNode.start();

        for(SlaveNode node : otherSlaveNodes) node.start();

        gateNode.exportState("Started");
        gateNode.localController.exportState("Started");
        
        actuationNode.exportState("Started");
        actuationNode.localController.exportState("Started");
        
        buzzerNode.exportState("Started");
        buzzerNode.localController.exportState("Started");
        
        speakerNode.exportState("Started");
        speakerNode.localController.exportState("Started");
    }
    
    @Override
    public void terminate() {
        gateNode.terminate();
        actuationNode.terminate();
        buzzerNode.terminate();
        speakerNode.terminate();
        
        for(SlaveNode node : otherSlaveNodes) node.terminate();
        super.terminate();
    }

}
