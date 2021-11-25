import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * In the constructor, the ring is initialized, that is, all the nodes and data on the nodes are created.
 * In the {@link RingProcessor # startProcessing ()} method, the  ring is started -
 * the data starts processinf clockwise. Also, logging to {@link RingProcessor#logs}.
 * happens. All work must be thread safe and handle all possible exceptions. If necessary, you can create your own exception classes.
 */
public class RingProcessor {
    private final int nodesAmount;
    private final int dataAmount;

    private final File logs;

    private List<Node> nodeList;

    private final Logger logger;
    FileHandler fileHandler;
    ArrayList<Thread> threads;

    /**
     * A record of the transit time of each data package.
     * Used in {@link RingProcessor # averageTime ()} to calculate average time
     * of reaching the coordinator by the data.
     */

    List<Long> timeList;

    RingProcessor(int nodesAmount, int dataAmount, File logs) throws IOException, InterruptedException {
        this.nodesAmount = nodesAmount;

        this.dataAmount = dataAmount;

        this.logs = logs;
        nodeList = new ArrayList<>();
        logger = Logger.getLogger("ringLogger");
        fileHandler  = new FileHandler(logs.getName(), true);
        logger.addHandler(fileHandler);
        logger.info("Number of Nodes: " + nodesAmount);
        logger.info("Data on each node: " + dataAmount);
        init();
    }

    // Computation of the average traversing time.
    private long averageTime() {
        return 0;
    }

    private void init() throws IOException, InterruptedException {
        Random rand = new Random();
        int coordinator = rand.nextInt(nodesAmount) + 1;
        logger.info("Coordinator Node id: " + coordinator);
        threads = new ArrayList<>();
        //initialize ring
        for (int i = 1; i <= nodesAmount; ++i) {
            RingProcessor temp = null;
            if (i == coordinator) {
                temp = this;
            }
            Node newNode = new Node(i, coordinator, dataAmount, temp, fileHandler, logger);
            nodeList.add(newNode);
            threads.add(new Thread(newNode, String.valueOf(i)));
            int receiveNode = ThreadLocalRandom.current().nextInt(nodesAmount) + 1;
            if (receiveNode == i) {
                if (receiveNode == nodesAmount) {
                    receiveNode -= 1;
                } else {
                    receiveNode += 1;
                }
            }

            DataPackage dataPackage = new DataPackage(receiveNode, generateRandomString(i));
            newNode.setData(dataPackage);
            System.out.println(i + " " + dataPackage + " " + receiveNode);
        }

        for (int i = 0; i < nodesAmount; ++i) {
            int j = min(i + 1, nodesAmount) == nodesAmount ? 0 : i + 1;
            nodeList.get(i).addNodeAndCoord(nodeList.get(j), nodeList.get(coordinator - 1));
        }
    }

    public void startProcessing() throws InterruptedException {
        for (int i = 0; i < nodesAmount; ++i) {
            threads.get(i).start();
        }
    }

    public void interruptAllNodes() {
        System.out.println("Shutting threads down");
        for (Thread t : threads) {
            t.interrupt();
        }

        System.exit(0);
    }

    public List<Node> getNodeList() {
        return nodeList;
    }

    private String generateRandomString(int length) {
        final Random RANDOM = new SecureRandom();
        final String ALPHABET = "0123456789QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm";
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            buffer.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new String(buffer);
    }
}
