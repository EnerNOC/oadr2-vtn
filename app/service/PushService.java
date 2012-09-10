package service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import play.Logger;

import tasks.EventPushTask;

public class PushService{
    
    final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    ThreadPoolExecutor threadPool = null;    
    
    public PushService(){        
        Logger.info("Making push service");
        threadPool = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, queue);    
        threadPool.prestartCoreThread();
    }
    
    public void provide(EventPushTask task){
        queue.add(task);
    }

}
