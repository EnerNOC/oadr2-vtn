package service.oadr;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.enernoc.open.oadr2.model.OadrDistributeEvent.OadrEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.enernoc.open.oadr2.model.ResponseCode;
import org.enernoc.open.oadr2.model.ResponseRequiredType;

import play.Logger;
import play.db.jpa.Transactional;
import service.XmppService;

public class EiEventService{

    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();

    private volatile static EiEventService instance = null;
    
    public EiEventService(){
        
    }
    
    public static EiEventService getInstance(){
        if(instance == null){
            synchronized(XmppService.class){
                if(instance == null){
                    instance = new EiEventService();
                }
            }
        }
        return instance;
    }
    
    //Change to return Object instead of result
    public Object handleOadrPayload(Object o){
        if(o instanceof OadrRequestEvent){
            //cast to a request event before passing to send()
            return handleOadrRequest((OadrRequestEvent)o);
        }
        else if(o instanceof OadrCreatedEvent){
            return handleOadrCreated((OadrCreatedEvent)o);
        }
        else if(o instanceof OadrResponse){
            handleFromOadrResponse((OadrResponse)o);
            return null;
        }
        else{
            //TODO find out what to return if an http payload is of an incorrect type, probably http status code
            //move to controller for http specific, have it throw Exception
            throw new RuntimeException("Object was not of correct class");
        }
    }    
    
    @Transactional
    public static OadrResponse handleOadrCreated(OadrCreatedEvent oadrCreatedEvent){
        persistFromCreatedEvent(oadrCreatedEvent);
        createNewEm();
        entityManager.persist(oadrCreatedEvent);
        entityManager.getTransaction().commit();
        
        return new OadrResponse()
            .withEiResponse(new EiResponse()
                .withRequestID("TH_REQUEST_ID")
                .withResponseCode(new ResponseCode("200"))
                .withResponseDescription("Optional description!")); 
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public static OadrDistributeEvent handleOadrRequest(OadrRequestEvent oadrRequestEvent){
        EiResponse eiResponse = new EiResponse(); 
        if(!oadrRequestEvent.getEiRequestEvent().getRequestID().equals(null)){
            Logger.info("Request ID is: " + oadrRequestEvent.getEiRequestEvent().getRequestID());
            eiResponse.setRequestID(oadrRequestEvent.getEiRequestEvent().getRequestID());
        }
        else{
            eiResponse
                .withRequestID("TH_REQUEST_ID");
        }
        
        //TODO Need to handle non 200 responses
        eiResponse.setResponseCode(new ResponseCode("200"));    
        
        createNewEm();
        entityManager.persist(oadrRequestEvent);  
        entityManager.getTransaction().commit();        
        persistFromRequestEvent(oadrRequestEvent);    
        OadrDistributeEvent oadrDistributeEvent = new OadrDistributeEvent()
                .withEiResponse(eiResponse)
                .withRequestID("TH_REQUEST_ID")
                .withVtnID("TH_VTN");
                
                
        try {
            VEN ven = (VEN) entityManager.createQuery("FROM VEN v WHERE v.venID = :ven")
                .setParameter("ven", oadrRequestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();        
            
            List<EiEvent> events = (List<EiEvent>)entityManager.createQuery("SELECT event FROM EiEvent event, EventDescriptor$EiMarketContext " +
                    "descriptor WHERE descriptor.marketContext.value = :market")
                    .setParameter("market", ven.getProgramId())
                    .getResultList();
            List<OadrEvent> oadrEvents = new ArrayList<OadrEvent>();
            
            for(EiEvent e : events){
                oadrEvents.add(new OadrEvent()
                    .withEiEvent(e)
                    .withOadrResponseRequired(ResponseRequiredType.ALWAYS) //TODO Not sure if set to always
                    );
            }
            
            oadrDistributeEvent.withOadrEvents(oadrEvents);
        }        
        catch (NoResultException e) {
            Logger.warn("Could not find VEN. Query was for " + oadrRequestEvent.getEiRequestEvent().getVenID(), e);            
        }
        return oadrDistributeEvent;
    }
        
    @SuppressWarnings("unchecked")
    @Transactional
    public static void persistFromRequestEvent(OadrRequestEvent requestEvent){
        createNewEm();
        VENStatus venStatus = null;
        try{
            venStatus = (VENStatus)entityManager.createQuery("SELECT status FROM VENStatus " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                    .getSingleResult();
        }
        catch(NoResultException e){
            venStatus = new VENStatus();
        };
        venStatus.setTime(new Date());
        venStatus.setVenID(requestEvent.getEiRequestEvent().getVenID());
        
        VEN customer = null;
        EiEvent event = null;
        createNewEm();
        
        customer = (VEN)entityManager.createQuery("SELECT c FROM VEN c WHERE c.venID = :ven")
                .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();
                  
        venStatus.setProgram(customer.getProgramId());
        
        event = (EiEvent)entityManager.createQuery("SELECT event FROM EiEvent event, EventDescriptor$EiMarketContext " +
                "marketContext WHERE marketContext.marketContext.value = :market and event.hjid = marketContext.hjid")
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

    @Transactional
    public static void persistFromCreatedEvent(OadrCreatedEvent createdEvent){
        createNewEm();
        VENStatus status = null;        
        try{
            status = (VENStatus)entityManager.createQuery("SELECT status FROM VENStatus " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                    .getSingleResult();
        }catch(Exception e){
            e.printStackTrace();
        };
        if(status != null){
            status.setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponses().get(0).getOptType().toString());
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);    
            entityManager.getTransaction().commit();
        }
    }
    
    public static void handleFromOadrResponse(OadrResponse response){
        VENStatus status = null;
        createNewEm();
        try{
            status = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.requestID = :requestId")
                    .setParameter("requestId", response.getEiResponse().getRequestID())
                    .getResultList().get(0);
        }catch(NoResultException e){};
        if(status != null){
            status.setTime(new Date());
            status.setOptStatus("Pending 2");
            createNewEm();
            entityManager.merge(status);
            entityManager.getTransaction().commit();
        }
    }
        
    public static void createNewEm(){
        entityManager = entityManagerFactory.createEntityManager();
        if(!entityManager.getTransaction().isActive()){
            entityManager.getTransaction().begin();
        }
    }
    
}
