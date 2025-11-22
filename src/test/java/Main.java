
public class Main {
    public static void main(String[] args) throws InterruptedException {
        TestIPC.startup();

        Runtime.getRuntime().addShutdownHook(new Thread(TestIPC::shutdown));

        while (true) {
            Thread.sleep(50);
        }
    }
}
