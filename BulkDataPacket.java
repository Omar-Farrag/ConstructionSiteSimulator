import java.util.ArrayList;
import java.util.Collection;


public class BulkDataPacket {

    private String sourceNodeName;
    ArrayList<DataPacket> packets;
    private int totalSize; // Bytes
    private String time_of_creation;

    
    
    public BulkDataPacket(String sourceNodeName, String time_of_creation) {
        this.sourceNodeName = sourceNodeName;
        this.time_of_creation = time_of_creation;
        packets = new ArrayList<>();
    }

    public void addPacket(DataPacket packet){
        packets.add(packet);
        totalSize += packet.getSize();
    }
    public void addPackets(Collection<DataPacket> packetsToAdd){
        packets.addAll(packetsToAdd);
        for(DataPacket packet : packetsToAdd) totalSize += packet.getSize();
    }

    
    public String getSourceNodeName() {
        return sourceNodeName;
    }

    public ArrayList<DataPacket> getPackets() {
        return packets;
    }

    public int getSize() {
        return totalSize;
    }

    public String getTime_of_creation() {
        return time_of_creation;
    }

}
