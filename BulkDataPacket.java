import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is a container for a list of DataPacket objects
 */
public class BulkDataPacket {

    //Name of the node that created the BulkDataPacket object 
    private String sourceNodeName;

    // Time at which the BulkDataPacket object was created
    private String time_of_creation;
    
    //List of packets aggregated in the BulkDataPacket
    ArrayList<DataPacket> packets;

    //Sum of the sizes in Bytes of the DataPacket objects in 'packets'
    private int totalSize; 

    /**
     * Constructor
     * @param sourceNodeName
     * @param time_of_creation
     */
    public BulkDataPacket(String sourceNodeName, String time_of_creation) {
        this.sourceNodeName = sourceNodeName;
        this.time_of_creation = time_of_creation;
        packets = new ArrayList<>();
    }

    /**
     * Add a packet to the list of packets included in the BulkDataPacket
     * @param packet
     */
    public void addPacket(DataPacket packet){
        packets.add(packet);
        totalSize += packet.getSize();
    }

    /**
     * Add multiple packets at once to the list of packets included in the BulkDataPacket
     * @param packetsToAdd
     */
    public void addPackets(Collection<DataPacket> packetsToAdd){
        packets.addAll(packetsToAdd);
        for(DataPacket packet : packetsToAdd) totalSize += packet.getSize();
    }

    /**
     * Getter
     * @return sourceNodeName
     */
    public String getSourceNodeName() {
        return sourceNodeName;
    }
    
    /**
     * Getter
     * @return packets
     */
    public ArrayList<DataPacket> getPackets() {
        return packets;
    }
    
    /**
     * Getter
     * @return totalSize
     */
    public int getSize() {
        return totalSize;
    }

    /**
     * Getter
     * @return time_of_creation
     */
    public String getTime_of_creation() {
        return time_of_creation;
    }

}
