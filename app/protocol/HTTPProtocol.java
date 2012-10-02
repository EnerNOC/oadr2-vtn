package protocol;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;

import play.Logger;
import service.XmppService;

//TODO Need to find out if whether or not "send" for HTTP is appropriate, since a POST using CURL won't return a response
//doing it this way, perhaps look at the response() 
public class HTTPProtocol extends BaseProtocol{
    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();
    static XmppService xmppService = XmppService.getInstance();

    public HTTPProtocol(){
        this.setProtocolType(ProtocolType.HTTP);
    }

    @Override
    public void send(String uri, OadrResponse oadrResponse, String pid) {
        xmppService.sendObjectToJID(oadrResponse, uri, pid);
    }

    @Override
    public void send(String uri, EiEvent eiEvent) {
        
    }

    @Override
    public void send(String uri, OadrDistributeEvent oadrDistributeEvent) {
        
    }

    @Override
    public void send(String uri, OadrCreatedEvent oadrCreatedEvent) {
        
    }

    @Override
    public void send(String uri, OadrRequestEvent oadrRequestEvent) {
        
    }
    
    @Override
    public void send(String uri, Object oadrObject){
        Logger.info("Sending an object via HTTP to: " + uri);
    }

}