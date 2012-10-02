package tasks;

import java.util.Random;

import org.enernoc.open.oadr2.model.OadrResponse;

import protocol.IProtocol;
import protocol.ProtocolRegistry;
import service.XmppService;

public class EventPushTask implements Runnable{
    

    Object oadrObject = null;
    String uri = null;
    String pid = null;
    
    XmppService xmppService = XmppService.getInstance();
    ProtocolRegistry protocolRegistry = ProtocolRegistry.getInstance();
    
    public EventPushTask(String uri, Object oadrObject){
        this.oadrObject = oadrObject;
        //switch to uri
        this.uri = uri;
    }

    @Override
    public void run() {
        //Logger.info("Running event for uri: " + uri + " - " + System.currentTimeMillis());
        Random r = new Random();
        try {
            Thread.sleep(r.nextInt(3000) + 1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //if else for protocol type
        IProtocol protocol = protocolRegistry.getProtocol(uri);
        if(pid != null){
            protocol.send(uri, (OadrResponse)oadrObject, pid);
        }
        protocolRegistry.getProtocol(uri).send(uri, oadrObject);
    }

}
