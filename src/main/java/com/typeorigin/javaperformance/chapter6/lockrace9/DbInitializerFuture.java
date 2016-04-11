package com.typeorigin.javaperformance.chapter6.lockrace9;

/**
 * 
 * @author Sam Zhang
 *
 */
public class DbInitializerFuture {
	private int recordsCreated;

	public DbInitializerFuture() {
	}

	public int getRecordsCreated() {
		return recordsCreated;
	}

	public void addToRecordsCreated(int value) {
		this.recordsCreated += value;
	}

}
