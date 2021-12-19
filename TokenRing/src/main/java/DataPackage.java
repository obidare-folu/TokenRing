public class DataPackage {
    private final int DESTINATION_NODE;

    private final String DATA;

    private final long START_TIME;

    private long endTime;

    private long bufferEntryTime;

    public DataPackage(int DESTINATION_NODE, String data) {
        this.DESTINATION_NODE = DESTINATION_NODE;

        this.DATA = data;

        // The time when the data package is created. Necessary for
        // calculation of the delivery time to the destination node.

        START_TIME = System.nanoTime();
    }


    public int getDESTINATION_NODE() {
        return DESTINATION_NODE;
    }

    public long getSTART_TIME() {
        return START_TIME;
    }

    void setEndTime(long time) {
        endTime = time;
    }

    public long getEndTime() {
        return endTime;
    }


    public void setBufferEntryTime(long bufferEntryTime) {
        this.bufferEntryTime = bufferEntryTime;
    }

    public long getBufferEntryTime() {
        return bufferEntryTime;
    }

    public String getDATA(){
        return DATA;
    }
}
