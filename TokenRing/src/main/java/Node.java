import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;


public class Node implements Runnable {
    static int myExpectedSize;
    private final int NODE_ID;
    private final boolean IS_COORD;
    private final int COORD_ID;
    private Node coordNode;
    private final int DATA_AMOUNT;
    final RingProcessor RING_PROCESSOR;
    private Node nextNode;
    private final FileHandler FILE_HANDLER;
    private final Logger LOGGER;
    private long bufferStayTime = 0;
    private int numOfBufferEntries = 0;
    static final Lock LOCK = new ReentrantLock();
    static boolean shouldStop = false;
    private final int TOTAL_NUM_OF_PACKAGES;


    private BufferStack<DataPackage> bufferStack = new BufferStack<>();
    private BufferStack<DataPackage> myPackages = new BufferStack<>();

    public List<DataPackage> allData;

    public Node(int nodeId, int cordId, int dataAmount, RingProcessor ringProcessor, FileHandler fileHandler, Logger logger, int totalNumOfPackages) {
        this.NODE_ID = nodeId;
        this.TOTAL_NUM_OF_PACKAGES = totalNumOfPackages;
        this.COORD_ID = cordId;
        this.DATA_AMOUNT = dataAmount;
        this.RING_PROCESSOR = ringProcessor;
        this.FILE_HANDLER = fileHandler;
        this.LOGGER = logger;

        if(nodeId == cordId) {
            allData = new ArrayList<>();
            IS_COORD = true;
        } else {
            IS_COORD = false;
        }
    }

    public void addNodeAndCoord(Node nextNode, Node Coord) {
        this.nextNode = nextNode;
        this.coordNode = Coord;
    }

    private void addDeliveredPackage(DataPackage dataPackage) {
        allData.add(dataPackage);
    }

    public long getId() {
        return NODE_ID;
    }

    public void setData(DataPackage dataPackage) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            Condition bufferStackFull = lock.newCondition();
            if (bufferStack.size() == 3) {
                System.out.println(Thread.currentThread().getName() + " is waiting for buffer to reduce.");
                bufferStackFull.await();
            }

            if (dataPackage.getDESTINATION_NODE() == NODE_ID) {
                dataPackage.setEndTime(System.nanoTime());
                myPackages.add(dataPackage);
                bufferStack.add(dataPackage);
                LOGGER.info("Thread " + Thread.currentThread().getName() + " of Node " + NODE_ID + " Received my data package " + dataPackage);
                coordNode.addDeliveredPackage(dataPackage);
                if (myPackages.size() == myExpectedSize) {
                    System.out.println("Node " + NODE_ID + " is done");
                    shouldStop = true;
                }
            } else {
                dataPackage.setBufferEntryTime(System.nanoTime());
                bufferStack.add(dataPackage);
                ++numOfBufferEntries;
            }
            bufferStackFull.signal();
        } finally {
            lock.unlock();
        }

        Thread.sleep(1); // because it is written in the question to sleep for 1 millisecond
    }

    public DataPackage getDataPackage() {
        return bufferStack.getFirst();
    }

    public BufferStack<DataPackage> getBuffer() {
        return bufferStack;
    }

    /**
     * Start of the node. I.e., a package with data is taken from Node.bufferStack
     * and is sent for processing. After that it is transferred to the next node.
     * Here lies the logic according to which only 3 data packages can be processed at the same time.
     */
    @Override
    public void run() {
        while (!coordNode.shouldStop) {
            if (IS_COORD) {
                if (allData.size() == TOTAL_NUM_OF_PACKAGES) {
                    long sum = 0;
                    for (DataPackage dataPackage : allData) {
                        sum = sum + dataPackage.getEndTime() - dataPackage.getSTART_TIME();
                    }
                    coordNode.shouldStop = true;
                    System.out.println(sum);
                    RING_PROCESSOR.avgTime = (sum / allData.size());
                }
            }
            try {
                ReentrantLock lock = new ReentrantLock();
                try {
                    lock.lock();
                    Condition bufferStackEmpty = lock.newCondition();
                    while (bufferStack.isEmpty()) {
                        System.out.println("Buffer of thread " + Thread.currentThread().getName() + " is empty. Waiting for data");
                        bufferStackEmpty.await();
                    }

                    nextNode.setData(getDataPackage());
                    LOGGER.info(getDataPackage() + "was transferred from " + NODE_ID + " to " + (NODE_ID + 1));
                    bufferStackEmpty.signal();
                    System.out.println("Buffer of thread " + Thread.currentThread().getName() + " now has some data");
                    bufferStayTime += (System.nanoTime() - getDataPackage().getBufferEntryTime());
                    bufferStack.removeFirst();
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(NODE_ID + " Coord Node stopped");
    }
}

