import java.util.ArrayList;

public class Gateway extends SimulationObject{

    private uController parentController;
    ArrayList<Gateway> connectedGateways;

    public Gateway(String object_name, String outputFileName, int runTimeStep){
        super(object_name, outputFileName, runTimeStep);
        connectedGateways = new ArrayList<>();
    }

    public void connectTo(Gateway... gateways){
        for(Gateway gateway : gateways) {
            connectedGateways.add(gateway);        
        }
    }
    public void setParentController(uController parentController) {
        this.parentController = parentController;
    }
    public boolean forward(Gateway source, Gateway previous, DataPacket packet, String route, int position){
        String[] routeComponents = route.split(",");
        if(position == (routeComponents.length-1)){
    
            if(routeComponents[position].equalsIgnoreCase(this.object_name)){
                parentController.receiveDataPacket(packet);
                exportState(String.format("[SUCCESS] Received packet from Gateway [%s]. Last Forwarded By Gateway [%s]", source.getObject_name(), previous.getObject_name()));
                return true;
            }
            else{
                exportState(String.format("[FAILURE] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), routeComponents[position]));
                return false;
            }
        }
        
        String nextGatewayName = routeComponents[position+1];
        for(Gateway nextGateway : connectedGateways){
            if(nextGateway.getObject_name().equalsIgnoreCase(nextGatewayName)){
                exportState(String.format("[SUCCESS] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), nextGateway.getObject_name()));
                return nextGateway.forward(source, this, packet, route, position+1);
            }
        }
        exportState(String.format("[FAILURE] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), nextGatewayName ));
        return false;
    }

    public Node getParentNode(){
        return parentController.getParentNode();
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
