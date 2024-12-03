import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class representing a Relay
 */
public class Relay extends Device {

    // Number of switches in the relay
    private int numSwitches;

    // Energization state of each switch. true --> energized
    // false --> not energized
    private ArrayList<Boolean> switchStates;

    // List of HighPowerDevice objects connected to the relay's switches
    private ArrayList<HighPowerDevice> connectedDevices;

    // Special Map between connected devices and their current state
    // Keys of the map are fieldNames, values of the map are the fieldValues
    // fieldNames are in the form "Connected Device [i]" or "Switch [i] Status"
    // The values are in the form "[HighPowerDevice name]" or "[true/false]"
    // Useful for querying certain information about the relay
    private HashMap<String, String> fieldValues;

    /**
     * Constructor
     * @param object_name Name of the Relay object
     * @param runTimeStep Timestep for the object's simulation lifetime in ms 
     * @param numSwitches Number of switches in the relay
     */
    public Relay(String object_name, int runTimeStep, int numSwitches){
        super(object_name, runTimeStep);
        this.numSwitches = numSwitches;

        switchStates = new ArrayList<>(numSwitches);
        connectedDevices = new ArrayList<>(numSwitches);
        fieldValues = new HashMap<>();
        
        // For every switch in total number of switches
        for (int i = 0; i<numSwitches;i++) {
            // Set the switch's initial state as false (no energiezed)
            switchStates.add(false);

            // Add an entry for the name of device connected to switch i
            // Set the initial state as "Disconnected"
            fieldValues.put("Connected Device " + i,"Disconnected");
            
            // Add an entry for the energization state of switch i
            // Set the initial state as "false"
            fieldValues.put("Switch " + i + " Status","false");

            // Add a null value to the list of connectedDevices indicating that there
            // is no device connected at that position
            connectedDevices.add(null);
        }

    }

    /**
     * Connect one of the relay's switches to a high power device
     * @param device Device to be connected to the relay
     * @param position Switch number to which the device is connected
     * @return whether the high power device was connected successfully
     */
    public boolean connectTo(HighPowerDevice device, int position){
                   
        // Ensure that the device is to be connected at position within
        // the relay's number of switches
        if(position < 0 || position >= numSwitches) return false;

        synchronized(connectedDevices){
            // Check if there is a device already conncted there at the target position
            if(connectedDevices.get(position) != null) return false;     

            // If no device connected, connect the new device to that position
            connectedDevices.set(position, device);
        }

        synchronized(switchStates){
            // Turn ON/OFF the newly connected device accordingly depending on the current
            // energization state  of the switch         
            if (switchStates.get(position)) device.powerOn();
            else device.powerOff();
        }

        // Update the value for the field "Connected Device [position] " to be the name of 
        // the newly conncted device
        synchronized(fieldValues){   
            fieldValues.put("Connected Device "+position,device.getObject_name());
        }

        return true;

    }

    /**
     * Function to disconnect the device currently connected at a certain position in the relay
     * @param position Postition in the relay at which the connected device, if any, would be disconnected
     * @return whether the device was disconnected or not
     */
    public boolean disconnect(int position){
                   
        // Ensure that the device to be disconnected from position [x] is within
        // the relay's number of switches
        if(position < 0 || position >= numSwitches) return false;

        synchronized(connectedDevices){
            // Power off the device to be discnnected
            if(connectedDevices.get(position) != null) connectedDevices.get(position).powerOff();
            
            // Acually disconnect tue device
            connectedDevices.set(position, null);
        }

        // Update the value for the field "Connected Device [position]" to be "Disconnected"
        synchronized(fieldValues){
            fieldValues.put("Connected Device "+position,"Disconnected");
        } 

        return true;
    }

    /**
     * Function to energize a certain switch in the relay
     * @param position Position of the switch to be energized
     */
    private boolean switchOn(int position){
        
        // Ensure position is within the bounds of the relay's number of switches
        if(position < 0 || position >= numSwitches) return false;
        
        synchronized(switchStates){
            // Energize the switch at the specified position
            switchStates.set(position, true);
        }
        synchronized(connectedDevices){
            // After energizing the switch, make sure to power on the device connected to that switch
            if(connectedDevices.get(position) != null) connectedDevices.get(position).execute("Switch", "ON");
        } 
        synchronized(fieldValues){
            // Update the value for the field "Switch [position]" to be "true"
            fieldValues.put("Switch "+position + " Status","true");
        }

        return true;
        
    }

