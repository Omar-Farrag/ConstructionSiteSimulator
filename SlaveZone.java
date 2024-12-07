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



    // List of other slave nodes in the zone in addition to the nodes mentioned above.
    // This allows the user to add new slave nodes to the slave zone to customize it. 
    protected ArrayList<SlaveNode> otherSlaveNodes;

    /** 
     * Constructor
     * @param object_name Name of the slave zone
     * @param runTimeStep Timestep in ms of the zone's uControllers' runtime threads
     * @param masterNodeLoop Control algorithm to run on the zone's master node uController
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     * @param BLE_Transmission_Rate transmission rate in Kbps for data sent by bluetooth from slave nodes to master node in the zone
     * @param WiFi_transmission_rate Transmission rate for data sent from this zone's gateway to other gateways
     */
    public SlaveZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, int RTT_to_Master_Node, int BLE_Transmission_Rate, int WIFI_Transmission_Rate){
        super(object_name, masterNodeLoop, null, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        this.RTT_to_Master_Node = RTT_to_Master_Node;
        this.otherSlaveNodes = new ArrayList<>();
        init(runTimeStep);
        
    }

    /** 
     * Constructor
     * @param object_name Name of the slave zone
     * @param runTimeStep Timestep in ms of the zone and its internal components's simulation lifetimes
     * @param masterNodeLoop Control algorithm to run on the zone's master node uController
     * @param masterNodeSetup Setup algorithm that runs once on the zone's master node uController at the beginning
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     * @param BLE_transmission_rate transmission rate in Kbps for data sent by bluetooth from slave nodes to master node in the zone
     * @param WiFi_transmission_rate Transmission rate for data sent from this zone's gateway to other gateways
     */
    public SlaveZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, ProcessingAlgorithm masterNodeSetup, int RTT_to_Master_Node, int BLE_Transmission_Rate, int WIFI_Transmission_Rate){
        super(object_name, masterNodeLoop, masterNodeSetup, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        this.otherSlaveNodes = new ArrayList<>();
        init(runTimeStep);
    }

    /**
     * Function to initailize the nodes of the zone
     * @param runTimeStep Timestep in ms for the nodes' uControllers' runTimeThreads
     * 
     */
    private void init(int runTimeStep) {

        // Create the nodes
        gateNode = createGateNode(runTimeStep);
        actuationNode = createActuationNode(runTimeStep);
        buzzerNode = createBuzzerNode(runTimeStep);
        speakerNode = createSpeakerNode(runTimeStep);
        masterNode = createMasterNode(runTimeStep);

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
     * @param runTimeStep Timestep in ms for the gate node's uController's runTimeThread
     * @return Fully created gate node
     */
    private SlaveNode createGateNode(int runTimeStep){
    
        // Get full name for gate node 
        String nodeName = getFullName("GateNode");
        
        // Initialize a new gate node as a SlaveNode
        SlaveNode gateNode = new Gate(nodeName, runTimeStep, RTT_to_Master_Node, BLE_Transmission_Rate); 
        
        // Set the master node of the gate node to be the master node in this class
        gateNode.setMasterNode(masterNode);
        
        // return the gate node
        return gateNode;
        
    }

    /**
     * Function to set up the actuation node
     * @param runTimeStep Timestep in ms for the node's uController's runTimeThread
     * @return Fully created actuation node
     */
    private SlaveNode createActuationNode(int runTimeStep){

        // Get full name of the actuator node
        String nodeName = getFullName("ActuatorNode");

        // Prepare full name of the actuator device to be part of the actuator node
        String relayName = nodeName + "_relay";

        // Prepare full name of the uController to be part of the actuator node
        String controllerName = nodeName + "_controller";

        // Initialize the actuator relay
        relay = new Relay(relayName, 4);

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
        SlaveNode node = new SlaveNode(nodeName, RTT_to_Master_Node, BLE_Transmission_Rate, controller);
        
        // Set the node's master node to be the master node in the class
        gateNode.setMasterNode(masterNode);

        // Return the fully created actuationNode
        return node;

    }
    
    /**
     * Function to set up the buzzer node
     * @param runTimeStep Timestep in ms for the node's uController's runTimeThread
     * @return fully created buzzer node
     */
    private SlaveNode createBuzzerNode(int runTimeStep){

        // Get full name of buzzer node
        String nodeName = getFullName("BuzzerNode");

        // Prepare names of the relay, uController, and buzzer in the node
        String relayName = nodeName + "_relay";
        String controllerName = nodeName + "_controller";
        String buzzerName =relayName + "_buzzer";

        // Create a buzzer
        HighPowerDevice buzzer = new HighPowerDevice(buzzerName);
        
        // Initialize a relay with a single switch 
        Relay relay = new Relay(relayName,  1);

        // Connect the relay to the buzzer
        relay.connectTo(buzzer, 0);

        // Initialize a new uController
        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{
            // uController continuous algorithm
            
            // Prepare packets for the relay containing fields and their values to be published to the master node.
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
        SlaveNode node = new SlaveNode(nodeName, RTT_to_Master_Node, BLE_Transmission_Rate,controller);
        
        // Set the master node of the buzzer node to be the master node in this class
        gateNode.setMasterNode(masterNode);

        // Returned the created buzzer node
        return node;
    }

    
    /**
     * Function to set up the speaker node
     * @param runTimeStep Timestep in ms for the node's uController's runTimeThread
     * @return Fully created speaker node
     */
    private SlaveNode createSpeakerNode(int runTimeStep){
        // Get full name of the speaker node
        String nodeName = getFullName("SpeakerNode");

        // Prepare names of the controller and speaker
        String controllerName = nodeName + "_controller";
        String speakerName = nodeName + "_speaker";

        // Create an input file with a header for speaker
        // Header used to identify the fields of the speaker.
        // No need to specify any values in the file since these can be set dynamically by a uController script
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

        // Initialize the speaker object
        LowPowerDevice speaker = new LowPowerDevice(speakerName, 100);

        // Initialize a uController
        uController controller = new uController(controllerName, runTimeStep, (uController cont)->{
            
            // Prepare packets for the speaker containing fields and their values  be published to the master node.
            ArrayList<DataPacket> packets = new ArrayList<>();
            
            // For every field in the all the devices connected to uController (only speaker is connected)
            for(String field : cont.getLocalOfferedFields()) {
                // Get the value of the field
                ExecutionResult result = cont.getField(speakerName, field);
                
                // Add the returned data packet to the list of packets to publish if the result was successful
                if(result.isSuccess()) packets.add(result.getReturnedPacket());
            }

            // Publish the prepared packets to the master node
            cont.publishPacket(packets.toArray(new DataPacket[0]));
        });

        // Connect uController to speaker
        controller.connectTo(speaker);

        // Initialize speaker slave node
        SlaveNode node = new SlaveNode(nodeName,RTT_to_Master_Node, BLE_Transmission_Rate,controller);
        
        // Set the master node of the speaker node to be the master node in this class
        gateNode.setMasterNode(masterNode);

        // Return the fully created speaker node
        return node;

    }

    /**
     * Function to set up the master node of the zone
     * @param runTimeStep Timestep in ms for the zone's uController's runTimeThread
     * @return fully created master node
     */
    @Override
    protected MasterNode createMasterNode(int runTimeStep){

        // Get full name of master node
        String nodeName = getFullName("MasterNode");

        // Prepare names of the uController, camera, and
        String controllerName = nodeName + "_controller";
        String cameraName = nodeName + "_camera";
        String gatewayName = nodeName + "_gateway";

        // Create input file with header for the camera. Header is used to identify the fields of the 
        // camera. No values need to be added to the input file since these values can be set dynamically in a 
        // uController script 
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

        // Initialize a camera device
        LowPowerDevice camera = new LowPowerDevice(cameraName, 3500000);
        
        // Initialize a gateway
        gateway = new Gateway(gatewayName,  WIFI_Transmission_Rate);

        // Initialize a controller using the appropriate constructor depending on whether a setup function was provided or not 
        uController controller;
        if(masterNodeSetup!= null) controller = new uController(controllerName, runTimeStep, masterNodeLoop, masterNodeSetup);
        else controller = new uController(controllerName, runTimeStep, masterNodeLoop);

        // Connect the uController to the camera and gateway
        controller.connectTo(camera);
        controller.connectTo(gateway);

        // Initialze the master node 
        MasterNode node = new MasterNode(nodeName, BLE_Transmission_Rate, controller);

        // Return the fully created master node
        return node;
    }

    /**
     * Function to initialize the fields of all devices in the zone from the input files
     */
    @Override
    public void initFields(){
        gateNode.initFields();
        actuationNode.initFields();
        buzzerNode.initFields();
        speakerNode.initFields();

        for(SlaveNode node : otherSlaveNodes) node.initFields();

        super.initFields();
    }

    /**
     * Function to start the zone's lifetime thread. The zone is responsible for starting 
     * its constituent nodes
     */
    @Override
    public void start() {
        super.start();
        
        gateNode.start();
        actuationNode.start();
        buzzerNode.start();
        speakerNode.start();

        for(SlaveNode node : otherSlaveNodes) node.start();     
    }
    
    /**
     * Function to terminate the zone's lifetime thread. The zone is responsible for terminating its constituent nodes
     */
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
