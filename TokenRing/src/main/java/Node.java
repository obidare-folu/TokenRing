import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;


public class Node implements Runnable {
    private final int nodeId;
    private final boolean isCoord;
    private final int cordId;
    private Node coordNode;
    private final int dataAmount;
    final RingProcessor ringProcessor;
    private Node nextNode;
    private final FileHandler fileHandler;
    private final Logger logger;
    private long bufferStayTime = 0;
    private int numOfBufferEntries = 0;
    static final Lock lock = new ReentrantLock();
    static final Condition bufferStackFull = lock.newCondition();
    static final Condition bufferStackEmpty = lock.newCondition();

//    static final Object bufferStackFull = new Object();
//    static final Object bufferStackEmpty = new Object();

    private BufferStack<DataPackage> bufferStack = new BufferStack<>();
    private BufferStack<DataPackage> myPackages = new BufferStack<>();

    public List<DataPackage> allData;

    Node(int nodeId, int cordId, int dataAmount, RingProcessor ringProcessor, FileHandler fileHandler, Logger logger) {
        this.nodeId = nodeId;

        this.cordId = cordId;
        this.dataAmount = dataAmount;
        this.ringProcessor = ringProcessor;
        this.fileHandler = fileHandler;
        this.logger = logger;

        if(nodeId == cordId) {
            allData = new ArrayList<>();
            isCoord = true;
        } else {
            isCoord = false;
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
        return nodeId;
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

            if (dataPackage.getDestinationNode() == nodeId) {
                dataPackage.setEndTime(System.nanoTime());
                myPackages.add(dataPackage);
                bufferStack.add(dataPackage);
                logger.info("Thread " + Thread.currentThread().getName() + " of Node " + nodeId + " Received my data package " + dataPackage);
//                System.out.println("Thread " + Thread.currentThread().getName() + " of Node " + nodeId + " Received my data package " + dataPackage);
                coordNode.addDeliveredPackage(dataPackage);
            } else {
                dataPackage.setBufferEntryTime(System.nanoTime());
                bufferStack.add(dataPackage);
                ++numOfBufferEntries;
            }
            bufferStackFull.signal();



        } finally {
            lock.unlock();
        }

//        lock.lock();
//        try {
//            while (bufferStack.size() == 3) {
//                bufferStackFull.await();
//            }
//
//            if (dataPackage.getDestinationNode() == nodeId) {
//                dataPackage.setEndTime(System.nanoTime());
//                myPackages.add(dataPackage);
//                logger.info("Thread " + Thread.currentThread().getName() + " Received my data package " + dataPackage);
//                coordNode.addDeliveredPackage(dataPackage);
//            } else {
//                dataPackage.setBufferEntryTime(System.nanoTime());
//                bufferStack.add(dataPackage);
//                ++numOfBufferEntries;
//            }
//
//            bufferStackEmpty.signal();
//        } finally {
//            lock.unlock();
//        }
        Thread.sleep(1);
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
        while (!Thread.currentThread().isInterrupted()) {
            if (isCoord) {
                System.out.println("I'm in charge");
                if (allData.size() == 3) {
                    System.out.println("But I should do this");
                    long sum = 0;
                    for (DataPackage dataPackage : allData) {
                        sum = sum + dataPackage.getEndTime() - dataPackage.getStartTime();
                    }
                    logger.info("Average Delivery time = " + (sum / allData.size()));
                    ringProcessor.interruptAllNodes();
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
                    logger.info(getDataPackage() + "was transferred from " + nodeId + " to " + (nodeId + 1));
//                    System.out.println(getData() + "was transferred from " + nodeId + " to " + (nodeId + 1));
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


//            lock.lock();
//            try {
//                while (bufferStack.isEmpty()) {
//                    try {
//                        bufferStackEmpty.await();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                nextNode.setData(getData());
//                bufferStackFull.signal();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } finally {
//                lock.unlock();
//            }

//            logger.info(getData() + "was transferred from " + nodeId + " to " + (nodeId + 1));
        }
    }
}

