package com.typeorigin.javaperformance.chapter6.lockrace1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: chugang.cg
 * @date: 2016/2/23.
 */
public class TaxPayerBailoutDbImpl implements TaxPayerBailoutDB{
    private final Map<String, TaxPayerRecord> db;

    public TaxPayerBailoutDbImpl(int size) {
        db = Collections.synchronizedMap(new HashMap<String, TaxPayerRecord>(size));
    }

    public TaxPayerRecord get(String id) {
        return db.get(id);
    }

    public TaxPayerRecord add(String id, TaxPayerRecord record) {
        TaxPayerRecord old = db.put(id, record);
        if (old != null) {
            old = db.put(id, old);
        }
        return old;
    }

    public TaxPayerRecord remove(String id) {
        return db.remove(id);
    }

    public int size() {
        return db.size();
    }
}
