package com.typeorigin.javaperformance.chapter6.lockrace3;

/**
 * @author: chugang.cg
 * @date: 2016/2/23.
 */
public class BailoutFuture {
    private double iterationPerSecond;
    private long recordsAdded;
    private long recordsRemoved;
    private long nullCounter;

    public BailoutFuture(double iterationPerSecond, long recordsAdded, long recordsRemoved, long nullCounter) {
        this.iterationPerSecond = iterationPerSecond;
        this.recordsAdded = recordsAdded;
        this.recordsRemoved = recordsRemoved;
        this.nullCounter = nullCounter;
    }

    public double getIterationPerSecond() {
        return iterationPerSecond;
    }

    public void setIterationPerSecond(double iterationPerSecond) {
        this.iterationPerSecond = iterationPerSecond;
    }

    public long getRecordsAdded() {
        return recordsAdded;
    }

    public void setRecordsAdded(long recordsAdded) {
        this.recordsAdded = recordsAdded;
    }

    public long getRecordsRemoved() {
        return recordsRemoved;
    }

    public void setRecordsRemoved(long recordsRemoved) {
        this.recordsRemoved = recordsRemoved;
    }

    public long getNullCounter() {
        return nullCounter;
    }

    public void setNullCounter(long nullCounter) {
        this.nullCounter = nullCounter;
    }
}
