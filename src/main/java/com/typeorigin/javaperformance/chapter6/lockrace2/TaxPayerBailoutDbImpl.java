package com.typeorigin.javaperformance.chapter6.lockrace2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Sam Zhang
 * @date: 2016/3/25 同步HashMap替换成用ConcurrentHashMap
 */
public class TaxPayerBailoutDbImpl implements TaxPayerBailoutDB {
	private final Map<String, TaxPayerRecord> db;

	public TaxPayerBailoutDbImpl(int size) {
		db = new ConcurrentHashMap<String, TaxPayerRecord>(size);
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
