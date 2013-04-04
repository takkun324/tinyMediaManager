/*
 * Copyright 2012 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tinymediamanager;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.tinymediamanager.ui.TmmSwingWorker;

/**
 * @author Myron Boyle
 * @version $Id$
 */
public abstract class TmmThreadPool extends TmmSwingWorker {

  /** The Constant LOGGER. */
  private static final Logger       LOGGER    = Logger.getLogger(TmmThreadPool.class);
  private ThreadPoolExecutor        pool      = null;
  private CompletionService<Object> service   = null;
  protected boolean                 cancel    = false;
  private int                       taskcount = 0;
  private int                       taskdone  = 0;
  private String                    poolname  = "";

  /**
   * create new ThreadPool
   * 
   * @param threads
   *          amount of threads
   * @param name
   *          a name for the logging
   */
  public void initThreadPool(int threads, String name) {
    this.poolname = name;
    pool = new ThreadPoolExecutor(threads, threads, // max threads
        2, TimeUnit.SECONDS, // time to wait before closing idle workers
        new LinkedBlockingQueue<Runnable>(), // our queue
        new TmmThreadFactory(name) // our thread settings
    );
    pool.allowCoreThreadTimeOut(true);
    this.service = new ExecutorCompletionService<Object>(pool);
  }

  /**
   * submits a new callable to thread pool
   * 
   * @param task
   *          the callable
   */
  public void submitTask(Callable<Object> task) {
    taskcount++;
    service.submit(task);
  }

  /**
   * submits a new runnable to thread pool
   * 
   * @param task
   *          the runnable
   */
  public void submitTask(Runnable task) {
    taskcount++;
    service.submit(task, null);
  }

  public void waitForCompletionOrCancel() {
    pool.shutdown();
    while (!cancel && !pool.isTerminated()) {
      try {
        final Future<Object> future = service.take();
        taskdone++;
        callback(future.get());
      }
      catch (InterruptedException e) {
        LOGGER.error("ThreadPool " + this.poolname + " interrupted!", e);
      }
      catch (ExecutionException e) {
        LOGGER.error("ThreadPool " + this.poolname + ": Error getting result!", e);
      }
    }
    if (cancel) {
      try {
        pool.getQueue().clear();
        pool.awaitTermination(3, TimeUnit.SECONDS);
        pool.shutdownNow();
      }
      catch (InterruptedException e) {
        LOGGER.error("ThreadPool " + this.poolname + " interrupted in shutdown!", e);
      }
    }
  }

  /**
   * callback for result
   * 
   * @param obj
   *          the result of the finished thread.
   */
  public abstract void callback(Object obj);

  /**
   * returns the amount of submitted tasks
   */
  public int getTaskcount() {
    return taskcount;
  }

  /**
   * returns the amount of executed tasks
   */
  public int getTaskdone() {
    return taskdone;
  }

  /**
   * cancel the pool
   */
  public void cancelThreadPool() {
    this.cancel = true;
  }

  /**
   * a copy of the default thread factory, just to set the pool name
   */
  static class TmmThreadFactory implements ThreadFactory {
    // static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup   group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String        namePrefix;

    TmmThreadFactory(String poolname) {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = "pool-" + poolname + "-thread-";
    }

    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
      if (t.isDaemon()) {
        t.setDaemon(false);
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }
}