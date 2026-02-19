// Driver.java
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class Driver {

    // Spec semaphores (fair scheduling to avoid starvation)
    private static final Semaphore Door = new Semaphore(8, true);      // 8 seats
    private static final Semaphore Nap = new Semaphore(0, true);       // wake/sleep + queued customers count
    private static final Semaphore mutex = new Semaphore(1, true);     // protects customer numbering

    // Shared structures so waiter can service customers in seating order
    private static final ConcurrentLinkedQueue<Integer> serviceQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<Integer, Semaphore> servedMap = new ConcurrentHashMap<>();

    private static int nextCustomerNum = 0;

    public static void main(String[] args) throws Exception {
        Random rng = new Random();
        Scanner in = new Scanner(System.in);

        Customer[] customers = new Customer[100];

        // Start waiter BEFORE any customers
        Waiter waiter = new Waiter(Nap, serviceQueue, servedMap);
        waiter.start();

        // Give waiter a chance to start and (likely) go to sleep
        Thread.sleep(1000);

        System.out.println("Please hit Enter to start Rush Hour Simulation");
        in.nextLine();

        // Rush Hour: create/start 100 customers; random 0–35ms between starts
        for (int i = 0; i < 100; i++) {
            customers[i] = new Customer(Door, Nap, mutex, serviceQueue, servedMap);
            customers[i].start();
            Thread.sleep(rng.nextInt(36)); // 0..35
        }

        // Wait for all rush-hour customers to finish
        for (int i = 0; i < 100; i++) {
            customers[i].join();
        }

        System.out.println("\n**************************\n");
        System.out.println("Please hit Enter to start Slow Time Simulation");
        in.nextLine();

        // Slow Time: create/start 100 customers; random 50–500ms between starts
        for (int i = 0; i < 100; i++) {
            customers[i] = new Customer(Door, Nap, mutex, serviceQueue, servedMap);
            customers[i].start();
            Thread.sleep(50 + rng.nextInt(451)); // 50..500
        }

        // Wait for all slow-time customers to finish
        for (int i = 0; i < 100; i++) {
            customers[i].join();
        }

        // Interrupt waiter and end
        waiter.interrupt();
        waiter.join();

        System.out.println("\nEND OF SLEEPY WAITER PROBLEM\n");
        in.close();
    }

    // Called only while holding mutex
    public static int getNextCustomerNumberUnsafe() {
        return nextCustomerNum++;
    }
}
