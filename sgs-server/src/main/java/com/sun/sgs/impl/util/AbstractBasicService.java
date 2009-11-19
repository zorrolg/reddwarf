/*
 * Copyright 2007-2009 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.impl.util;

import com.sun.sgs.app.ExceptionRetryStatus;
import com.sun.sgs.auth.Identity;
import com.sun.sgs.impl.sharedutil.LoggerWrapper;
import com.sun.sgs.impl.sharedutil.PropertiesWrapper;
import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.kernel.TaskQueue;
import com.sun.sgs.kernel.TaskScheduler;
import com.sun.sgs.kernel.TransactionScheduler;
import com.sun.sgs.service.Node;
import com.sun.sgs.service.Service;
import com.sun.sgs.service.TransactionProxy;
import com.sun.sgs.service.WatchdogService;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * An abstract implementation of a service.  It manages state
 * transitions (i.e., initialized, ready, shutting down, shutdown), in
 * progress call tracking for services with embedded remote servers,
 * and shutdown support.
 *
 * <p>The {@link #getName getName} method invokes the instance's {@code
 * toString} method, so a concrete subclass of {@code AbstractService}
 * should provide an implementation of the {@code toString} method.
 * 
 * An {@link #AbstractBasicService} supports the following properties: <p>
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>
 *	com.sun.sgs.impl.util.io.task.max.retries
 *	</b></code><br>
 *	<i>Default:</i> 5 retries <br>
 *
 * <dd style="padding-top: .5em">
 *	Specifies how many times an {@link IoRunnable IoRunnable} task should 
 *      be retried before performing failure procedures. The value
 *	must be greater than or equal to {@code 0}.<p>
 *
 * <dt> <i>Property:</i> <code><b>
 *	com.sun.sgs.impl.util.io.task.wait.time
 *	</b></code><br>
 *	<i>Default:</i> 100 milliseconds <br>
 *
 * <dd style="padding-top: .5em">
 *      Specifies the wait time between {@link IoRunnable IoRunnable} task
 *      retries. The value must be greater than or equal to {@code 0}.
 *
 * </dl>
 */
public abstract class AbstractBasicService implements Service {

    /** Service state. */
    protected static enum State {
        /** The service is initialized. */
	INITIALIZED,
        /** The service is ready. */
        READY,
        /** The service is shutting down. */
        SHUTTING_DOWN,
        /** The service is shut down. */
        SHUTDOWN
    }

    /** The transaction proxy, or null if configure has not been called. */    
    protected static volatile TransactionProxy txnProxy = null;

    /** The logger for the subclass. */
    protected final LoggerWrapper logger;

    /** The task scheduler. */
    protected final TaskScheduler taskScheduler;

    /** The transaction scheduler. */
    protected final TransactionScheduler transactionScheduler;

    /** The task owner. */
    protected final Identity taskOwner;

    /** The lock for {@code state} and {@code callsInProgress} fields. */
    private final Object lock = new Object();
    
    /** The server state. */
    private State state;
    
    /** The count of calls in progress. */
    private int callsInProgress = 0;

    /** Thread for shutting down the server. */
    private volatile Thread shutdownThread;

    /** Prefix for io task related properties. */
    public static final String IO_TASK_PROPERTY_PREFIX =
            "com.sun.sgs.impl.util.io.task";

    /**
     * An optional property that specifies the maximum number of retries for
     * IO tasks in services.
     */
    public static final String IO_TASK_RETRIES_PROPERTY = 
            IO_TASK_PROPERTY_PREFIX + ".max.retries";

    /**
     * An optional property that specifies the wait time between successive
     * IO task retries.
     */
    public static final String IO_TASK_WAIT_TIME_PROPERTY = 
            IO_TASK_PROPERTY_PREFIX + ".wait.time";

    /** The default number of IO task retries **/
    private static final int DEFAULT_MAX_IO_ATTEMPTS = 5;
    
    /** The default time interval to wait between IO task retries **/
    private static final int DEFAULT_RETRY_WAIT_TIME = 100;
    
    /** The time (in milliseconds) to wait between retries for IO 
     * operations. */ 
    protected final int retryWaitTime;
    
