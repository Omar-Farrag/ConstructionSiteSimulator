public class DataPacket {

    private String time_of_creation;
    private String sourceObjectName;
    private String fieldName ;
    private String value;
    private int size; // Bytes

    
    
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

    public static String getHeader(){
        return "Time of Creation, Source Object, Field Name, Value, Size";
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%d",
        time_of_creation, sourceObjectName, fieldName, value, size);
    }
    

    
}
