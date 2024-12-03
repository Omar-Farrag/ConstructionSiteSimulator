import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents low power devices such as sensors
 */
public class LowPowerDevice extends Device {

    // List of fields in this low power device. Fields are similar to registers
    // present on digital sensors. For example, an IMU may have separate registers
    // for accelerations along the three axes, angular speeds, magnetic fields, etc.
    // Storing the field names in an arraylist ensures that we can read them in a fixed
    // order
    private ArrayList<String> fieldNames;

    // Map between field names and their current values
    private HashMap<String, String> fieldValues;

    // CSV file containing values to be used in the simulation
    // First row of the CSV file contains the names of the fields
    // The values are added by the user 
    private String inputFileName;
    
    // List of rows read from the input file
    // Each row contains the values of the device's fields at a single point in time
    private List<List<String>> fileInputs;

    // Current row being used in fileInputs
    private int rowIndex;

    // Size in bytes of the fields in the device
    // It is assumed that all fields have the same size
    private int fieldSize;

    /**
     * Constructor
     * @param name Name of the low power device
     * @param runTimeStep Timestep of the object's lifetime in ms
     * @param fieldSize Size in bytes of the object's fields
     */
    public LowPowerDevice(String name, int runTimeStep, int fieldSize){
        super(name, runTimeStep);
        fieldNames = new ArrayList<>();
        fileInputs = new ArrayList<>();
        fieldValues = new HashMap<>();
        rowIndex = 0;
        this.inputFileName = getInputFileName(name);

        // Create an empty input file for the user to fill
        if(!new File(inputFileName).exists())
        { 
            try (FileWriter fileWriter = new FileWriter(inputFileName);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write("");
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.fieldSize = fieldSize; 
    }

    /**
     * Read the content of the input file
     */
    @Override
    public void initFields(){

        fieldValues.clear();
        
        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
            
            // Read the first line(header) in the file
            String line = br.readLine();
            if(line != null){
                // Extract field names in header
                String[] headerValues = line.split(",");
                
                // Add the field names to the fieldNames array
                for (String headerValue : headerValues) fieldNames.add(headerValue);
            }

            // Read the rest of the file line by line
            while ((line = br.readLine()) != null) {

                // List of values to store the field values for a row
                List<String> row = new ArrayList<>();

                // Line contains the values for all fields separated by commas
                // Exctract the field values 
                String[] values = line.split(",");
                
                // Add the field values to the row
                for (String value : values) {
                    row.add(value);
                }

                // Add row to fileInputs array
                fileInputs.add(row);
            }

            // Add an entry in fieldValues for each field and set the initial value to "Uninitialized"
            for(String fieldName : fieldNames){
                fieldValues.put(fieldName, "Uninitialized");
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    /**
     * Function to append the current state of the device to an output log file 
     * @param events Optional message to include in the output log
     */
    @Override
    public void exportState(String... events) {
        if(!hasAddedHeader){
            // Write the header to the output file if not already added
            ArrayList<String> exported_header_columns = new ArrayList<>();
            exported_header_columns.add("Timestamp");
            exported_header_columns.add("Object Name");
            exported_header_columns.addAll(fieldNames);
 
            String exported_header = String.join(",",exported_header_columns);

            synchronized(writer){
                writer.println(exported_header);
            }
            hasAddedHeader = true;
        }

        // Array containing the current values to be exported to log file
        ArrayList<String> exported_values = new ArrayList<>();
        
        synchronized (fieldValues){
            // For each field in fieldNames
            for (String fieldName : fieldNames){
                // Retrieve the field's value
                String exported_value = fieldValues.get(fieldName);
                
                // Add the value to the list of values
                exported_values.add(exported_value.toString());
            }
        }

        // Join the values using commas
        String joined_values = String.join(",", exported_values);

        synchronized(writer){
            // Write the values to the output file
            writer.println(getCurrentTimestamp()+"," + object_name + "," + joined_values);
        }
    }

    /**
     * Function to retrieve the value of a specific field in the device
     * @param fieldName Name of the field whose value is to be retrieved
     * @return current value of the field encapsulated in a DataPacket
     */
    private DataPacket readField(String fieldName){
            String value = fieldValues.get(fieldName);
            return new DataPacket(object_name,fieldName,value,fieldSize,getCurrentTimestamp());
    }
    
    /**
     * Function to set the value of a specific field in the device
     * @param fieldName Name of the field whose value is to be updated
     * @param value New value of the field
     * @return DataPacket encapsulating the new value of the field
     */
    private DataPacket setField(String fieldName, String value){
            fieldValues.put(fieldName,value);
            return new DataPacket(object_name,fieldName,value,fieldSize,getCurrentTimestamp());
    }

    /**
     * Implementation of the execute function in the base Device class. In low power devices,
     * two commands are supported: "GET [fieldName]" and "SET [fieldName] [New Value]"
     * @param command Command to be executed
     * @param arguments Arguments to be used by the command as indicated above
     * @return ExecutionResult encapsulating the success state of the command and the packet returned by the command 
     */
    @Override
    public ExecutionResult execute(String command, String... arguments){
        boolean success = false;
        DataPacket packet = null;

        synchronized(fieldValues){
            // If command is "GET"
            if(command.equalsIgnoreCase("GET")){
                // Check that the field to be retrieved exists
                if(fieldValues.containsKey(arguments[0])) {
                    // Retrieve its value if it exists
                    packet = readField(arguments[0]);    
                    success = true;    
                }
            }
            
            // If command is "SET"
            else if(command.equalsIgnoreCase("SET")){
                // Check that the field to be retrieved exists
                if(fieldValues.containsKey(arguments[0])) {
                    // Update its value if it exists
                    packet = setField(arguments[0], arguments[1]);
                    success = true;        
                }
            }
        }
        // Return results of the command
        return new ExecutionResult(success, packet);
    }

    /**
     * Function runnning continuously with a timestep of runTimeStep
     */
    @Override
    protected void runTimeFunction() {
        synchronized(fieldValues){
            // If the input file had no data rows,
            if (fileInputs.size() == 0) {
                // Add a log message to the output log file
                exportState();
                return;
            }

            // Othewise, take the next row from fileInputs and set it as the current 
            // value for the fields in fieldValues map
            int columnIndex = 0;
            // Loop through the field names in order
            for(String fieldName : fieldNames){

                // Get a value from the current row at the current column and increment column
                String stringValue = fileInputs.get(rowIndex).get(columnIndex++);
                
                // Set the value to the current field in the fieldValues map. This becomes the current
                // value for that field. Maintain is a special value indicating that the previous value
                // is unaltered
                if(!stringValue.equalsIgnoreCase("MAINTAIN")){
                    fieldValues.put(fieldName,stringValue);
                }
            }

            // After updating the values for all fields using current row,
            // Increment rowIndex to next row. Roll over if last row is reached
            rowIndex = (rowIndex + 1) % fileInputs.size();
        }
        
        // Add a log to the output log file to show the new state of all fields in the device 
        exportState();
    }

    /**
     * Get list of field names in this device
     * @return array of the names of fields in the device
     */
    @Override
    public ArrayList<String> getFieldNames() {
        return fieldNames;
    }
}
