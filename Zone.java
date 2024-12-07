
/**
 * Abstract parent class representing a zone
 */
public abstract class Zone extends SimulationObject {

    // Master node of the zone
    protected MasterNode masterNode;
    
    // Control algorithm running continuously on master node's uController
    protected ProcessingAlgorithm masterNodeLoop;
    
    // Setup algorithm that runs once on master node's uController at the beginning
    protected ProcessingAlgorithm masterNodeSetup;

    // Reference to the master node's internal gateway
    protected Gateway gateway;

    // Round trip time in ms between the zone's slave nodes and master node.
    // It is assumed that all slave nodes have the same RTT to the master node
    protected int RTT_to_Master_Node;
    
    // BLE transmission rate in Kbps between slave nodes and master node in the same zone
    protected int BLE_Transmission_Rate;

    // WiFi transmission rate in Kbps between connected gateways
    protected int WIFI_Transmission_Rate;

    /**
     * Constructor
     * @param objectName Name of the zone
     * @param runTimeStep Timestep in ms of the zone's uControllers' runtime threads
     * @param loop Control algorithm running continuously on master node's uController
     * @param setup Setup algorithm that runs once on master node's uController at the beginning
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     * @param BLE_Transmission_Rate transmission rate in Kbps for data sent by bluetooth from slave nodes to master node in the zone
     * @param WiFi_transmission_rate Transmission rate for data sent from this zone's gateway to other gateways
     */
    public Zone(String objectName, ProcessingAlgorithm loop, ProcessingAlgorithm setup, int RTT_to_Master_Node, int BLE_Transmission_Rate, int WIFI_Transmission_Rate){
        super(objectName);
        this.masterNodeLoop = loop;
        this.masterNodeSetup = setup;
        this.RTT_to_Master_Node = RTT_to_Master_Node;
        this.BLE_Transmission_Rate = BLE_Transmission_Rate;
        this.WIFI_Transmission_Rate = WIFI_Transmission_Rate;
    }

    /**
     * Function to initialize the fields of the devices in the zone. Default implementation
     */
    public void initFields(){
        masterNode.initFields();

    }

    /**
     * Function to connect two adjacent zones together via Wi-Fi
     * @param zone Zone to conenct to this zone
     * @param RTT_to_gateway Round trip time in ms between this zone's gateway and other zone's gateway
     */
    public void connectToZone(Zone zone, int RTT_to_gateway){
        this.gateway.connectTo(zone.gateway, RTT_to_gateway);
    }
    
    /**
     * Utility function to generate full name of a local object name
     * @param localName Name of the object whose full name is to be retrieved
     * @return Name of the object with the name of the zone added as a prefix
     */
    protected String getFullName(String localName){
        return this.object_name + "_"+localName;
    }
    
    /**
     * Function called continuously by the object's runtime thread assuming a thread was started for the zone object
     */
    @Override
    protected void runTimeFunction() {
        // Do Nothing
    }
    
    /**
     * Function to start the object without starting a new run time thread. Zone is responsible for starting
     * its master node.
     */
    @Override
    public void start() {
        super.start(false,0);
        exportState("Started");
        
        masterNode.start();   
        
    }

    /**
     * Function to terminate the zone's runtime thread. Zone is responsible for terminating
     * its master node.
     */
    @Override
    public void terminate() {        
        masterNode.terminate();
        super.terminate();
    }
    
    /**
     * Abstract method to be implemented by children. This function sets up a master node for the zone
     * @param runTimeStep Timestep in ms for the node's uController's runTimeThread
     * @return Fully created master node object
     */
    protected abstract MasterNode createMasterNode(int runTimeStep);
}
