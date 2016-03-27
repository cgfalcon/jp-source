package com.typeorigin.javaperformance.chapter6.lockrace3;

/**
 * @author: chugang.cg
 * @date: 2016/2/23.
 */
public interface TaxPayerBailoutDB {

    static final int NUMBER_OF_RECORDS_DESIRED = 2 * 1000000;

    TaxPayerRecord get(String id);

    TaxPayerRecord add(String id, TaxPayerRecord record);

    TaxPayerRecord remove(String id);

    int size();
}
