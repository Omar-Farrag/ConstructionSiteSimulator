import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LowPowerDevice extends Device {

    private ArrayList<String> fieldNames;
    private HashMap<String, String> fieldValues;
    private List<List<String>> fileInputs;
    private int rowIndex;
    private int fieldSize;


    public LowPowerDevice(String name, String outputFileName, int runTimeStep, String inputFileName, int fieldSize){
        super(name, outputFileName, runTimeStep);
        fieldNames = new ArrayList<>();
        fileInputs = new ArrayList<>();
        fieldValues = new HashMap<>();
        initFields(inputFileName);
        rowIndex = 0;
        this.fieldSize = fieldSize; 
    }

    private void initFields(String inputFileName){

        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
        
            String line = br.readLine();
            String[] headerValues = line.split(",");
            for (String headerValue : headerValues) fieldNames.add(headerValue);

            while ((line = br.readLine()) != null) {
                List<String> row = new ArrayList<>();
                String[] values = line.split(",");
                
                for (String value : values) {
                    row.add(value);
                }
                fileInputs.add(row);
            }

            for(String fieldName : fieldNames){
                fieldValues.put(fieldName, "Uninitialized");
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    @Override
    public void exportState(String... event) {
        if(!hasAddedHeader){
            ArrayList<String> exported_header_columns = new ArrayList<>();
            exported_header_columns.add("Timestamp");
            exported_header_columns.add("Object Name");
            exported_header_columns.addAll(fieldNames);
 
            String exported_header = String.join(",",exported_header_columns);

            writer.println(exported_header);
            hasAddedHeader = true;
        }

        ArrayList<String> exported_values = new ArrayList<>();
        synchronized (fieldValues){
            for (String fieldName : fieldNames){
                String exported_value = fieldValues.get(fieldName);
                exported_values.add(exported_value.toString());
            }
        }

        String joined_values = String.join(",", exported_values);

        writer.println(getCurrentTimestamp()+"," + object_name + "," + joined_values);
    }

    private DataPacket readField(String fieldName){
        synchronized(fieldValues){
            String value = fieldValues.get(fieldName);
            return new DataPacket(object_name,fieldName,value,fieldSize,getCurrentTimestamp());
        }
    }
    
    private DataPacket setField(String fieldName, String value){
        synchronized(fieldValues){
            fieldValues.put(fieldName,value);
            return new DataPacket(object_name,fieldName,value,fieldSize,getCurrentTimestamp());
        }
    }

    @Override
    public synchronized ExecutionResult execute(String command, String... arguments){
        boolean success = false;
        DataPacket packet = null;

        if(command.equalsIgnoreCase("GET")){
            if(fieldValues.containsKey(arguments[0])) {
                packet = readField(arguments[0]);    
                success = true;    
            }
        }

        else if(command.equalsIgnoreCase("SET")){
            if(fieldValues.containsKey(arguments[0])) {
                packet = setField(arguments[0], arguments[1]);
                success = true;        
            }
        }

        return new ExecutionResult(success, packet);
    }

    @Override
    protected void runTimeFunction() {
        synchronized(fieldValues){
            if (fileInputs.size() == 0) {
                exportState();
                return;
            }

            int columnIndex = 0;
            for(String fieldName : fieldNames){
                String stringValue = fileInputs.get(rowIndex).get(columnIndex++);
                
                if(!stringValue.equalsIgnoreCase("MAINTAIN")){
                    fieldValues.put(fieldName,stringValue);
                }
            }

            rowIndex = (rowIndex + 1) % fileInputs.size();
        }
        exportState();
    }

    public ArrayList<String> getFieldNames() {
        return fieldNames;
    }
}
