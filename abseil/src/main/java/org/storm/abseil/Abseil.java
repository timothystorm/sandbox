package org.storm.abseil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.storm.abseil.runnable.RunnableSupplier;

/**
 * <p>
 * An abseil is a controlled descent down a vertical drop. In the same vein this {@link Abseil} is a controlled
 * execution of many {@link Runnable}s until either a timeout occurs, no {@link Runnable}s are remaining or the process
 * is terminated by the client.
 * </p>
 * <p>
 * Like a physical abseil this helps the user prevent run away or locked process; both of which are easy to do when
 * thread programming.
 * </p>
 * 
 * @author Timothy Storm
 */
public class Abseil {
  private final AtomicInteger _count = new AtomicInteger();

  /**
   * Executes the {@link Runnable}s in the {@link RunnableSupplier}
   */
  private class AbseilTask implements Runnable, RunnableMonitor.Listener {
    /** active tasks, this is not exact but is a best effort answer */
    private final AtomicLong      _active  = new AtomicLong();

    /** average execution time of strategy tasks */
    private final AtomicLong      _average = new AtomicLong();

    /** executor that does the work of executing the strategy tasks */
    private final ExecutorService _executor;

    AbseilTask(ExecutorService executor) {
      _executor = executor;
    }

    @Override
    public void accept(final RunnableMonitor monitor) {
      _active.set(monitor.getActiveCount());
      _average.set(monitor.getAverageRuntime());
    }

    /**
     * @return current state of this {@link Abseil}
     */
    public State getState() {
      _stateLock.lock();

      try {
        State currentState = _state;
        return currentState;
      } finally {
        _stateLock.unlock();
      }
    }

    /**
     * setup task resources
     */
    void init() {
      if (getState().is(State.INIT)) {
        transitionTo(State.STARTING);
        _startAt.set(System.currentTimeMillis());
        transitionTo(State.RUNNING);
      }
    }

    /**
     * Processes the {@link RunnableSupplier} by submitting a new command for each {@link Runnable} of the
     * {@link RunnableSupplier}.
     */
    @Override
    public void run() {
      init();

      Runnable run = null;
      while (getState().is(State.RUNNING) && (run = _runnableFactory.get()) != null) {
        _executor.execute(new RunnableMonitor(run, this));
      }

      shutdown(true);
    }

    /**
     * Shutdown this task.
     * 
     * @param graceful
     *          - (true) allow running tasks to finish. (false) stop running tasks.
     */
    private void shutdown(boolean graceful) {
      if (getState().is(State.RUNNING)) {
        transitionTo(State.SHUTTING_DOWN);

        if (log.isDebugEnabled()) log.debug("shut down starting...");

        try {
          if (graceful) _executor.shutdown();
          else _executor.shutdownNow();

          // configure terminate timeout by using either the min/max wait or the average execution time of other tasks,
          // as long as that average is between the min/max wait bounds.
          long wait = _average.get() * _active.get();
          long median = Math.min(Math.max(wait, _minWait), _maxWait);
          _executor.awaitTermination(median, TimeUnit.MILLISECONDS);

          if (_executor.isTerminated()) transitionTo(State.SHUTDOWN);
          else kill(/* forcefully kill this thread */);
        } catch (InterruptedException e) {
          return;
        }
      }
    }

    /**
     * Transitions current state to the desired state
     * 
     * @param state
     *          - to transition to
     * @return previous state
     */
    private State transitionTo(final State state) {
      _stateLock.lock();
      try {
        State prev = _state;
        _state = state;

        if (log.isInfoEnabled()) log.info("{} -> {}", prev, _state);
        return prev;
      } finally {
        _stateLock.unlock();
      }
    }
  }

  /**
   * State of the Abseil
   */
  public enum State {
    INIT,
    RUNNING,
    SHUTDOWN,
    SHUTTING_DOWN,
    STARTING;

    /**
     * Checks this state against the provided state
     * 
     * @param state
     *          - to check this state against
     * @return true if this state and the argument state are the same, false otherwise
     */
    public boolean is(State state) {
      return this.equals(state);
    }
  }

