public class DataPacket {

    private String name;
    private Float value;
    private int size; // Bytes
    private String time_of_creation;

    
    public DataPacket(String name, Float value, int size, String time_of_creation) {
        this.name = name;
        this.value = value;
        this.size = size;
        this.time_of_creation = time_of_creation;
    }


    public String getName() {
        return name;
    }


    public Float getValue() {
        return value;
    }


    public int getSize() {
        return size;
    }


    public String getTime_of_creation() {
        return time_of_creation;
    }

    

    

}
