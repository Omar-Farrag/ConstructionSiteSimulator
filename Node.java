
public abstract class Node extends SimulationObject{

    private uController controller;
    private int responseDelay;

    public DataPacket queryField(String objectName, String field){
        try {
            Thread.sleep(responseDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(controller){
            return controller.queryField(objectName, field);
        }
    }
    public Boolean setField(String objectName, String field, Float value){
        try {
            Thread.sleep(responseDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized(controller){
            return controller.setField(objectName, field, value);
        }
    }
    
}
