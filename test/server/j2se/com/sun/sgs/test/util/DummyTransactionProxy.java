/*
 * Copyright 2007 Sun Microsystems, Inc. All rights reserved
 */

package com.sun.sgs.test.util;

import com.sun.sgs.app.TransactionNotActiveException;
import com.sun.sgs.app.TransactionTimeoutException;
import com.sun.sgs.kernel.TaskOwner;
import com.sun.sgs.service.Service;
import com.sun.sgs.service.Transaction;
import com.sun.sgs.service.TransactionProxy;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;

/** Provides a simple implementation of TransactionProxy, for testing. */
public class DummyTransactionProxy implements TransactionProxy {

    /** Stores information about the transaction for the current thread. */
    private final ThreadLocal<DummyTransaction> threadTxn =
	new ThreadLocal<DummyTransaction>();

    /** The task owner. */
    private final TaskOwner taskOwner = new DummyTaskOwner();

    /** Mapping from type to service. */
    private final Map<Class<? extends Service>, Service> services =
	new HashMap<Class<? extends Service>, Service>();

    /** Creates an instance of this class. */
    public DummyTransactionProxy() { }

    /* -- Implement TransactionProxy -- */

    /**
     * Note that this implementation also aborts the transaction if it has
     * timed out.  This behavior roughly simulates the way that tasks
     * automatically abort timed out transactions, even though it is not the
     * transaction proxy that is responsible for this behavior in the product.
     * -tjb@sun.com (06/14/2007)
     */
    public Transaction getCurrentTransaction() {
	Transaction txn = threadTxn.get();
	if (txn == null) {
	    throw new TransactionNotActiveException(
		"No transaction is active");
	}
	try {
	    txn.checkTimeout();
	} catch (TransactionTimeoutException e) {
	    txn.abort(e);
	    throw e;
	}
	return txn;
    }

    public TaskOwner getCurrentOwner() {
	return taskOwner;
    }

    public <T extends Service> T getService(Class<T> type) {
	Object service = services.get(type);
	if (service == null) {
	    throw new MissingResourceException(
		"Service of type " + type + " was not found",
		type.getName(), "Service");
	}
	return type.cast(service);
    }

    /* -- Other public methods -- */

    /**
     * Specifies the transaction object that will be returned for the current
     * transaction, or null to specify that no transaction should be
     * associated.  Also stores itself in the transaction instance, so that the
     * transaction can clear the current transaction on prepare, commit, or
     * abort.
     */
    public void setCurrentTransaction(DummyTransaction txn) {
	threadTxn.set(txn);
	if (txn != null) {
	    txn.proxy = this;
	}
    }

    /**
     * Specifies the service that should be returned for an exact match for
     * the specified type.
     */
    public <T extends Service> void setComponent(Class<T> type, T service) {
	if (type == null || service == null) {
	    throw new NullPointerException("Arguments must not be null");
	}
	services.put(type, service);
    }
}
