import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class unitTest {
    @Test
    public void testSetData() throws InterruptedException, IOException {
        RingProcessor processor = new RingProcessor(3, 3, new File("logPath"));

        List<Node> nodes = processor.getNodeList();
        String data = nodes.get(0).getDataPackage().getData();
        nodes.get(1).setData(nodes.get(0).getDataPackage());

        assertEquals(nodes.get(1).getBuffer().getLast().getData(), data);
    }
}
