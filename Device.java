import java.util.ArrayList;

/**
 * Abstract Base class for Devices
 */
public abstract class Device extends SimulationObject{
    
    /**
     * Constructor
     * @param name Name of the Device Object
     */
    public Device(String name){
        super(name);
    }

    /**
     * This function initializes the fields of a device.
     * Default implementation does not initialize anything.
     * Child classes can override this behavior.
     */
    public void initFields(){
        // Nothing to initialize
    }

    /**
     * Utility function to generate the Input file name using the object's name
     * @param name object's name for which the output log file name is to be generated
     * @return inputFileName
     */
    protected static String getInputFileName(String name) {
        return "inputs/"+ name+ "_input.csv";
    }

    
    /**
     * Executes a command on the device. This is similar to how 
     * digital sensors accept UART commands to return register 
     * values or update them 
     * @param command Command to be executed
     * @param arguments Arguments for the command
     * @return result The result after executing the command
     */
    public abstract ExecutionResult execute(String command, String... arguments);

    /**
     * Function to return the field names in the device. These fields can represent
     * named registers in the device. For example, an IMU may have a separate register
     * for the acceleration in each direction (x_acceleration, y_acceleration, z_acceleration).
     * @return array of the names of all fields in the device
     */
    public abstract ArrayList<String> getFieldNames();


}
