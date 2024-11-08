public class DataPacket {

    private String sourceObjectName;
    private String fieldName ;
    private String value;
    private int size; // Bytes
    private String time_of_creation;

    
    
    public DataPacket(String sourceObjectName, String fieldName, String value, int size, String time_of_creation) {
        this.sourceObjectName = sourceObjectName;
        this.fieldName = fieldName;
        this.value = value;
        this.size = size;
        this.time_of_creation = time_of_creation;
    }



    public String getSourceObjectName() {
        return sourceObjectName;
    }



    public String getFieldName() {
        return fieldName;
    }



    public String getValue() {
        return value;
    }



    public int getSize() {
        return size;
    }



    public String getTime_of_creation() {
        return time_of_creation;
    }

    
}
