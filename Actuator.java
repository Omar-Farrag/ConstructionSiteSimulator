import java.util.ArrayList;

public class Actuator extends Device {

    private int numSwitches;
    private ArrayList<Boolean> switchStates;
    private ArrayList<HighPowerDevice> connectedDevices;

    public Actuator(String object_name,String outputFileName, int runTimeStep, int numSwitches){
        super(object_name, outputFileName, runTimeStep);
        this.numSwitches = numSwitches;

        switchStates = new ArrayList<>(numSwitches);
        connectedDevices = new ArrayList<>(numSwitches);
        
        for (int i = 0; i<numSwitches;i++) switchStates.add(false);
        for (int i = 0; i<numSwitches;i++) connectedDevices.add(null);
    }

    public synchronized boolean connectTo(HighPowerDevice device, int position){
                   
        if(position < 0 || position >= numSwitches) return false;
        if(connectedDevices.get(position) != null) return false;
           
        connectedDevices.set(position, device);
        if (switchStates.get(position)) device.powerOn();
        else device.powerOff();
            
        return true;

    }

    public synchronized boolean disconnect(int position){
                   
        if(position < 0 || position >= numSwitches) return false;
        if(connectedDevices.get(position) != null) connectedDevices.get(position).powerOff();

        connectedDevices.set(position, null);
        return true;
    }

    private synchronized boolean switchOn(int position){
        if(position < 0 || position >= numSwitches) return false;
        
        switchStates.set(position, true);
        if(connectedDevices.get(position) != null) connectedDevices.get(position).execute("Switch", "ON");
        return true;
        
    }

    private synchronized boolean switchOff(int position){
        if(position < 0 || position >= numSwitches) return false;
        
        switchStates.set(position, false);
        if(connectedDevices.get(position) != null) connectedDevices.get(position).execute("Switch","OFF");
        return true;
        
    }

    @Override
    public synchronized void exportState(String... event) {
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
        for (int i = 0; i<numSwitches;i++){
            if(connectedDevices.get(i) != null) values.add(connectedDevices.get(i).getObject_name());
            else values.add("Disconnected");
            values.add(switchStates.get(i).toString());
        }    
        

        String joined_values = String.join(",", values);

        writer.println(getCurrentTimestamp()+"," + object_name + "," + joined_values);
    }

    @Override
    public void runTimeFunction() {
        exportState();
    }

    @Override
    public synchronized ExecutionResult execute(String command, String... arguments) {
        boolean success = false;
        DataPacket packet = null;
        
        if(command.equalsIgnoreCase("Switch")){
            int position = Integer.parseInt(arguments[0]);
            if(arguments[1].equalsIgnoreCase("ON")) success = switchOn(position);
            else if (arguments[1].equalsIgnoreCase("OFF")) success = switchOff(position);
        }

        return new ExecutionResult(success, packet); 
    }

}
