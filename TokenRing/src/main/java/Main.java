import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        RingProcessor processor = new RingProcessor(3, 3, new File("logPath"));

        processor.startProcessing();
    }

}

