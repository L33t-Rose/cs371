import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

public class CPUScheduler {
	// DO NOT MODIFY THE CLASS Job
	public static class Job {
		int jobId;
		int arrivalTime;
		int burst;
		int remainingBurst;
		int startTime;
		int endTime;

		public Job(int jobId, int arrivalTime, int burst) {
			this.jobId = jobId;
			this.arrivalTime = arrivalTime;
			this.burst = burst;
			this.remainingBurst = burst;
			this.startTime = -1;
			this.endTime = -1;
		}

		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("[");
			result.append(jobId);
			result.append("] ");
			result.append("Burst - ");
			result.append(burst);
			result.append(" Arrival - ");
			result.append(arrivalTime);
			result.append(" Start - ");
			result.append(startTime);
			result.append(" Remaining Burst - ");
			result.append(remainingBurst);
			return result.toString();
		}

		public boolean equals(Object o) {
			return ((Job) o).jobId == this.jobId;
		}
	}

	// DO NOT MODIFY THE CLASS ReadyQueue
	public static class ReadyQueue {
		private LinkedList<Job> readyQueue;

		public ReadyQueue() {
			readyQueue = new LinkedList<Job>();
		}

		public void enqueue(Job j) {
			readyQueue.add(j);
		}

		public Job dequeue() {
			if (readyQueue.isEmpty()) {
				return null;
			} else {
				return readyQueue.poll();
			}
		}

		public LinkedList<Job> queue() {
			return readyQueue;
		}
	}

	// DO NOT MODIFY THE CLASS CPU
	public static class CPU {
		private ReadyQueue readyQueue;
		private Job running;

		private int lastJobCreated;
		private Random rand;

		public CPU(ReadyQueue readyQueue) {
			this.readyQueue = readyQueue;
			running = null;
			lastJobCreated = 0;
			rand = new Random();
		}

		public void newJob(int sec) {
			// Ensure 20% chance a new job is created.
			if (rand.nextInt(100) < 20) {
				lastJobCreated++;
				Job j = new Job(lastJobCreated, sec, rand.nextInt(10) + 1);
				System.out.println(sec + " === " + j);
				readyQueue.enqueue(j);
			}
		}

		public Job getRunning() {
			return running;
		}

		public void setRunning(int sec, Job j) {
			System.out.println(sec + " <<< " + running);
			running = j;
			if (j == null) {
				System.out.println(sec + " >>> No Job Scheduled");
			} else {
				System.out.println(sec + " >>> " + j);
			}
		}

	}

	// DO NOT MODIFY THE INTERFACE Schedule
	public static interface Scheduler {
		public void schedule(int currSec);
	}

	// WRITE CODE HERE; UPDATE THE CLASS FIFOScheduler
	public static class FIFOScheduler implements Scheduler {
		CPU c;
		ReadyQueue r;

		public FIFOScheduler(CPU c, ReadyQueue r) {
			this.c = c;
			this.r = r;
		}

		// WRITE CODE HERE
		public void schedule(int currSec) {
			Job currJob = c.getRunning();
			if (currJob == null) {
				currJob = r.dequeue();
				if (currJob == null) {
					return;
				}
				currJob.startTime = currSec;
				c.setRunning(currSec, currJob);
			} else {
				currJob.remainingBurst--;

				if (currJob.remainingBurst == 0) {
					currJob.endTime = currSec;
					Job newJob = r.dequeue();
					if (newJob != null) {
						newJob.startTime = currSec;
					}
					c.setRunning(currSec, newJob);
				}
			}
		}
	}

	// WRITE CODE HERE; UPDATE THE CLASS SRTFScheduler
	public static class SRTFScheduler implements Scheduler {
		CPU c;
		ReadyQueue r;
		PriorityQueue<Job> pq = new PriorityQueue<Job>(new JobComparator());

		// PriorityQueue something = new PriorityQueue<>();
		public SRTFScheduler(CPU c, ReadyQueue r) {
			this.c = c;
			this.r = r;
		}

		class JobComparator implements Comparator<Job> {
			@Override
			public int compare(CPUScheduler.Job o1, CPUScheduler.Job o2) {
				return o1.remainingBurst - o2.remainingBurst;
			}
		}

		// WRITE CODE HERE
		public void schedule(int currSec) {
			Job currJob = c.getRunning();
			if (currJob == null) {
				currJob = r.dequeue();
				if (currJob == null) {
					return;
				}
				currJob.startTime = currSec;
				c.setRunning(currSec, currJob);
				return;
			}
			currJob.remainingBurst--;

			// I've decided to transfer all of the jobs in rq into pq so that I don't have
			// to recreate
			// the pq everytime. This effectively makes our pq the real ready queue for our
			// jobs
			while (!r.queue().isEmpty()) {
				pq.add(r.dequeue());
			}

			// Grab the smallest job in the pq
			Job possibleSmallest = pq.poll();
			// If there's nothing there then we can just let the current job continue
			if (possibleSmallest == null) {
				// Edge case: What if the current job is finished?
				if (currJob.remainingBurst == 0) {
					c.setRunning(currSec, null);
				}
				return;
			}
			// If the current job is finished then by default the smallestPossible should be
			// next
			else if (currJob.remainingBurst == 0) {
				// Since we put old jobs back into pq we don't want to modify their start times.
				if (possibleSmallest.startTime == -1) {
					possibleSmallest.startTime = currSec;
				}
				c.setRunning(currSec, possibleSmallest);
			}
			// Compare it to the current job to see who's smallest
			// Only switch if possibleSmallest's remBurst is smaller otherwise just move on
			else if (possibleSmallest.remainingBurst < currJob.remainingBurst) {
				// If the current job isn't finished we'll put it back into the pq.
				if (currJob.remainingBurst > 0) {
					pq.add(currJob);
				}
				// Since we put old jobs back into pq we don't want to modify their start times.
				if (possibleSmallest.startTime == -1) {
					possibleSmallest.startTime = currSec;
				}
				c.setRunning(currSec, possibleSmallest);
			}
			// If it's not smaller we need to add that job back into the pq
			else {
				pq.add(possibleSmallest);
			}
		}
	}

	// DO NOT MODIFY THE METHOD simulate
	public static void simulate(int length, CPU c, Scheduler s) {
		for (int sec = 0; sec < length; sec++) {
			c.newJob(sec);
			s.schedule(sec);
		}
	}

	// TRY OTHER TEST CASES BY UPDATING THE METHOD main
	public static void main(String[] args) {
		int MAX_LENGTH = 100;

		System.out.println("==== FIFO ====");
		ReadyQueue r = new ReadyQueue();
		CPU c = new CPU(r);
		Scheduler f = new FIFOScheduler(c, r);
		simulate(MAX_LENGTH, c, f);

		System.out.println("==== SRTF ====");
		r = new ReadyQueue();
		c = new CPU(r);
		Scheduler s = new SRTFScheduler(c, r);
		simulate(MAX_LENGTH, c, s);
	}
}
