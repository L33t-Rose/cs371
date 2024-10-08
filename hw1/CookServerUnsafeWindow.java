import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

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
		public Semaphore kitchen;
		public Semaphore cookQueue;
		public Semaphore cookToServerBridge;
		public Semaphore serverToCookBridge;

		public Cook(String cookName, UnsafeWindow queue, int maxRun, Semaphore kitchen, Semaphore cookQueue,
				Semaphore cookToServerBridge, Semaphore serverToCookBridge) {
			this.cookName = cookName;
			this.queue = queue;
			this.maxRun = maxRun;
			this.kitchen = kitchen;
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
				try {
					// Kitchen allows for more chefs to actually come in and cook their food
					kitchen.acquire();
					Tray toServe = cook();
					// Once a chef is ready to serve they'll come in one at a time
					cookQueue.acquire();
					queue.add(toServe);
					
					// Signals to the servers that there's a new burger on the queue
					cookToServerBridge.release();
					// Once the server signals to the chef that it's done serving that food
					// Cook can now leave.
					serverToCookBridge.acquire();
					cookQueue.release();
					kitchen.release();
					Thread.sleep((long) Math.random() * 401 + 100);
				} catch (Exception e) {
					System.out.println("error" + e);
				}
			}
		}
	}

	// YOU CAN MODIFY THIS CLASS; DO NOT DELETE ANY GIVEN CODE.
	public static class Server extends Thread {
		private String serverName;
		private UnsafeWindow queue;
		private boolean canContinue;
		public Semaphore cookToServerBridge;
		public Semaphore serverToCookBridge;

		public Server(String serverName, UnsafeWindow queue, Semaphore cookToServerBridge,
				Semaphore serverToCookBridge) {
			this.serverName = serverName;
			this.queue = queue;
			this.canContinue = true;
			this.cookToServerBridge = cookToServerBridge;
			this.serverToCookBridge = serverToCookBridge;
		}

		public synchronized boolean canContinue() {
			return this.canContinue;
		}

		public synchronized void stopRun() {
			this.canContinue = false;

			// Realized that once all the cooks are done my servers would hang cuz they're
			// waiting on cookToServerBridge to open. So this is a way to clean up any
			// leftover threads.
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
			while (canContinue()) {
				try {
					cookToServerBridge.acquire();
					serve();
					serverToCookBridge.release();
					Thread.sleep((long) Math.random() * 401 + 100);
				} catch (Exception e) {
					System.out.println("error" + e);
				}
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		// YOU CAN MODIFY THE CONSTATNS BELOW FOR TESTING.
		int NUM_COOKS = 10;
		int COOK_MAXRUN = 100; // 100
		int NUM_SERVERS = 5;
		int WINDOW_SIZE = 10;

		// YOU CAN MODIFY THE CODES BELOW THIS LINE.
		UnsafeWindow window = new UnsafeWindow(WINDOW_SIZE);
		Cook[] cooks = new Cook[NUM_COOKS];
		Server[] servers = new Server[NUM_SERVERS];
		Semaphore kitchen = new Semaphore(WINDOW_SIZE);
		Semaphore cookQueue = new Semaphore(1);
		Semaphore cookToServer = new Semaphore(0);
		Semaphore serverToCook = new Semaphore(0);

		for (int i = 0; i < NUM_COOKS; i++) {
			cooks[i] = new Cook("p" + i, window, COOK_MAXRUN, kitchen, cookQueue, cookToServer, serverToCook);
			cooks[i].start();
		}

		for (int i = 0; i < NUM_SERVERS; i++) {
			servers[i] = new Server("c" + i, window, cookToServer, serverToCook);
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
