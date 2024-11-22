import java.util.ArrayList;
import java.util.HashMap;

public class Relay extends Device {

    private int numSwitches;
    private ArrayList<Boolean> switchStates;
    private ArrayList<HighPowerDevice> connectedDevices;
    private HashMap<String, String> fieldValues;

    public Relay(String object_name, int runTimeStep, int numSwitches){
        super(object_name, runTimeStep);
        this.numSwitches = numSwitches;

        switchStates = new ArrayList<>(numSwitches);
        connectedDevices = new ArrayList<>(numSwitches);
        fieldValues = new HashMap<>();
        
        for (int i = 0; i<numSwitches;i++) {
            switchStates.add(false);
            fieldValues.put("Connected Device " + i,"Disconnected");
            fieldValues.put("Switch " + i + " Status","false");
        }
        for (int i = 0; i<numSwitches;i++) connectedDevices.add(null);

    }

    public synchronized boolean connectTo(HighPowerDevice device, int position){
                   
        if(position < 0 || position >= numSwitches) return false;

        synchronized(connectedDevices){
            if(connectedDevices.get(position) != null) return false;     
            connectedDevices.set(position, device);
        }
        
        synchronized(switchStates){
            if (switchStates.get(position)) device.powerOn();
            else device.powerOff();
        }

        synchronized(fieldValues){   
            fieldValues.put("Connected Device "+position,device.getObject_name());
        }

        
        return true;

    }

    public boolean disconnect(int position){
                   
        if(position < 0 || position >= numSwitches) return false;

        synchronized(connectedDevices){
            if(connectedDevices.get(position) != null) connectedDevices.get(position).powerOff();
            connectedDevices.set(position, null);
        }
        
        synchronized(fieldValues){
            fieldValues.put("Connected Device "+position,"Disconnected");
        } 

        return true;
    }

    private boolean switchOn(int position){
        if(position < 0 || position >= numSwitches) return false;
        
        synchronized(switchStates){
            switchStates.set(position, true);
        }
        synchronized(connectedDevices){
            if(connectedDevices.get(position) != null) connectedDevices.get(position).execute("Switch", "ON");
        } 
        synchronized(fieldValues){
            fieldValues.put("Switch "+position + " Status","true");
        }


        return true;
        
    }

    private boolean switchOff(int position){
        if(position < 0 || position >= numSwitches) return false;
        
        synchronized(switchStates){
            switchStates.set(position, false);
        }
        synchronized(connectedDevices){
            if(connectedDevices.get(position) != null) connectedDevices.get(position).execute("Switch","OFF");
        }
        synchronized(fieldValues){
            fieldValues.put("Switch "+position + " Status","false");
        }
        return true;
        
    }

    @Override
    public void exportState(String... event) {
        if(!hasAddedHeader){
            ArrayList<String> header_columns = new ArrayList<>();
            header_columns.add("Timestamp");
            header_columns.add("Object Name");
            
            for(int i = 0; i<numSwitches;i++) {
                header_columns.add("Connected Device " + i);
                header_columns.add("Switch " + i + " Status"); 
            } 
            String header = String.join(",",header_columns);

            writer.println(header);
            hasAddedHeader = true;
        }

        ArrayList<String> values = new ArrayList<>();
        
        synchronized(connectedDevices){
            for (int i = 0; i<numSwitches;i++){
                if(connectedDevices.get(i) != null) values.add(connectedDevices.get(i).getObject_name());
                else values.add("Disconnected");
                values.add(switchStates.get(i).toString());
            }    
        }
        

        String joined_values = String.join(",", values);

        synchronized(writer){
            writer.println(getCurrentTimestamp()+"," + object_name + "," + joined_values);
        }
    }

    @Override
    protected void runTimeFunction() {
        exportState();
    }

    @Override
    public ExecutionResult execute(String command, String... arguments) {
        boolean success = false;
        DataPacket packet = null;
        
        if(command.equalsIgnoreCase("Switch")){
            try {
                int position = Integer.parseInt(arguments[0]);
                if(arguments[1].equalsIgnoreCase("ON")) success = switchOn(position);
                else if (arguments[1].equalsIgnoreCase("OFF")) success = switchOff(position);
                packet = new DataPacket(this.object_name, arguments[0], arguments[1], 1, getCurrentTimestamp());
                
            } catch (NumberFormatException e) {
                // e.printStackTrace();
            }
        }
        if(command.equalsIgnoreCase("GET")){
            synchronized(fieldValues){
                if(fieldValues.containsKey(arguments[0])) {
                    packet = new DataPacket(this.getObject_name(), arguments[0], fieldValues.get(arguments[0]),1,getCurrentTimestamp());                 
                    success = true;    
                }
            }
        }

        return new ExecutionResult(success, packet); 
    }

    @Override
    public void terminate() {
        for (HighPowerDevice dev : connectedDevices){
            if(dev!= null) dev.terminate();
        }
        super.terminate();
    }

    @Override
    public void start() {
        for (HighPowerDevice dev : connectedDevices){
            if(dev != null) dev.start();

        } 
        super.start();
    }

    @Override
    public ArrayList<String> getFieldNames() {
        ArrayList<String> fieldNames = new ArrayList<>();
        for(int i = 0; i<numSwitches;i++) {
           fieldNames.add("Connected Device " + i);
           fieldNames.add("Switch " + i + " Status"); 
        } 
        return fieldNames;
    }
    

}
