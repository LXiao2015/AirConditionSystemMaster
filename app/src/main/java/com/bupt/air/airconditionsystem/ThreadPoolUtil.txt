package com.bupt.air.airconditionsystem;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by OnlySaturday on 2015/5/26.
 */
public class ThreadPoolUtil {
    private static ExecutorService instance = null;

    private ThreadPoolUtil() {
    }

    public static ExecutorService getInstance() {
        if (instance == null)
            instance = Executors.newCachedThreadPool();
        return instance;
    }
}
