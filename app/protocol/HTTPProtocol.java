package protocol;

import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;

import models.VEN;
import models.VENStatus;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;

import play.Logger;
//http://www.yelp.com/biz/cambridge-family-ymca-cambridge

//TODO Need to find out if whether or not "send" for HTTP is appropriate, since a POST using CURL won't return a response
//doing it this way, perhaps look at the response() 
public class HTTPProtocol extends BaseProtocol{
    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();

    public HTTPProtocol(){
        this.setProtocolType(ProtocolType.HTTP);
    }

    @Override
    public void send(VEN vtn, OadrResponse oadrResponse) {
        VENStatus status = null;
        createNewEm();
        try{
            status = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.requestID = :requestId")
                    .setParameter("requestId", oadrResponse.getEiResponse().getRequestID())
                    .getResultList().get(0);
        }catch(NoResultException e){};
        if(status != null){
            status.setTime(new Date());
            if(status.getOptStatus().equals("Pending 1"))
                status.setOptStatus("Pending 2");
            createNewEm();
            entityManager.merge(status);
            entityManager.getTransaction().commit();
        }
    }

    @Override
    public void send(VEN vtn, OadrDistributeEvent oadrDistributeEvent) {
        //Shouldn't ever receive a DistributeEvent, might not need this method
    }

    @Override
    public void send(VEN vtn, OadrCreatedEvent oadrCreatedEvent) {
        createNewEm();
        VENStatus status = null;        
        try{
            status = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", oadrCreatedEvent.getEiCreatedEvent().getVenID())
                    .getSingleResult();
        }catch(Exception e){Logger.warn("send(OadrCreatedEvent) threw either a NoResult or MultipleResult Exception");};
        if(status != null){
            status.setOptStatus(oadrCreatedEvent.getEiCreatedEvent().getEventResponses().getEventResponse().get(0).getOptType().toString());
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);    
            entityManager.getTransaction().commit();
        }        
    }

    @SuppressWarnings("unchecked")
    @Override
    public void send(VEN vtn, OadrRequestEvent oadrRequestEvent){
        EiResponse eiResponse = new EiResponse(); 
        
        if(oadrRequestEvent.getEiRequestEvent().getRequestID() != null){
            eiResponse.setRequestID(oadrRequestEvent.getEiRequestEvent().getRequestID());
        }     
        
        createNewEm();
        VENStatus venStatus = null;
        
        try{
            venStatus = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", oadrRequestEvent.getEiRequestEvent().getVenID())
                    .getSingleResult();
        }catch(NoResultException e){Logger.warn("HTTP send(OadrRequestEvent) threw a NoResult or MultipleResult Exception");};
        
        if(venStatus == null){
            venStatus = new VENStatus();
        }
        
        venStatus.setTime(new Date());
        venStatus.setVenID(oadrRequestEvent.getEiRequestEvent().getVenID());        
        VEN customer = null;
        EiEvent event = null;
        createNewEm();
        
        //TODO Change this to throw an exception then catch it and return a 500/400 error
        customer = (VEN)entityManager.createQuery("SELECT c FROM Customers c WHERE c.venID = :ven")
                .setParameter("ven", oadrRequestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();
        
        venStatus.setProgram(customer.getProgramId());
        
        event = (EiEvent)entityManager.createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor$EiMarketContext " +
                "marketContext WHERE marketContext.marketContext = :market and event.hjid = marketContext.hjid")
                .setParameter("market", venStatus.getProgram())
                .getSingleResult();
                
        if(customer != null && event != null){  
            venStatus.setEventID(event.getEventDescriptor().getEventID());
            venStatus.setOptStatus("Pending 2");
            createNewEm();
            entityManager.merge(venStatus);
            entityManager.getTransaction().commit();
        }
    }
    
    public static void createNewEm(){
        entityManager = entityManagerFactory.createEntityManager();
        if(!entityManager.getTransaction().isActive()){
            entityManager.getTransaction().begin();
        }
    }

    @Override
    public void send(VEN vtn, EiEvent eiEvent) {        
        VENStatus status = null;
        createNewEm();
        try{
            status = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", eiEvent.getEventDescriptor().getEventID())
                    .getSingleResult();
        }catch(NoResultException e){};
        if(status == null){
            status = new VENStatus();
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);
            entityManager.getTransaction().commit();
        }        
    }

}
