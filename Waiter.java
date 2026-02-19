// Waiter.java
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class Waiter extends Thread {

    private final Semaphore nap;
    private final ConcurrentLinkedQueue<Integer> serviceQueue;
    private final ConcurrentHashMap<Integer, Semaphore> servedMap;
    private final Random rng = new Random();

    public Waiter(Semaphore nap,
                  ConcurrentLinkedQueue<Integer> serviceQueue,
                  ConcurrentHashMap<Integer, Semaphore> servedMap) {
        this.nap = nap;
        this.serviceQueue = serviceQueue;
        this.servedMap = servedMap;
    }

    @Override
    public void run() {
        try {
            while (true) {

                // If no customers waiting, go to sleep (block)
                if (!nap.tryAcquire()) {
                    System.out.println("Waiter is sleeping");
                    nap.acquire();                 // blocks until first customer arrives
                    System.out.println("Waiter is now awake");
                }

                // We consumed 1 "waiting customer" permit, so service exactly 1 customer now.
                Integer custNum;
                while ((custNum = serviceQueue.poll()) == null) {
                    // Extremely rare timing edge; yield until the queue has the customer
                    Thread.yield();
                }

                System.out.println("Waiter is servicing customer " + custNum);

                // Simulate service time 50–100 ms
                Thread.sleep(50 + rng.nextInt(51));

                // Signal ONLY that specific customer
                Semaphore served = servedMap.remove(custNum);
                if (served != null) {
                    served.release();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Waiter is now going home!");
        }
    }
}
