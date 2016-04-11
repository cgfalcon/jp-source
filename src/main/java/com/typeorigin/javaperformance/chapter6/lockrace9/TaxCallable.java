package com.typeorigin.javaperformance.chapter6.lockrace9;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * @author: chugang.cg,Sam Zhang
 * @date: 2016/2/23.
 */
public class TaxCallable implements Callable<BailoutFuture> {

	private static long runTimeInMillis = BailoutMain.TEST_TIME;
	final private static ThreadLocal<Random> generator = BailoutMain.threadLocalRandom;
	private long nullCounter;
	private long recordsRemoved;
	private long newRecordsAdded;
	private int index;
	private String taxPayerId;
	final private List<String> taxPayerList;
	final private TaxPayerBailoutDB db;

	public TaxCallable(List<String> taxPayerList, TaxPayerBailoutDB db) {
		this.db = db;
		this.taxPayerList = taxPayerList;
		index = 0;
	}

	public BailoutFuture call() throws Exception {
		long iterations = 0L;
		long elapsedTime = 0L;
		long startTime = System.currentTimeMillis();
		double iterationsPerSecond = 0;
		do {
			setTaxPayer();
			iterations++;
			TaxPayerRecord tpr = null;

			// if iterations overflow
			if (iterations == Long.MAX_VALUE) {
				long elapsed = System.currentTimeMillis() - startTime;
				iterationsPerSecond = iterations / ((double) (elapsed / 1000));
				System.err.println("Iteration counter about to overflow ...");
				System.err
						.println("Calculating current operations per second ...");
				System.err.println("Iterations per second: "
						+ iterationsPerSecond);
				iterations = 0;
				startTime = System.currentTimeMillis();
				runTimeInMillis -= elapsed;
			}

			if (iterations % 1001 == 0) {
				tpr = addNewTaxPayer(tpr);
			} else if (iterations % 60195 == 0) {
				tpr = removeTaxPayer(tpr);
			} else {
				tpr = updateTaxPayer(iterations, tpr);
			}

			if (iterations % 1000 == 0) {
				elapsedTime = System.currentTimeMillis() - startTime;
			}
		} while (elapsedTime < runTimeInMillis);

		if (iterations > 1000) {
			iterationsPerSecond = iterations / ((double) (elapsedTime / 1000));
		}

		BailoutFuture bailoutFuture = new BailoutFuture(iterationsPerSecond,
				newRecordsAdded, recordsRemoved, nullCounter);

		return bailoutFuture;
	}

	private void setTaxPayer() {
		if (++index >= taxPayerList.size()) {
			index = 0;
		}
		this.taxPayerId = taxPayerList.get(index);
	}

	private TaxPayerRecord addNewTaxPayer(TaxPayerRecord tpr) {
		// add new taxpayer to db
		String tmpTaxPayerId = BailoutMain.getRandomTaxPayerId();
		tpr = BailoutMain.makeTaxPayerRecord();
		TaxPayerRecord old = db.add(tmpTaxPayerId, tpr);
		if (old == null) {
			// add to local
			taxPayerList.add(tmpTaxPayerId);
			newRecordsAdded++;
		}
		return tpr;
	}

	private TaxPayerRecord removeTaxPayer(TaxPayerRecord tpr) {
		// remove tax payer from db
		tpr = db.remove(taxPayerId);
		if (tpr != null) {
			taxPayerList.remove(index);
			recordsRemoved++;
		}

		return tpr;
	}

	private TaxPayerRecord updateTaxPayer(long iterations, TaxPayerRecord tpr) {
		if (iterations % 1001 == 0) {
			tpr = db.get(taxPayerId);
		} else {
			// update TaxPayer record
			tpr = db.get(taxPayerId);
			if (tpr != null) {
				long tax = generator.get().nextInt(10) + 15;
				tpr.taxPaid(tax);
			}
		}

		if (tpr == null) {
			nullCounter++;
		}
		return tpr;
	}

}
