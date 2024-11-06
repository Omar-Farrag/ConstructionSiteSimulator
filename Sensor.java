import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Sensor extends Device {


    protected List<List<String>> simulationValues;
    protected HashMap<String, Float> fields;
    protected int valueIndex;

    protected abstract void initFields();

    public Sensor(String name, String outputFileName, int runTimeStep, String inputFileName){
        super(name, outputFileName, runTimeStep);
        fields = new HashMap<>();
        initFields();
        readSimulationValues(inputFileName);

        try{
            if (simulationValues.get(0).size() != fields.size()) throw new Exception(name + ": simulation values and device's fields do not match");
        } catch(Exception e){
            e.printStackTrace();
        }
        valueIndex = 0;
    }

    private void readSimulationValues(String inputFileName){
        simulationValues = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFileName))) {
        
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                List<String> row = new ArrayList<>();
                String[] values = line.split(",");
                
                for (String value : values) {
                    row.add(value);
                }
                simulationValues.add(row);
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }

    @Override
    public synchronized void exportState(String... event) {
        if(!hasAddedHeader){
            ArrayList<String> header_columns = new ArrayList<>();
            header_columns.add("Timestamp");
            header_columns.add("Object Name");

            synchronized(fields){
                for (String field : fields.keySet()) header_columns.add(field);
                
            } 
            String header = String.join(",",header_columns);

            writer.println(header);
            hasAddedHeader = true;
        }

        ArrayList<String> values = new ArrayList<>();
        synchronized (fields){
            for (String key : fields.keySet()){
                Float value = fields.get(key);
                values.add(value.toString());
            }
        }

        String joined_values = String.join(",", values);

        writer.println(getCurrentTimestamp()+"," + object_name + "," + joined_values);
    }

    private DataPacket readField(String fieldName){
        synchronized(fields){
            Float value = fields.get(fieldName);
            return new DataPacket(String.join("_",object_name,fieldName),value,1,getCurrentTimestamp());
        }
    }

    
    private boolean setField(String fieldName, Float value){
        synchronized(fields){
            if(fields.containsKey(fieldName)){
                fields.put(fieldName,value);
                return true;
            }
            return false;
        }
    }

    @Override
    public synchronized ExecutionResult execute(String command, String... arguments){
        boolean success = false;
        DataPacket packet = null;

        if(command.equalsIgnoreCase("GET")){
            synchronized(fields){
                if(fields.containsKey(arguments[0])) {
                    packet = readField(arguments[0]);    
                    success = true;    
                }
            }
        }

        else if(command.equalsIgnoreCase("SET")){
            synchronized(fields){
                if(fields.containsKey(arguments[0])) {
                    success = setField(arguments[0], Float.parseFloat(arguments[1]));        
                }
            }
        }

        return new ExecutionResult(success, packet);


    }
}
