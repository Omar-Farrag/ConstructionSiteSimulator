import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class is a special type of a SlaveNode representing
 * the gate to a certain a zone
 */
public class Gate extends SlaveNode{

    //Scanner for the gate responsible for reading workers' ID
    private LowPowerDevice scanner;

    //Relay connected to a motor to move up/down the gate barrier
    private Relay motorRelay;

    //Motor moving up/down the gate barrier to prevent or allow workers to enter
    private HighPowerDevice motor;

    /**
     * Constructor
     * @param nodeName Name of this gate node
     * @param runTimeStep Timestep for this gate's lifetime in ms
     * @param RTT_to_Zone_Controller RTT between the gate node and the master node in ms
     * @param BLE_transmission_rate BLE transmission rate in kbps for data sent from this node to the master node
     */
    public Gate(String nodeName, int runTimeStep, int RTT_to_Zone_Controller, int BLE_transmission_rate){
        super(nodeName, runTimeStep, RTT_to_Zone_Controller, BLE_transmission_rate,
                // Initialize new uController
                new uController(
                    getFullName(nodeName, "controller") 
                    , runTimeStep
                    , (uController controller)->{gateControllerFunction(controller,nodeName);}
                    )
            );

        // Get full name for the scanner object including the prefix and file extension   
        String name = getFullName(nodeName,"scanner");
        
        // Get name of the input file corresponding to the scanner object's full name
        String inputFile = Device.getInputFileName(name);

        // If the input file for the scanner does not exist, create a new one
        // and add ta header ("ID") to it
        if(!new File(inputFile).exists())
        {        
            try (FileWriter fileWriter = new FileWriter(inputFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("ID");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Initialize scanner
        scanner = new LowPowerDevice(name, runTimeStep, 12); 

        // Get full name for the relay object including the prefix and file extension  
        name = getFullName(nodeName,"relay");
        
        // Initialize relay with one switch
        motorRelay = new Relay(name, runTimeStep,1);

        // Get full name for the relay object including the prefix and file extension  
        name = getFullName(nodeName,"motor");

        // Initialize motor  
        motor = new HighPowerDevice(name, runTimeStep);

        // Connect the uController in the node to the scanner object
        localController.connectTo(scanner);

        // Connect switch 0 in motorRelay to the motor
        motorRelay.connectTo(motor, 0);

        // Connect the uController in the node to the motor relay
        localController.connectTo(motorRelay);

    }

    /**
     * Function running continuously on the gate node's uController
     * @param controller uController on which this function runs
     * @param gateObjectName Name of the current gate slave node object
     */
    private static void gateControllerFunction(uController controller, String gateObjectName){
        
        // Get the latest value for field "ID" in device called gateObjectName_scanner that is connected to the uController controller 
        ExecutionResult result = controller.getField(getFullName(gateObjectName, "scanner"), "ID");
        
        // Check if ID field was retrieved successfully
        if(result.isSuccess()){

            // Get the value of the ID
            String value = result.getReturnedPacket().getValue();
            
            // The parent node of the uController that is currently running this function
            // is in fact the current Gate slave node object.  
            SlaveNode parent = controller.getParentSlaveNode();
                      
            // Add a log message to the controller's output log file
            controller.exportState(String.format("Asked Parent Node [%s] about ID [%s]'s permission",parent.getObject_name(),value));
                        
            // Ask the parent to check whether the read ID value is permitted to enter
            boolean permitted = parent.isPermittedToEnter(value);

            // Add a log message to the controller's output log file
            controller.exportState(String.format("ID [%s]'s permission: [%s]", value, permitted? "ALLOWED": "DENIED"));
            
            // If permitted, update switch 0 in connected object gateObjeName_relay to be ON to turn on the motor to move up barrier
            if(permitted) controller.updateSwitch(getFullName(gateObjectName, "relay"), "0","true");
            
            // Otherwise, update the switch to OFF to move it down
            // In reality, moving the motor in the opposite direction is done 
            // by reversing the direction of the current in the motor using an
            // H-bridge for example. However, to keep things simple just assume 
            // OFF moves the motor in the opposite direction 
            else controller.updateSwitch(getFullName(gateObjectName, "relay"), "0","false");
            
            // Retrieve the name of the device connected to switch 0 of the relay and the current state of the switch
            ExecutionResult result1 = controller.getField(getFullName(gateObjectName, "relay"),"Connected Device 0" );
            ExecutionResult result2 = controller.getField(getFullName(gateObjectName, "relay"),"Switch 0 Status" );
            
            // Publish the information to the subscribed node (the master node in the same zone)
            if(result1.isSuccess() && result2.isSuccess()) controller.publishPacket(result1.getReturnedPacket(), result2.getReturnedPacket());
        }

    }

    /**
     * Returns the name of an object as nodeName_localObjectName
     * @param nodeName
     * @param localObjectName
     * @return full name
     */
    private static String getFullName(String nodeName, String localObjectName){
        return nodeName + "_"+ localObjectName;
    }    
   
}
