import java.util.HashMap;
import java.util.Map.Entry;

public class Gateway extends SimulationObject{

    final int WIFI_TRANSMISSION_RATE = 2000; //kbps

    private uController parentController;
    HashMap<Gateway,Integer> connectedGateways;

    public Gateway(String object_name, int runTimeStep){
        super(object_name, runTimeStep);
        connectedGateways = new HashMap<>();
    }

    public void connectTo(Gateway gateway, int RTT_to_gateway){
        connectedGateways.put(gateway,RTT_to_gateway);
        gateway.addConnectedGateway(this, RTT_to_gateway);        
    }

    private void addConnectedGateway(Gateway gatway, int RTT_to_gateway){
        connectedGateways.put(gatway, RTT_to_gateway);
    }
    
    public void setParentController(uController parentController) {
        this.parentController = parentController;
    }
    
    public boolean forward(Gateway source, Gateway previous, BulkDataPacket packet, String route, int position){
        String[] routeComponents = route.split(",");
        if(position == (routeComponents.length-1)){
    
            if(routeComponents[position].equalsIgnoreCase(this.object_name)){
                parentController.receiveDataPacket(source, previous, packet);
                exportState(String.format("[SUCCESS] Received packet from Gateway [%s]. Last Forwarded By Gateway [%s]", source.getObject_name(), previous.getObject_name()));
                return true;
            }
            else{
                exportState(String.format("[FAILURE] Forwarded packet from Gateway [%s] to Gateway [%s]. Reached End of Route", previous.getObject_name(), routeComponents[position]));
                return false;
            }
        }
        
        String nextGatewayName = routeComponents[position+1];
        for(Entry<Gateway,Integer> nextGateway : connectedGateways.entrySet()){
            if(nextGateway.getKey().getObject_name().equalsIgnoreCase(nextGatewayName)){
                exportState(String.format("[SUCCESS] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), nextGateway.getKey().getObject_name()));
                try {
                    int time_delay= nextGateway.getValue()/2 + packet.getSize() / (WIFI_TRANSMISSION_RATE);
                    Thread.sleep(time_delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return nextGateway.getKey().forward(source, this, packet, route, position+1);
            }
        }
        exportState(String.format("[FAILURE] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), nextGatewayName ));
        return false;
    }

    public ControlNode getParentNode(){
        return parentController.getParentControlNode();
    }

    @Override
    protected void runTimeFunction() {
        // Do Nothing. Gateway does not perform any actions on its own.
        // It is controlled by the microcontroller
    }

    @Override
    public int hashCode() {
        return object_name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Gateway other = (Gateway) obj;
        return object_name.equals(other.getObject_name());
    }

    

}
