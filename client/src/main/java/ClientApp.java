import java.io.IOException;
import java.nio.file.Paths;

public class ClientApp {
    public static void main(String[] args) throws IOException {
        new NetStorageClient(new ClientGUI());
    }
}