    /**
     * Function to de-energize a certain switch in the relay
     * @param position Position of the switch to be de-energized
     */
    private boolean switchOff(int position){
        
        // Ensure position is within the bounds of the relay's number of switches
        if(position < 0 || position >= numSwitches) return false;
        
        synchronized(switchStates){
            // De-energize the switch at the specified position
            switchStates.set(position, false);
        }
        synchronized(connectedDevices){
            // After de-energizing the switch, make sure to power off the device connected to that switch
            if(connectedDevices.get(position) != null) connectedDevices.get(position).execute("Switch","OFF");
        }
        synchronized(fieldValues){
            // Update the value for the field "Switch [position]" to be "false"
            fieldValues.put("Switch "+position + " Status","false");
        }

        return true;
        
    }

    /**
     * Override of the exportState function in SimulationObject
     * Adds log messages to the output log file
     * @param event 
     */
    @Override
    public void exportState(String... events) {
        
        // Add a header to the CSV file if not already added
        if(!hasAddedHeader){
            ArrayList<String> header_columns = new ArrayList<>();
            header_columns.add("Timestamp");
            header_columns.add("Object Name");
            
            // Add columns for the fieldNames in the fieldValues map 
            for(int i = 0; i<numSwitches;i++) {
                header_columns.add("Connected Device " + i);
                header_columns.add("Switch " + i + " Status"); 
            } 
            // Join the header column names using commas
            String header = String.join(",",header_columns);

            synchronized(writer){
                // Write the header to the output CSV file
                writer.println(header);
            }
            hasAddedHeader = true;
        }

        ArrayList<String> values = new ArrayList<>();
        
        synchronized(connectedDevices){
            // For every switch in the relay
            for (int i = 0; i<numSwitches;i++){
                // Add the name of the device connected to that switch
                values.add(fieldValues.get(String.format("Connected Device %d",i)));

                // Add the energization state of the switch
                values.add(fieldValues.get(String.format("Switch %d Status",i)));
            }    
        }

        // Join the values using commas
        String joined_values = String.join(",", values);

        synchronized(writer){
            // Write row to output log file
            writer.println(getCurrentTimestamp()+"," + object_name + "," + joined_values);
        }
    }

    /**
     * Function running continuously in the object's runtime thread in timesteps of runTimeStep
     */
    @Override
    protected void runTimeFunction() {
        // Just export the current state of the relay
        exportState();
    }

    /**
     * Implementation of the execute function in the base Device class. Relays support two commands
     * Switch [position] [true/false] and GET [Connected Device i]/GET [Switch i Status], where i is an integer
     * @param command Command to be executed
     * @param arguments Arguments to the command as indicated above
     * @return ExecutionResult encapsulating the success state of the executed command and its returned packet
     */
    @Override
    public ExecutionResult execute(String command, String... arguments) {
        boolean success = false;
        DataPacket packet = null;
        
        // If command is Switch
        if(command.equalsIgnoreCase("Switch")){
            try {
                // Parse the position argument
                int position = Integer.parseInt(arguments[0]);
                
                // Switch on the appropriate position in the relay depending on the new state argument
                if(arguments[1].equalsIgnoreCase("true")) success = switchOn(position);
                else if (arguments[1].equalsIgnoreCase("false")) success = switchOff(position);
                
                // Return new packet encapsulating the user provided parameters
                packet = new DataPacket(this.object_name, arguments[0], arguments[1], 1, getCurrentTimestamp());
                
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        // If command is GET
        if(command.equalsIgnoreCase("GET")){
            synchronized(fieldValues){
                // Check if the field to retrieve is in the fieldValues map
                if(fieldValues.containsKey(arguments[0])) {

                    // If it is, encapsulate its value in a DataPacket
                    packet = new DataPacket(this.getObject_name(), arguments[0], fieldValues.get(arguments[0]),1,getCurrentTimestamp());                 
                    success = true;    
                }
            }
        }

        // Return the results after executing the command
        return new ExecutionResult(success, packet); 
    }

    /**
     * Function to terminate the Relay's simulation lifetime thread
     */
    @Override
    public void terminate() {

        // The relay is responsible for terminating all of its connected high power devices
        for (HighPowerDevice dev : connectedDevices){
            if(dev!= null) dev.terminate();
        }

        // Call to base class terminator
        super.terminate();
    }

    
    /**
     * Function to start the Relay's simulation lifetime thread
     */
    @Override
    public void start() {

        // The relay is responsible for starting all of its connected high power devices
        for (HighPowerDevice dev : connectedDevices){
            if(dev != null) dev.start();
        } 
        
        // Call to base class starter
        super.start();
    }

    /**
     * Function to return the field names available in the relay
     * @return Array of the names of the fields in the device
     */
    @Override
    public ArrayList<String> getFieldNames() {
        return new ArrayList<>(fieldValues.keySet());
    }
    

}
