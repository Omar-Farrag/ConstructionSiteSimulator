import java.util.ArrayList;

public class HighPowerDevice extends Device {

    private Boolean connectedToPower;

    public HighPowerDevice(String name, int runTimeStep){
        super(name, runTimeStep);
        connectedToPower = false;
    }

    public void powerOn(){
        synchronized(connectedToPower){
            connectedToPower = true;
        }
    }

    public void powerOff(){
        synchronized(connectedToPower){
            connectedToPower = false;
        }
    }

    public Boolean isPowered(){
        synchronized(connectedToPower){
            return connectedToPower;
        }
    }

    @Override
    public void exportState(String... args) {

        synchronized(writer){
            if(!hasAddedHeader){
                writer.println("Timestamp, Device Name, Is Powered");
                hasAddedHeader = true;
            }
            writer.println(getCurrentTimestamp()+"," + object_name + "," + isPowered());
        }
    }

    @Override
    protected void runTimeFunction() {
        exportState();
    }

    @Override
    public synchronized ExecutionResult execute(String command, String... arguments) {
        boolean success = false;
        DataPacket packet = null;

        if(command.equalsIgnoreCase("Switch")){
            if(arguments[0].equalsIgnoreCase("ON")) {
                powerOn();
                success = true;
            }

            else if(arguments[0].equalsIgnoreCase("OFF")) {
                powerOff();
                success = true;
            }
        }
        return new ExecutionResult(success, packet);
    }

    @Override
    public ArrayList<String> getFieldNames() {
        ArrayList<String> fieldNames = new ArrayList<>();
        fieldNames.add("Is Powered");
        return fieldNames;
    }

}
