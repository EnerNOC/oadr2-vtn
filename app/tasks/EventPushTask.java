package tasks;

import org.enernoc.open.oadr2.model.OadrDistributeEvent;

import play.Logger;

import service.XmppService;

public class EventPushTask implements Runnable{
    

    OadrDistributeEvent distributeEvent = null;
    String jid = null;
    XmppService xmppService = XmppService.getInstance();
    
    public EventPushTask(String jid, OadrDistributeEvent distributeEvent){
        this.distributeEvent = distributeEvent;
        this.jid = jid;
    }

    @Override
    public void run() {
        Logger.info("Running event for jid: " + jid);
        
        //TODO Write the method to persist to the database and get the send information ready
        //ex call a send for the JID with the message contained, might need to add the contained message for
        //it to be appropriate, will also need to access the XMPPConnection so might need to make that injected or a singleton
    }

}
