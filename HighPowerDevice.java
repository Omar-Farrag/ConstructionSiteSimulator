import java.util.ArrayList;

/**
 * Class representing high power devices such as motors
 */
public class HighPowerDevice extends Device {

    // State of the motor, whether its connected to power or not
    private Boolean connectedToPower;

    /**
     * Constructor
     * @param name Name of the high power device
     * @param runTimeStep Timestep of the object's lifetime in ms
     */
    public HighPowerDevice(String name, int runTimeStep){
        super(name, runTimeStep);
        connectedToPower = false;
    }

    /**
     * Function to power on the device
     */
    public void powerOn(){
        synchronized(connectedToPower){
            connectedToPower = true;
        }
    }

    /**
     * Function to power off the device
     */
    public void powerOff(){
        synchronized(connectedToPower){
            connectedToPower = false;
        }
    }

    /**
     * Function to retrieve the current power state of the device
     * @return true if the device is connected to power, false otherwise
     */
    public Boolean isPowered(){
        synchronized(connectedToPower){
            return connectedToPower;
        }
    }

    /*
     * Override of the base exportState function 
     */
    @Override
    public void exportState(String... args) {

        synchronized(writer){
            // Add a header to the output CSV log file if it hasn't been added
            if(!hasAddedHeader){
                writer.println("Timestamp, Device Name, Is Powered");
                hasAddedHeader = true;
            }
            // Add the current power status of the device to the log file
            writer.println(getCurrentTimestamp()+"," + object_name + "," + isPowered());
        }
    }

    /**
     * Function running continuously in the device's runtime thread with a timestep of runTimeStep
     */
    @Override
    protected void runTimeFunction() {
        exportState();
    }

    /**
     * Implementation of the execute function in the base Device class. 
     * Only one command is supported for HighPowerDevices: "Switch [new state]"
     * @param command Commmand to be executed
     * @param arguments arguments[0] is the new power state of the device ON/OFF
     * @return ExecutionResult encapsulating the success status of the executed command
     */
    @Override
    public synchronized ExecutionResult execute(String command, String... arguments) {
        boolean success = false;
        DataPacket packet = null;

        //Ensure that the command entered is "Switch"
        if(command.equalsIgnoreCase("Switch")){

            // Switch ON if the argument is "ON"
            if(arguments[0].equalsIgnoreCase("ON")) {
                powerOn();
                success = true;
            }

            // Switch OFF if the argument is "OFF"
            else if(arguments[0].equalsIgnoreCase("OFF")) {
                powerOff();
                success = true;
            }
        }

        // Return the results of the command
        return new ExecutionResult(success, packet);
    }

    /**
     * Returns the fields in this high power device. 
     * The only field in HighPowerDevices is "Is Powered"
     * @return array of field names in the device
     */
    @Override
    public ArrayList<String> getFieldNames() {
        ArrayList<String> fieldNames = new ArrayList<>();
        fieldNames.add("Is Powered");
        return fieldNames;
    }

}
