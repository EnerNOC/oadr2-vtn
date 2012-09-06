package service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import play.Logger;

import tasks.EventPushTask;

public class PushService{
    
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(10, true);
    ThreadPoolExecutor threadPool = null;    
    
    public PushService(){        
        Logger.info("Making push service");
        threadPool = new ThreadPoolExecutor(10, 10, 10, TimeUnit.SECONDS, queue);
        //threadPool.prestartAllCoreThreads();
        
    }
    
    public void provide(EventPushTask task){
        queue.add(task);
    }
    
    public void executeTask(){
        while(queue.size() > 0){
            threadPool.execute(queue.poll());
        }
        //threadPool.shutdown();
    }

}
