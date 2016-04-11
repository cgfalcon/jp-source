package com.typeorigin.javaperformance.chapter6.lockrace9;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: chugang.cg,Sam Zhang
 * @date: 2016/2/23.
 */
public class BailoutMain {

	public static final int TEST_TIME = 240 * 1000;
	public static final ThreadLocal<Random> threadLocalRandom = new ThreadLocal<Random>() {
		@Override
		protected Random initialValue() {
			return new Random(Thread.currentThread().getId());
		}
	};
	private static char[] alphabet = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
			'v', 'w', 'x', 'y', 'z' };

	private static String[] states = { "Alabama", "Alaska", "Arizona",
			"Arkansas", "California", "Colorado", "Connecticut", "Delaware",
			"Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana",
			"Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland",
			"Massachusetts", "Michigan", "Minnesota", "Mississippi",
			"Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
			"New Jersey", "New Mexico", "New York", "North Carolina",
			"North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania",
			"Rhode Island", "South Carolina", "South Dakota", "Tennessee",
			"Texas", "Utah", "Vermont", "Virginia", "Washington",
			"West Virginia", "Wisconsin", "Wyoming" };

	public static void main(String[] args) {
		final long start = System.nanoTime();
		final int numberOfThreads = Runtime.getRuntime().availableProcessors();
		final int dbSize = TaxPayerBailoutDB.NUMBER_OF_RECORDS_DESIRED;
		final int taxPayerListSize = dbSize / numberOfThreads;

		System.out.println("Number of threads to run concurrently : "
				+ numberOfThreads);
		System.out.println("Tax payer database size: " + dbSize);

		// put db
		System.out.println("Creating tax payer database ...");
		TaxPayerBailoutDB db = new TaxPayerBailoutDbImpl(dbSize);
		List<String>[] taxPayerList = new List[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			taxPayerList[i] = Collections
					.synchronizedList(new ArrayList<String>(taxPayerListSize));
		}

		System.out.println("Allocating thread pool and (" + numberOfThreads
				+ ") db initializer threads ...");

		ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);
		Callable<DbInitializerFuture>[] dbCallables = new DbInitializer[numberOfThreads];
		for (int i = 0; i < dbCallables.length; i++) {
			dbCallables[i] = new DbInitializer(db, taxPayerList, dbSize
					/ numberOfThreads);
		}

		System.out.println("\tThread pool & db threads allocated.");
		System.out.println("Starting (" + dbCallables.length
				+ ") db initializer threads ...");
		Set<Future<DbInitializerFuture>> dbSet = new HashSet<Future<DbInitializerFuture>>();
		for (int i = 0; i < dbCallables.length; i++) {
			Callable<DbInitializerFuture> callable = dbCallables[i];
			Future<DbInitializerFuture> future = pool.submit(callable);
			dbSet.add(future);
		}

		int recordsCreated = 0;
		for (Future<DbInitializerFuture> future : dbSet) {
			DbInitializerFuture result = null;
			try {
				result = future.get();
			} catch (InterruptedException ex) {
				Logger.getLogger(BailoutMain.class.getName()).log(Level.SEVERE,
						null, ex);
			} catch (ExecutionException ex) {
				Logger.getLogger(BailoutMain.class.getName()).log(Level.SEVERE,
						null, ex);
			}
			recordsCreated += result.getRecordsCreated();
		}
		final long initDbTime = System.nanoTime() - start;
		System.out.println("\tDb initializer threads completed.");
		System.out.println("\tTax payer db created & populated in ("
				+ initDbTime / (1000 * 1000) + ") ms.");
		System.out.println("\tCreated (" + recordsCreated + ") records ...");

		System.out.println("Allocating threads, main processing ...");
		Callable<BailoutFuture>[] callables = new TaxCallable[numberOfThreads];
		for (int i = 0; i < callables.length; i++) {
			callables[i] = new TaxCallable(taxPayerList[i], db);
		}

		System.out.println("\tthreads allocated.");

		System.out.println("Starting (" + callables.length + ") threads ...");
		Set<Future<BailoutFuture>> set = new HashSet<Future<BailoutFuture>>();
		for (int i = 0; i < callables.length; i++) {
			final Callable<BailoutFuture> callable = callables[i];
			final Future<BailoutFuture> future = pool.submit(callable);
			set.add(future);
		}

		System.out.println("\t(" + callables.length + ") threads started.");

		// blocked, until callables finish
		System.out.println("Waiting for " + TEST_TIME / 1000 + " seconds for ("
				+ callables.length + ") threads to " + "complete ...");

		double iterationsPerSecond = 0;
		long recordsAdded = 0;
		long recordsRemoved = 0;
		long nullCounter = 0;
		int counter = 1;
		for (Future<BailoutFuture> future : set) {
			BailoutFuture result = null;
			try {
				result = future.get();
			} catch (InterruptedException e) {
				Logger.getLogger(BailoutMain.class.getName()).log(Level.SEVERE,
						null, e);
			} catch (ExecutionException e) {
				Logger.getLogger(BailoutMain.class.getName()).log(Level.SEVERE,
						null, e);
			}

			System.out.println("Iterations per second on thread[" + counter++
					+ "] -> " + result.getIterationPerSecond());
			iterationsPerSecond += result.getIterationPerSecond();
			recordsAdded += result.getRecordsAdded();
			recordsRemoved += result.getRecordsRemoved();
			nullCounter = result.getNullCounter();
		}

		DecimalFormat df = new DecimalFormat("#.##");
		System.out.println("Total iterations per second -> "
				+ df.format(iterationsPerSecond));

		NumberFormat nf = NumberFormat.getInstance();
		System.out.println("Total records added -------> "
				+ nf.format(recordsAdded));
		System.out.println("Total records removed -----> "
				+ nf.format(recordsRemoved));
		System.out.println("Total records in db -------> "
				+ nf.format(db.size()));
		System.out.println("Total null records encountered: "
				+ nf.format(nullCounter));

		System.exit(0);
	}

	public static TaxPayerRecord makeTaxPayerRecord() {
		String firstName = getRandomName();
		String lastName = getRandomName();
		String ssn = getRandomSSN();
		String address = getRandomAddress();
		String city = getRandomCity();
		String state = getRandomState();
		return new TaxPayerRecord(firstName, lastName, ssn, address, city,
				state);
	}

	public static DbInitializerFuture populateDatabase(TaxPayerBailoutDB db,
			List<String>[] taxPayerIdList, int dbSize) {
		for (int i = 0; i < dbSize; i++) {
			String key = getRandomTaxPayerId();
			TaxPayerRecord tpr = makeTaxPayerRecord();
			db.add(key, tpr);
			int index = i % taxPayerIdList.length;
			taxPayerIdList[index].add(key);
		}
		DbInitializerFuture future = new DbInitializerFuture();
		future.addToRecordsCreated(dbSize);
		return future;
	}

	public static String getRandomTaxPayerId() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 20; i++) {
			int index = threadLocalRandom.get().nextInt(alphabet.length);
			sb.append(alphabet[index]);
		}

		return sb.toString();
	}

	public static String getRandomName() {
		StringBuilder sb = new StringBuilder();
		int size = threadLocalRandom.get().nextInt(8) + 5;
		for (int i = 0; i < size; i++) {
			int index = threadLocalRandom.get().nextInt(alphabet.length);
			char c = alphabet[index];
			if (i == 0) {
				c = Character.toUpperCase(c);
			}
			sb.append(c);
		}

		return sb.toString();
	}

	public static String getRandomSSN() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 11; i++) {
			if (i == 3 || i == 6) {
				sb.append("-");
			}
			int x = threadLocalRandom.get().nextInt(9);
			sb.append(x);
		}

		return sb.toString();
	}

	public static String getRandomAddress() {
		StringBuilder sb = new StringBuilder();
		int size = threadLocalRandom.get().nextInt(14) + 10;
		for (int i = 0; i < size; i++) {
			if (i < 5) {
				int x = threadLocalRandom.get().nextInt(8);
				sb.append(x + 1);
			}
			int index = threadLocalRandom.get().nextInt(alphabet.length);
			char c = alphabet[index];
			if (i == 0) {
				c = Character.toUpperCase(c);
			}
			sb.append(c);
		}

		return sb.toString();
	}

	public static String getRandomCity() {
		StringBuilder sb = new StringBuilder();
		int size = threadLocalRandom.get().nextInt(5) + 6;
		for (int i = 0; i < size; i++) {
			int index = threadLocalRandom.get().nextInt(alphabet.length);
			char c = alphabet[index];
			if (i == 0) {
				c = Character.toUpperCase(c);
			}
			sb.append(c);
		}

		return sb.toString();
	}

	public static String getRandomState() {
		int index = threadLocalRandom.get().nextInt(states.length);
		return states[index];
	}

}
