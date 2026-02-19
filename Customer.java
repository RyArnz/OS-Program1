// Customer.java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class Customer extends Thread {

    private final Semaphore door;
    private final Semaphore nap;
    private final Semaphore mutex;

    private final ConcurrentLinkedQueue<Integer> serviceQueue;
    private final ConcurrentHashMap<Integer, Semaphore> servedMap;

    public Customer(Semaphore door,
                    Semaphore nap,
                    Semaphore mutex,
                    ConcurrentLinkedQueue<Integer> serviceQueue,
                    ConcurrentHashMap<Integer, Semaphore> servedMap) {
        this.door = door;
        this.nap = nap;
        this.mutex = mutex;
        this.serviceQueue = serviceQueue;
        this.servedMap = servedMap;
    }

    @Override
    public void run() {
        int myNum = -1;
        Semaphore myServed = new Semaphore(0, true);

        try {
            System.out.println(Thread.currentThread().getName() + " is attempting to enter restaurant");

            // Wait outside if full (only 8 seats)
            door.acquire();

            // Get customer number in the order they enter
            mutex.acquire();
            myNum = Driver.getNextCustomerNumberUnsafe();
            System.out.println(Thread.currentThread().getName() + " is now customer " + myNum);
            mutex.release();

            System.out.println("Customer " + myNum + " has entered restaurant and is seated");

            // Register my personal "served" semaphore and enqueue myself for FIFO service
            servedMap.put(myNum, myServed);
            serviceQueue.add(myNum);

            // Greet / wake waiter + increment waiting-customer count
            nap.release();

            System.out.println("Customer " + myNum + " is waiting for the waiter");

            // Wait until waiter serves *me*
            myServed.acquire();

            System.out.println("Customer " + myNum + " has been served");
            System.out.println("Customer " + myNum + " is leaving");

            // Free up a seat for someone outside
            door.release();

        } catch (InterruptedException e) {
            // If interrupted, attempt to not leak a seat if we had one
            if (myNum >= 0) {
                servedMap.remove(myNum);
            }
            // Best-effort: if we acquired Door, we should release it.
            // We can't perfectly know here, but typical runs won't interrupt customers.
        } finally {
            // If mutex was acquired and exception hit before release (unlikely), avoid deadlock.
            if (mutex.availablePermits() == 0) {
                mutex.release();
            }
        }
    }
}