  private static final Logger log           = LoggerFactory.getLogger(Abseil.class);

  /** max runtime millis the abseil should run */
  private final Long          _maxRuntime;

  /** max time to wait for a shutdown */
  private Long                _maxWait      = TimeUnit.SECONDS.toMillis(10);

  /** min time to wait for a shutdown */
  private Long                _minWait      = TimeUnit.SECONDS.toMillis(3);

  /** abseil process that is run apart from the main thread and can be stopped if the abseil task is unresponsive */
  private Thread              _processor;

  private RunnableSupplier    _runnableFactory;

  /** time this abseil process started */
  private final AtomicLong    _startAt      = new AtomicLong(Long.MIN_VALUE);

  /** state of this abseil */
  private State               _state        = State.INIT;

  /** locks state mutation to prevent overlapping transitions */
  private final Lock          _stateLock    = new ReentrantLock();

  private final AbseilTask    _task;

  /** period sleep duration for the timeout daemon */
  private Long                _timeoutSleep = TimeUnit.MILLISECONDS.toMillis(500);

  /**
   * Creates an {@link Abseil} from parameters in the {@link AbseilBuilder}
   * 
   * @param builder
   */
  public Abseil(AbseilBuilder builder) {
    this(builder.newExecutorService(), builder.getMaxRuntime(), TimeUnit.MILLISECONDS);
  }

  /**
   * Creates an {@link Abseil} that uses the provided {@link ExecutorService} to process the tasks and will timeout, if
   * processing hasn't completed, before the given maxRuntime.
   * 
   * @param executor
   *          - that will execute the tasks
   * @param maxRuntime
   *          - when this {@link Abseil} stops running if the tasks are done or not
   * @param unit
   *          - of the maxRuntime
   */
  public Abseil(ExecutorService executor, long maxRuntime, TimeUnit unit) {
    assert maxRuntime > 0;

    _maxRuntime = unit.toMillis(maxRuntime);
    _task = new AbseilTask(executor);
    _processor = new Thread(_task, "Abseil - processor - " + _count.incrementAndGet());
  }

  public State getState() {
    return _task.getState();
  }

  /**
   * Forcefully kill this {@link Abseil} processor thread. This doesn't allow for proper resource cleanup and should
   * only be called as a last resort.
   */
  private void kill() {
    log.warn("killing process");
    _processor.interrupt();
    Thread.currentThread().interrupt();
  }

  /**
   * configure the maximum time to wait during a shutdown before a forced shutdown is initiated
   * 
   * @param time
   * @param unit
   * @return this {@link Abseil} for further configuration
   */
  public Abseil maxTimeoutWait(Long time, TimeUnit unit) {
    assert time >= 0;

    _maxWait = unit.toMillis(time);
    return this;
  }

  /**
   * configure the minimum time to wait during a shutdown before a forced shutdown is initiated
   * 
   * @param time
   * @param unit
   * @return this this {@link Abseil} for further configuration for further configuration
   */
  public Abseil minTimeoutWait(Long time, TimeUnit unit) {
    assert time >= 0;

    _minWait = unit.toMillis(time);
    return this;
  }

  /**
   * Process the {@link RunnableSupplier}
   */
  public void process(final RunnableSupplier runnableFactory) {
    _runnableFactory = runnableFactory;

    _startAt.set(System.currentTimeMillis());

    // start timeout daemon
    Thread timeoutDaemon = new Thread(() -> {
      try {
        while ((System.currentTimeMillis() - _startAt.get()) < _maxRuntime) {
          Thread.sleep(_timeoutSleep);
        }
        shutdown();
      } catch (InterruptedException e) {
        kill();
      }
    }, "Abseil - timeout monitor");
    timeoutDaemon.setDaemon(true);
    timeoutDaemon.start();

    // setup short circuit shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      shutdown();
    }));

    _processor.start();
  }

  /**
   * Tries to shutdown the Abseil as fast as possible
   */
  public void shutdown() {
    _task.shutdown(false);
  }
}
