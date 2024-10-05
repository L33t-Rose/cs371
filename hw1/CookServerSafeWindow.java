import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.*;

public class CookServerSafeWindow {
	// DO NOT MODIFY THIS CLASS.
	public static class Tray {
		public String cook;
		public int numBurgers;

		public Tray(String cook, int numBurgers) {
			this.cook = cook;
			this.numBurgers = numBurgers;
		}
	}

	// YOU CAN MODIFY THIS CLASS; DO NOT DELETE ANY GIVEN CODE.
	public static class SafeWindow {
		private Queue<Tray> window = new LinkedList<Tray>();
		private int maxSize;
		private int cooked;
		private int served;
		public Condition canCook;
		public Condition canServe;
		public Lock lock;

		public SafeWindow(int maxSize) {
			this.maxSize = maxSize;
			this.cooked = 0;
			this.served = 0;
			lock = new ReentrantLock();
			canCook = lock.newCondition();
			canServe = lock.newCondition();
		}

		// FILL IN THE METHOD.
		public boolean add(Tray d) {
			lock.lock();
			while (size() >= maxSize) {
				try {
					canCook.await();
				} catch (Exception e) {

				}
			}
			window.add(d);
			cooked += d.numBurgers;
			// cooked += 1;
			canCook.signalAll();
			canServe.signalAll();
			lock.unlock();
			return true;
		}

		// FILL IN THE METHOD.
		public Tray remove() {
			lock.lock();
			while (size() == 0) {
				try {
					canServe.await();
				} catch (Exception e) {
				}
			}
			Tray d = window.poll();
			if (d != null) {
				// served += 1;
				served += d.numBurgers;
			}
			if (size() != 0) {
				canServe.signal();
			} else {

				canCook.signalAll();
			}
			lock.unlock();
			return d;
		}

		public int getCooked() {
			return cooked;
		}

		public int getServed() {
			return served;
		}

		public int size() {
			return window.size();
		}
	}

	// DO NOT MODIFY THIS CLASS.
	public static class Cook extends Thread {
		private String cookName;
		private SafeWindow window;
		private int maxRun;

		public Cook(String cookName, SafeWindow window, int maxRun) {
			this.cookName = cookName;
			this.window = window;
			this.maxRun = maxRun;
		}

		private Tray cook() {
			int numBurgers = (int) (Math.random() * 5) + 1;
			System.out.println(">>> Cook: " + cookName + " Value: " + numBurgers + " Size: " + window.size());
			return new Tray(cookName, numBurgers);
		}

		public void run() {
			int i = 0;
			Tray d = null;
			while (i < maxRun) {
				d = cook();
				window.add(d);
				i++;
				try {
					Thread.sleep((long) (100 * Math.random()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	// DO NOT MODIFY THIS CLASS.
	public static class Server extends Thread {
		private String serverName;
		private SafeWindow window;
		private boolean canContinue;

		public Server(String serverName, SafeWindow window) {
			this.serverName = serverName;
			this.window = window;
			this.canContinue = true;
		}

		public synchronized boolean canContinue() {
			return this.canContinue;
		}

		public synchronized void stopRun() {
			this.canContinue = false;
		}

		private Tray serve() {
			Tray d = window.remove();
			if (d == null) {
				return null;
			}
			System.out.println("<<< Cook: " + d.cook + " Server: " + serverName + " Value: " + d.numBurgers + " Size: "
					+ window.size());
			return d;
		}

		public void run() {
			while (canContinue()) {
				serve();
				try {
					Thread.sleep((long) (100 * Math.random()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		// YOU CAN MODIFY THE CONSTATNS BELOW FOR TESTING.
		int NUM_COOKS = 10;
		int COOK_MAXRUN = 100;
		int NUM_SERVERS = 5;
		int WINDOW_SIZE = 3;

		// DO NOT MODIFY ANYTHING BELOW THIS LINE.
		SafeWindow window = new SafeWindow(WINDOW_SIZE);
		Cook[] cooks = new Cook[NUM_COOKS];
		Server[] servers = new Server[NUM_SERVERS];
		for (int i = 0; i < NUM_COOKS; i++) {
			cooks[i] = new Cook("p" + i, window, COOK_MAXRUN);
			cooks[i].start();
		}

		for (int i = 0; i < NUM_SERVERS; i++) {
			servers[i] = new Server("c" + i, window);
			servers[i].start();
		}

		for (int i = 0; i < NUM_COOKS; i++) {
			cooks[i].join();
		}

		for (int i = 0; i < NUM_SERVERS; i++) {
			servers[i].stopRun();
		}

		System.out.println(window.getCooked() + " " + window.getServed());
		System.exit(0);
	}
}
