public class DataPackage {
    private final int destinationNode;

    private final String data;

    private final long startTime;

    private long endTime;

    private long bufferEntryTime;

    DataPackage(int destinationNode, String data) {
        this.destinationNode = destinationNode;

        this.data = data;

        // The time when the data package is created. Necessary for
        // calculation of the delivery time to the destination node.

        startTime = System.nanoTime();
    }


    public int getDestinationNode() {
        return destinationNode;
    }

    public long getStartTime() {
        return startTime;
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

    public String getData(){
        return data;
    }
}
