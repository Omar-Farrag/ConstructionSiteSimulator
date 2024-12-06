import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This class represents a Master Zone
 */
public class MasterZone extends Zone {

    /**
     * Constructor
     * @param object_name Name of the master zone
     * @param runTimeStep Timestep of the object's simulation lifetime in ms
     * @param masterNodeLoop Algorithm running on the zone's master node's uController
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     * @param BLE_Transmission_Rate transmission rate in Kbps for data sent by bluetooth from slave nodes to master node in the zone
     * @param WiFi_transmission_rate Transmission rate for data sent from this zone's gateway to other gateways
     */
    MasterZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, int RTT_to_Master_Node,int BLE_Transmission_Rate, int WIFI_Transmission_Rate){
        super(object_name, runTimeStep, masterNodeLoop, null, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        
        // Create an output Database.csv file
        createDatabaseFile();

        // Initialize the master node
        this.masterNode = createMasterNode();
    }
    
    /**
     * Constructor
     * @param object_name Name of the master zone
     * @param runTimeStep Timestep of the object's simulation lifetime in ms
     * @param masterNodeLoop Algorithm running on the zone's master node's uController
     * @param masterNodeSetup Setup algorithm that runs once on the zone's master node's uController
     * @param RTT_to_Master_Node RTT in ms between the slave nodes and master node in the zone
     * @param BLE_Transmission_Rate transmission rate in Kbps for data sent by bluetooth from slave nodes to master node in the zone
     * @param WiFi_transmission_rate Transmission rate for data sent from this zone's gateway to other gateways
     */
    MasterZone(String object_name, int runTimeStep, ProcessingAlgorithm masterNodeLoop, ProcessingAlgorithm masterNodeSetup, int RTT_to_Master_Node, int BLE_Transmission_Rate, int WIFI_Transmission_Rate){
        super(object_name, runTimeStep, masterNodeLoop, masterNodeSetup, RTT_to_Master_Node, BLE_Transmission_Rate, WIFI_Transmission_Rate);
        
        // Create an output Database.csv file
        createDatabaseFile();

        // Initialize the master node
        this.masterNode = createMasterNode();
    }
    
    /**
     * Function to create the Database.csv file
     */
    private void createDatabaseFile(){
        try {
            File file = new File("logs/Database.csv");
            
            // Delete the file if it already exists from previous simulation runs 
            if (file.exists()) file.delete();

            // Write the header to the file
            FileWriter fileWriter = new FileWriter("logs/Database.csv", true);
            PrintWriter writer = new PrintWriter(fileWriter,true);
            writer.println("Time of Arrival,"+DataPacket.getHeader());
            writer.close();

        } catch (IOException e) {
            System.out.println("An error occurred while appending to the file.");
            e.printStackTrace();
        }
    }

    /**
     * Function to create the master node that consists of a uController and a gateway
     * @return fully created master node
     */
    @Override
    protected MasterNode createMasterNode(){

        // Get name of the MasterNode with the name of this MasterZone appended to it at the beginning
        String nodeName = getFullName("MasterNode");

        // Prepare names of the uController and gateway
        String controllerName = nodeName + "_controller";
        String gatewayName = nodeName + "_gateway";

        // Initialize new gateway
        gateway = new Gateway(gatewayName, runTimeStep, WIFI_Transmission_Rate);

        uController controller;

        // Initialize controller using the appropriate constructor
        if(masterNodeSetup!= null) controller = new uController(controllerName, runTimeStep, masterNodeLoop, masterNodeSetup);
        else controller = new uController(controllerName, runTimeStep, masterNodeLoop);

        // Connect the uController to the gateway
        controller.connectTo(gateway);

        // Initialize the MasterNode object
        MasterNode node = new MasterNode(nodeName, runTimeStep, BLE_Transmission_Rate, controller);

        return node;
    }
}
