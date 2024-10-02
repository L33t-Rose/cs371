import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CookServerUnsafeWindow {
	// DO NOT MODIFY THIS CLASS.
	public static class Tray {
		public String cook;
		public int numBurgers;

		public Tray(String cook, int numBurgers) {
			this.cook = cook;
			this.numBurgers = numBurgers;
		}
	}

	// DO NOT MODIFY THIS CLASS.
	public static class UnsafeWindow {
		private Queue<Tray> window = new LinkedList<Tray>();
		private int maxSize;
		private int cooked;
		private int served;

		public UnsafeWindow(int maxSize) {
			this.maxSize = maxSize;
			this.cooked = 0;
			this.served = 0;
		}

		public boolean isFull() {
			return window.size() == maxSize;
		}

		public boolean isEmpty() {
			return window.size() == 0;
		}

		public int size() {
			return window.size();
		}

		public boolean add(Tray d) {
			if (isFull()) {
				return false;
			} else {
				window.add(d);
				this.cooked += d.numBurgers;
				return true;
			}
		}

		public Tray remove() {
			if (isEmpty()) {
				return null;
			} else {
				Tray d = window.poll();
				this.served += d.numBurgers;
				return d;
			}
		}

		public int getCooked() {
			return cooked;
		}

		public int getServed() {
			return served;
		}
	}

	// YOU CAN MODIFY THIS CLASS; DO NOT DELETE ANY GIVEN CODE.
	public static class Cook extends Thread {
		private String cookName;
		private UnsafeWindow queue;
		private int maxRun;
		public Lock windowLock;
		public Semaphore cookQueue;
		public Semaphore cookToServerBridge;
		public Semaphore serverToCookBridge;

		public Cook(String cookName, UnsafeWindow queue, int maxRun, Lock windowLock, Semaphore cookQueue,
				Semaphore cookToServerBridge, Semaphore serverToCookBridge) {
			this.cookName = cookName;
			this.queue = queue;
			this.maxRun = maxRun;
			this.windowLock = windowLock;
			this.cookQueue = cookQueue;
			this.cookToServerBridge = cookToServerBridge;
			this.serverToCookBridge = serverToCookBridge;
		}

		private Tray cook() {
			int numBurgers = (int) (Math.random() * 5) + 1;
			System.out.println(">>> Cook: " + cookName + " Value: " + numBurgers + " Size: " + queue.size());
			return new Tray(cookName, numBurgers);
		}

		// FILL IN THE METHOD.
		public void run() {
			for (int i = 0; i < maxRun; i++) {
				System.out.println("new run " + maxRun + "hi");
				try {

					cookQueue.acquire();
					System.out.println(">>> Cook: " + cookName + " joined the cookQueue slot");

					windowLock.lock();
					System.out.println(">>> Cook: " + cookName + " picked up the lock");
					boolean res = queue.add(cook());
					windowLock.unlock();
					if (!res) {
						System.out.println("NOO");
					} else {
						System.out.println(">>> Cook: " + cookName + " Successfully added to the queue");
					}
					System.out.println(">>> Cook: " + cookName + " opened cookToServer bridge");
					cookToServerBridge.release();
					serverToCookBridge.acquire();
					System.out.println(">>> Cook: " + cookName + " came back from server finishing task");
					// System.out.println(">>> Cook: " + cookName + " released the lock.");
					cookQueue.release();
				} catch (Exception e) {

				}
				try {
					System.out.println(">>> Cook: " + cookName + " is sleeping");

					Thread.sleep((long) Math.random() * 401 + 100);
				} catch (Exception e) {
					System.out.println("error" + e);
				}
				System.out.println(">>> Cook: " + cookName + " woke up");

			}
			System.out.println("Cook:" + cookName + " just finished all runs" + queue.getCooked());
		}
	}

	// YOU CAN MODIFY THIS CLASS; DO NOT DELETE ANY GIVEN CODE.
	public static class Server extends Thread {
		private String serverName;
		private UnsafeWindow queue;
		private boolean canContinue;
		public Lock windowLock;
		public Semaphore cookToServerBridge;
		public Semaphore serverToCookBridge;

		public Server(String serverName, UnsafeWindow queue, Lock windowLock, Semaphore cookToServerBridge,
				Semaphore serverToCookBridge) {
			this.serverName = serverName;
			this.queue = queue;
			this.canContinue = true;
			this.windowLock = windowLock;
			this.cookToServerBridge = cookToServerBridge;
			this.serverToCookBridge = serverToCookBridge;
		}

		public synchronized boolean canContinue() {
			return this.canContinue;
		}

		public synchronized void stopRun() {
			this.canContinue = false;
			
			// Realized that once all the cooks are done my servers would hang cuz they're
			// waiting on cookToServerBridge to open. So this is a way to clean up any leftover threads.
			while (cookToServerBridge.hasQueuedThreads()) {
				cookToServerBridge.release();
			}
		}

		private Tray serve() {
			Tray d = queue.remove();
			if (d == null) {
				return null;
			}
			System.out.println("<<< Cook: " + d.cook + " Server: " + serverName + " Value: " + d.numBurgers + " Size: "
					+ queue.size());
			return d;
		}

		// FILL IN THE METHOD.
		public void run() {
			// System.out.println(">>> Server:" + serverName + " Processing...");
			while (canContinue) {
				try {

					cookToServerBridge.acquire();
					System.err.println(serverName + "where am I stuck?");
					System.out.println("<<< Server: " + serverName + " just jumped in from cookToServer bridge");

					windowLock.lock();
					System.out.println("<<< Server: " + serverName + " picked up the mutex lock");
					Tray t = serve();
					windowLock.unlock();
					if (t != null) {
						System.out.println("<<< Server: " + serverName + " removed from the window");
					}
					System.out
							.println("<<< Server: " + serverName + " contacted the cook from the serverToCook bridge");
					serverToCookBridge.release();
					System.out.println("<<< Server: " + serverName + " is going to sleep");
					Thread.sleep((long) Math.random() * 401 + 100);
				} catch (Exception e) {
					System.out.println("error" + e);
				}
				System.out.println("<<< Server: " + serverName + " woke up");
			}
			System.out.println("Server: " + serverName + " finsihed");
		}
	}

	public static void main(String[] args) throws InterruptedException {
		// YOU CAN MODIFY THE CONSTATNS BELOW FOR TESTING.
		int NUM_COOKS = 10;
		int COOK_MAXRUN = 100; // 100
		int NUM_SERVERS = 5;
		int WINDOW_SIZE = 3;

		// YOU CAN MODIFY THE CODES BELOW THIS LINE.
		UnsafeWindow window = new UnsafeWindow(WINDOW_SIZE);
		Cook[] cooks = new Cook[NUM_COOKS];
		Server[] servers = new Server[NUM_SERVERS];
		ReentrantLock lock = new ReentrantLock();
		Semaphore s_cooks = new Semaphore(1);
		Semaphore cookToServer = new Semaphore(0);
		Semaphore serverToCook = new Semaphore(0);

		for (int i = 0; i < NUM_COOKS; i++) {
			cooks[i] = new Cook("p" + i, window, COOK_MAXRUN, lock, s_cooks, cookToServer, serverToCook);
			cooks[i].start();
		}

		for (int i = 0; i < NUM_SERVERS; i++) {
			servers[i] = new Server("c" + i, window, lock, cookToServer, serverToCook);
			servers[i].start();
		}

		// DO NOT MODIFY ANYTHING BELOW THIS LINE.
		for (int i = 0; i < NUM_COOKS; i++) {
			cooks[i].join();
		}
		System.out.println("All of the cooks are finished");
		for (int i = 0; i < NUM_SERVERS; i++) {
			servers[i].stopRun();
			servers[i].join();
		}

		System.out.println(window.getCooked() + " " + window.getServed());
		System.exit(0);
	}
}
