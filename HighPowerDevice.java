
public class HighPowerDevice extends Device {

    private Boolean connectedToPower;

    public HighPowerDevice(String name, String outputFileName, int runTimeStep){
        super(name, outputFileName,runTimeStep);
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
    public synchronized void exportState(String... args) {

          if(!hasAddedHeader){
              writer.println("Timestamp, Device Name, Is Powered");
              hasAddedHeader = true;
            }
            writer.println(getCurrentTimestamp()+"," + object_name + "," + isPowered());
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

}
