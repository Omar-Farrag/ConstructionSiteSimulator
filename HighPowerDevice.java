
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
    public void exportState() {
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

}