    /** The maximum number of retry attempts for IO operations. */
    protected final int maxIoAttempts;

    /**
     * Constructs an instance with the specified {@code properties}, {@code
     * systemRegistry}, {@code txnProxy}, and {@code logger}.  It sets this
     * service's state to {@code INITIALIZED}.
     *
     * @param	properties service properties
     * @param	systemRegistry system registry
     * @param	txnProxy transaction proxy
     * @param	logger the service's logger
     */
    protected AbstractBasicService(Properties properties,
				   ComponentRegistry systemRegistry,
				   TransactionProxy txnProxy,
				   LoggerWrapper logger)
    {
	if (properties == null) {
	    throw new NullPointerException("null properties");
	} else if (systemRegistry == null) {
	    throw new NullPointerException("null systemRegistry");
	} else if (txnProxy == null) {
	    throw new NullPointerException("null txnProxy");
	} else if (logger == null) {
	    throw new NullPointerException("null logger");
	}
	
	synchronized (AbstractService.class) {
	    if (AbstractService.txnProxy == null) {
		AbstractService.txnProxy = txnProxy;
	    } else {
		assert AbstractService.txnProxy == txnProxy;
	    }
	}

        PropertiesWrapper wrappedProps = new PropertiesWrapper(properties);
        retryWaitTime = wrappedProps.getIntProperty(
                IO_TASK_WAIT_TIME_PROPERTY, DEFAULT_RETRY_WAIT_TIME, 0,
                Integer.MAX_VALUE);
        maxIoAttempts = wrappedProps.getIntProperty(
                IO_TASK_RETRIES_PROPERTY, DEFAULT_MAX_IO_ATTEMPTS, 0,
                Integer.MAX_VALUE);
	
	this.logger = logger;
	this.taskScheduler = systemRegistry.getComponent(TaskScheduler.class);
	this.transactionScheduler =
	    systemRegistry.getComponent(TransactionScheduler.class);
	this.taskOwner = txnProxy.getCurrentOwner();

	setState(State.INITIALIZED);
    }

