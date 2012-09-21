package service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import models.VEN;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.ResponseCode;
import org.enernoc.open.oadr2.model.OadrDistributeEvent.OadrEvent;

import play.db.jpa.Transactional;

import tasks.EventPushTask;

public class PushService{
    
    final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    ThreadPoolExecutor threadPool = null;    
    
    public PushService(){        
        threadPool = new ThreadPoolExecutor(2, 2, 10, TimeUnit.SECONDS, queue);    
        threadPool.prestartAllCoreThreads();
    }
    
    public void provide(EventPushTask task){
        queue.add(task);
    }
    
    @Transactional
    public void pushNewEvent(EiEvent e, List<VEN> vens) throws JAXBException{       
        for(VEN v : vens){
            OadrDistributeEvent distribute = new OadrDistributeEvent()
            .withOadrEvents(new OadrEvent().withEiEvent(e));
            
            distribute.setEiResponse(new EiResponse().withRequestID("Request ID")
                    .withResponseCode(new ResponseCode("200"))
                    .withResponseDescription("Response Description"));
            distribute.getOadrEvents().add(new OadrEvent().withEiEvent(e));
            distribute.setRequestID("Request ID");
            distribute.setVtnID("VTN ID");
           queue.add(new EventPushTask(v.getClientURI(), distribute));     
        }
    }

}
