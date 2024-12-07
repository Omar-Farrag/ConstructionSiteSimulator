import java.util.HashMap;
import java.util.Map.Entry;

/**
 * This class is the gateway of a zone. It communicates with other
 * gateways to send and receive data packets. It acts a like a wireless 
 * router
 */
public class Gateway extends SimulationObject{

    // Data rate for WiFi transmissions in kbps
    private int WIFI_Transmission_Rate;

    // uController connected to this gateway
    private uController parentController;
    
    // List of gateways wirelessly connected to
    // the current gateway. The key of each pair
    // is the connected gateway while the value
    // is the RTT from the current gateway to 
    // the connected gateway 
    HashMap<Gateway,Integer> connectedGateways;

    /**
     * Constructor
     * @param object_name Name of the gateway object
     * @param WiFi_Transmission_Rate WiFi transmission rate in kbps for data sent between gateways
     */
    public Gateway(String object_name, int WIFI_TRANSMISSION_RATE){
        super(object_name);
        connectedGateways = new HashMap<>();
        this.WIFI_Transmission_Rate = WIFI_TRANSMISSION_RATE;
    }

    /**
     * Function to connect the current gateway to a new gateway.
     * This adds the new gateway to the current gateway's list of connected gateways
     * It also adds the current gateway to the new gateways's list of connected gateways
     * @param gateway new gateway to connect to
     * @param RTT_to_gateway RTT between current and new gateways
     */
    public void connectTo(Gateway gateway, int RTT_to_gateway){
        this.addConnectedGateway(gateway,RTT_to_gateway);
        gateway.addConnectedGateway(this, RTT_to_gateway);        
    }

    /**
     * Low level function to perform the actual addition of a gatway
     * to the list of connected gateways
     */
    private void addConnectedGateway(Gateway gatway, int RTT_to_gateway){
        connectedGateways.put(gatway, RTT_to_gateway);
    }

    /**
     * Function to set a uController as the controller connected to this gateway
     * @param parentController
     */
    public void setParentController(uController parentController) {
        this.parentController = parentController;
    }
    
    /**
     * Function to forward a BulkDataPacket to the next gateway along a predefined route
     * @param source first gateway in the route
     * @param previous previous gateway in the route that forwarded the packet to the current gateway
     * @param packet BulkDataPacket being forwarded
     * @param route Route along which the packet is forwaded. Must be a comma separated list of gateway names
     * @param position Current position of the packet along the route. Initially 0, incremented each time packet is forwarded along route
     * @return true if the packet has arrived at its last desination, false otherwise
     * 
     */
    public boolean forward(Gateway source, Gateway previous, BulkDataPacket packet, String route, int position){
        String[] routeComponents = route.split(",");
        
        // If current position is the last in the route 
        if(position == (routeComponents.length-1)){
            // Check if the last position in the route matches the current gateway 
            if(routeComponents[position].equalsIgnoreCase(this.object_name)){
                // Add a log message to the gateway's log file
                exportState(String.format("[SUCCESS] Received packet from Gateway [%s]. Last Forwarded By Gateway [%s]", source.getObject_name(), previous.getObject_name()));
                
                // Share the received packet with the connected uController
                parentController.receiveBulkDataPacket(source, previous, packet);
                return true;
            }
            // Unexpected Behavior
            else{
                exportState(String.format("[FAILURE] Forwarded packet from Gateway [%s] to Gateway [%s]. Reached End of Route", previous.getObject_name(), routeComponents[position]));
                return false;
            }
        }
        // If not the end of the route, retreive the name of next gateway
        String nextGatewayName = routeComponents[position+1];
       
        // Search in the current gateway's list of connected gateways to find the next gateway 
        for(Entry<Gateway,Integer> nextGateway : connectedGateways.entrySet()){
            // If the next gateway is found
            if(nextGateway.getKey().getObject_name().equalsIgnoreCase(nextGatewayName)){
                
                //Add a log message to the gateway's output log file
                exportState(String.format("[SUCCESS] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), nextGateway.getKey().getObject_name()));
                
                // Wait for a delay simulating the transmission of the packet.
                // Delay = RTT/2 + packet size/transmission rate
                try {
                    int time_delay= nextGateway.getValue()/2 + packet.getSize() / (WIFI_Transmission_Rate);
                    Thread.sleep(time_delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Forward the packet to the next gateway
                return nextGateway.getKey().forward(source, this, packet, route, position+1);
            }
        }
        // If the next gateway was not found in the current gateway's list of connected gateways, add a log message
        exportState(String.format("[FAILURE] Forwarded packet from Gateway [%s] to Gateway [%s]", previous.getObject_name(), nextGatewayName ));
        return false;
    }

    /**
     * Get the node that contains this gateway
     * Most likely a master node in some zone
     * @return reference to parent master node
     */
    public MasterNode getParentNode(){
        return parentController.getParentMasterNode();
    }

    /**
     * Function to run continuously in the Gateway's runtime thread with a timestep of runTimeStep. This assumes
     * that a thread was started for the gateway
     */
    @Override
    protected void runTimeFunction() {
        // Do Nothing. Gateway does not perform any actions on its own.
        // It is controlled by the microcontroller
    }

    /**
     * Override of the hashcode function. Necessary for storing a gateway in a hashmap
     */
    @Override
    public int hashCode() {
        return object_name.hashCode();
    }

    /**
     * Override of the equals function. Necessary for storing a gateway in a hashmap
     */
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

    /**
     * Start the object without starting a thread
     */
    @Override
    public void start() {
        super.start(false,0);
        exportState("Started");
    }
    

}