    /** {@inheritDoc} */
    public String getName() {
	return toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If this service is in the {@code INITIALIZED} state, this
     * method sets the state to {@code READY} and invokes the {@link
     * #doReady doReady} method.  If this service is already in the
     * {@code READY} state, this method performs no actions.  If this
     * service is shutting down, or is already shut down, this method
     * throws {@code IllegalStateException}.
     *
     * @throws	Exception if a problem occurs
     * @throws	IllegalStateException if this service is shutting down
     *		or is already shut down
     */
    public void ready() throws Exception {
	logger.log(Level.FINEST, "ready");
	synchronized (lock) {
	    switch (state) {
		
	    case INITIALIZED:
		setState(State.READY);
		break;
		
	    case READY:
		return;
		
	    case SHUTTING_DOWN:
	    case SHUTDOWN:
		throw new IllegalStateException("service shutting down");
	    default:
		throw new AssertionError();
	    }
	}
	doReady();
    }

    /**
     * Performs ready operations.  This method is invoked by the
     * {@link #ready ready} method only once so that the subclass can
     * perform any operations necessary during the "ready" phase.
     *
     * @throws	Exception if a problem occurs
     */
    protected abstract void doReady() throws Exception;

    /**
     * {@inheritDoc}
     *
     * <p>If this service is in the {@code INITIALIZED} state or
     * {@code READY} state, this method sets the state to
     * {@code SHUTTING_DOWN}, waits for all calls in progress to
     * complete, starts a thread to invoke the {@link #doShutdown
     * doShutdown} method, waits for that thread to complete, and
     * returns. If this service is in the {@code SHUTTING_DOWN}
     * state, this method will block until the shutdown is complete. If
     * this service is in the {@code SHUTDOWN} state, then it will
     * return immediately. Any retries or interruption handling should be
     * done in the service's implementation of the
     * {@link #doShutdown() doShutdown} method.
     *
     */
    public void shutdown() {
	logger.log(Level.FINEST, "shutdown");
	
	synchronized (lock) {
	    switch (state) {
		
	    case INITIALIZED:
	    case READY:
		logger.log(Level.FINEST, "initiating shutdown");
		setState(State.SHUTTING_DOWN);
		while (callsInProgress > 0) {
		    try {
			lock.wait();
		    } catch (InterruptedException e) {
                        return;
		    }
		}
		shutdownThread = new ShutdownThread();
		shutdownThread.start();
		break;

	    case SHUTTING_DOWN:
		break;
		
	    case SHUTDOWN:
                return;

	    default:
	        throw new AssertionError();
	    }
	}

	try {
	    shutdownThread.join();
	} catch (InterruptedException e) {
            return;
	}
    }

    /**
     * Performs shutdown operations.  This method is invoked by the
     * {@link #shutdown shutdown} method only once so that the
     * subclass can perform any operations necessary to shutdown the
     * service.
     */
    protected abstract void doShutdown();

    /**
     * Returns this service's state.
     *
     * @return this service's state
     */
    protected State getState() {
	synchronized (lock) {
	    return state;
	}
    }
    
    /**
     * Increments the number of calls in progress.  This method should
     * be invoked by remote methods to both increment in progress call
     * count and to check the state of this server.  When the call has
     * completed processing, the remote method should invoke {@link
     * #callFinished callFinished} before returning.
     *
     * @throws	IllegalStateException if this service is shutting down
     */
    protected void callStarted() {
	synchronized (lock) {
	    if (shuttingDown()) {
		throw new IllegalStateException("service is shutting down");
	    }
	    callsInProgress++;
	}
    }

    /**
     * Decrements the in progress call count, and if this server is
     * shutting down and the count reaches 0, then notifies the waiting
     * shutdown thread that it is safe to continue.  A remote method
     * should invoke this method when it has completed processing.
     */
    protected void callFinished() {
	synchronized (lock) {
	    callsInProgress--;
	    if (state == State.SHUTTING_DOWN && callsInProgress == 0) {
		lock.notifyAll();
	    }
	}
    }

    /**
     * Returns {@code true} if this service is shutting down.
     *
     * @return	{@code true} if this service is shutting down
     */
    protected boolean shuttingDown() {
	synchronized (lock) {
	    return
		state == State.SHUTTING_DOWN ||
		state == State.SHUTDOWN;
	}
    }
    
    /** 
     * Returns {@code true} if this service is in the initialized state
     * but is not yet ready to run.
     * 
     * @return {@code true} if this service is in the initialized state
     */
    protected boolean isInInitializedState() {
        synchronized (lock) {
            return state == State.INITIALIZED;
        }
    }
    
    /**
     * Runs a transactional task to query the status of the node with the
     * specified {@code nodeId} and returns {@code true} if the node is alive
     * and {@code false} otherwise.
     *
     * <p>This method must be called from outside a transaction or {@code
     * IllegalStateException} will be thrown.
     *
     * @param	nodeId a node ID
     * @return	{@code true} if the node with the associated ID is
     *		considered alive, otherwise returns {@code false}
     * @throws	IllegalStateException if this method is invoked inside a
     *		transactional context 
     */
    public boolean isAlive(long nodeId) {
	checkNonTransactionalContext();
	try {
	    CheckNodeStatusTask nodeStatus =
		new CheckNodeStatusTask(nodeId);
	    transactionScheduler.runTask(nodeStatus, taskOwner);
	    return nodeStatus.isAlive;
	} catch (IllegalStateException ignore) {
	    // Ignore because the service is shutting down.
	} catch (Exception e) {
	    // This shouldn't happen, so log.
	    if (logger.isLoggable(Level.WARNING)) {
		logger.logThrow(
		    Level.WARNING, e, "running CheckNodeStatusTask throws");
	    }
	}
	// TBD: is this the correct value to return?  We can't really tell
	// what the status of a non-local node is if the local node is
	// shutting down.
	return false;
    } 

    /**
     * Creates a {@code TaskQueue} for dependent, transactional tasks.
     *
     * @return	the task queue
     */
    public TaskQueue createTaskQueue() {
	return transactionScheduler.createTaskQueue();
    }

    /**
     * Executes the specified {@code ioTask} by invoking its {@link
     * IoRunnable#run run} method. If the specified task throws an
     * {@code IOException}, this method will retry the task for a fixed 
     * number of times. The method will stop retrying if the node with
     * the given {@code nodeId} is no longer alive. The number of retries
     * and the wait time between retries are configurable properties.
     *
     * <p>
     * This method must be called from outside a transaction or {@code
     * IllegalStateException} will be thrown.
     *
     * @param ioTask a task with IO-related operations
     * @param nodeId the node that is the target of the IO operations
     * @throws IllegalStateException if this method is invoked within a
     * transactional context
     */
    public void runIoTask(IoRunnable ioTask, long nodeId) {
        int maxAttempts = maxIoAttempts;
        checkNonTransactionalContext();
        do {
            try {
                ioTask.run();
                return;
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.logThrow(Level.FINEST, e,
                            "IoRunnable {0} throws", ioTask);
                }
                if (maxAttempts-- == 0) {
                    logger.logThrow(Level.WARNING, e,
                            "A communication error occured while running an" +
                            "IO task. Reporting node {0} as failed.", nodeId);

                    // Report failure of remote node since are
                    // having trouble contacting it
                    txnProxy.getService(WatchdogService.class).
                            reportFailure(nodeId, this.getClass().toString());
                    
                    break;
                }
                try {
                    // TBD: what back-off policy do we want here?
                    Thread.sleep(retryWaitTime);
                } catch (InterruptedException ie) {
                }
            }
        } while (isAlive(nodeId));
    }
    
    /**
     * Returns {@code true} if the specified exception is retryable, and
     * {@code false} otherwise.  A retryable exception is one that
     * implements {@link ExceptionRetryStatus} and invoking its {@link
     * ExceptionRetryStatus#shouldRetry shouldRetry} method returns {@code
     * true}.
     *
     * @param	e an exception
     * @return	{@code true} if the specified exception is retryable, and
     *		{@code false} otherwise
     */
    public static boolean isRetryableException(Throwable e) {
	return (e instanceof ExceptionRetryStatus) &&
	    ((ExceptionRetryStatus) e).shouldRetry();
    }

    /**
     * Checks that the current thread is not in a transactional context
     * and throws {@code IllegalStateException} if the thread is in a
     * transactional context.
     *
     * @param	txnProxy the transaction proxy
     */
    public static void checkNonTransactionalContext(
	TransactionProxy txnProxy)
    {
	if (txnProxy.inTransaction()) {
	    throw new IllegalStateException(
		"operation not allowed from a transactional context");
	}
    }
	
    /* -- Private methods and classes -- */
    
    /**
     * Sets this service's state to {@code newState}.
     *
     * @param	newState a new state.
     */
    private void setState(State newState) {
	synchronized (lock) {
	    state = newState;
	}
    }

    /**
     * Checks that the current thread is not in a transactional context
     * and throws {@code IllegalStateException} if the thread is in a
     * transactional context.
     */
    protected void checkNonTransactionalContext() {
	checkNonTransactionalContext(txnProxy);
    }

    /**
     * A task to obtain the status of a given node.
     */
    private static class CheckNodeStatusTask extends AbstractKernelRunnable {
	private final long nodeId;
	volatile boolean isAlive = false;

	/** Constructs an instance with the specified {@code nodeId}. */
	CheckNodeStatusTask(long nodeId) {
	    super(null);
	    this.nodeId = nodeId;
	}

	/** {@inheritDoc} */
	public void run() {
	    WatchdogService watchdogService =
		txnProxy.getService(WatchdogService.class);
	    Node node = watchdogService.getNode(nodeId);
	    isAlive = node != null && node.isAlive();
	}
    }
    
    /**
     * Thread for shutting down service/server.
     */
    private final class ShutdownThread extends Thread {

	/** Constructs an instance of this class as a daemon thread. */
	ShutdownThread() {
	    super(ShutdownThread.class.getName());
	    setDaemon(true);
	}

	/** {@inheritDoc} */
	public void run() {
	    try {
		doShutdown();
	    } catch (RuntimeException e) {
		logger.logThrow(
		    Level.WARNING, e, "shutting down service throws");
		// swallow exception
	    }
	    setState(AbstractService.State.SHUTDOWN);
	}
    }
}
