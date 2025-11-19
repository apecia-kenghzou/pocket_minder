package com.pocketminder.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Centralized thread management for background operations
 */
public class ThreadManager {
    private static ThreadManager instance;
    private final ExecutorService ioExecutor;
    private final ExecutorService networkExecutor;
    private final Handler mainHandler;

    private ThreadManager() {
        ioExecutor = Executors.newFixedThreadPool(2);
        networkExecutor = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized ThreadManager getInstance() {
        if (instance == null) {
            instance = new ThreadManager();
        }
        return instance;
    }

    /**
     * Execute IO operations (database, file access)
     */
    public Future<?> executeIO(Runnable task) {
        return ioExecutor.submit(task);
    }

    /**
     * Execute network operations
     */
    public Future<?> executeNetwork(Runnable task) {
        return networkExecutor.submit(task);
    }

    /**
     * Execute on main thread
     */
    public void executeMain(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler.post(task);
        }
    }

    /**
     * Execute on main thread with delay
     */
    public void executeMainDelayed(Runnable task, long delayMillis) {
        mainHandler.postDelayed(task, delayMillis);
    }

    /**
     * Shutdown all executors
     */
    public void shutdown() {
        ioExecutor.shutdown();
        networkExecutor.shutdown();
    }
}
