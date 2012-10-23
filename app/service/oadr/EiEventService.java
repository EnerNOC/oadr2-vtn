package service.oadr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
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
        /**
         * Comparator to determine ordering of the OadrEvents
         * Expected ordering is
         * 1. Active events have priority
         * 2. Within Active, priority is determine by the EventDescriptor.Priority
         * 3. If both have equal EventDescriptor.Priority, the earlier start time is the higher priority
         * 4. Pending events are sorted by earlier start time
         * @author jlajoie
         *
         */
        class OadrEventComparator implements Comparator<OadrEvent>{
            public int compare(OadrEvent eventOne, OadrEvent eventTwo){
                
                boolean eventOneIsActive = eventOne.getEiEvent().getEventDescriptor().getEventStatus().equals(EventStatusEnumeratedType.ACTIVE);
                boolean eventTwoIsActive = eventTwo.getEiEvent().getEventDescriptor().getEventStatus().equals(EventStatusEnumeratedType.ACTIVE);
                int comparedEventPriority = eventOne.getEiEvent().getEventDescriptor().getPriority().compareTo(eventTwo.getEiEvent().getEventDescriptor().getPriority());
                int comparedEventDt = eventOne.getEiEvent().getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().compare(
                        eventTwo.getEiEvent().getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue());
                
                if(eventOneIsActive){
                    if(eventTwoIsActive){
                        if(comparedEventPriority == 0){
                            return comparedEventDt;
                        }
                        return comparedEventPriority;
                    }
                    return -1;
                }
                else if(eventTwoIsActive){
                    return 1;
                }
                else{
                    return comparedEventDt;
                }
            }
                
        }         
        
        EiResponse eiResponse = new EiResponse(); 
        if(!oadrRequestEvent.getEiRequestEvent().getRequestID().equals("")){
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
                
        List<VEN> vens = entityManager.createQuery("FROM VEN v WHERE v.venID = :ven")
            .setParameter("ven", oadrRequestEvent.getEiRequestEvent().getVenID())
            .getResultList();        
        List<EiEvent> events = new ArrayList<EiEvent>();
        for(VEN ven : vens){
                for(EiEvent event : (List<EiEvent>)entityManager.createQuery("SELECT event FROM EiEvent event WHERE event.eventDescriptor.eiMarketContext.marketContext.value = :market")
                        .setParameter("market", ven.getProgramId())
                        .getResultList()){
                    events.add(event);
            }            
            Logger.info(events.size() + "");
            List<OadrEvent> oadrEvents = new ArrayList<OadrEvent>();
            for(EiEvent e : events){
                Logger.info("Event - " + e.getEventDescriptor().getEventID());
                oadrEvents.add(new OadrEvent()
                    .withEiEvent(e)
                    .withOadrResponseRequired(ResponseRequiredType.ALWAYS) //TODO Not sure if set to always
                );
            }
            Collections.sort(oadrEvents, new OadrEventComparator());
            oadrDistributeEvent.withOadrEvents(oadrEvents);
        }
        return oadrDistributeEvent;
    }
        
    @SuppressWarnings("unchecked")
    @Transactional
    public static void persistFromRequestEvent(OadrRequestEvent requestEvent){
        createNewEm();
        List<VENStatus> venStatuses = new ArrayList<VENStatus>();
        venStatuses = entityManager.createQuery("SELECT status FROM VENStatus " +
            "status WHERE status.venID = :ven")
            .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
            .getResultList();
        if(venStatuses.size() == 0){
            venStatuses.add(new VENStatus());
        }
        for(VENStatus venStatus : venStatuses){
            venStatus.setTime(new Date());
            venStatus.setVenID(requestEvent.getEiRequestEvent().getVenID());
        
            createNewEm();
            
            List<VEN> customers = (List<VEN>)entityManager.createQuery("SELECT c FROM VEN c WHERE c.venID = :ven")
                    .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                    .getResultList();
            for(VEN customer : customers){
                venStatus.setProgram(customer.getProgramId());
                
                
                List<EiEvent> events = (List<EiEvent>)entityManager.createQuery("SELECT event FROM EiEvent event WHERE event.eventDescriptor.eiMarketContext.marketContext.value = :market")
                        .setParameter("market", venStatus.getProgram())
                        .getResultList();
                
                Logger.info("Events size - " + events.size());
                
                if(customer != null){  
                    for(EiEvent event : events){
                        venStatus.setEventID(event.getEventDescriptor().getEventID());
                        venStatus.setOptStatus("Pending 2");
                        createNewEm();
                        Logger.info("VenStatus - " + venStatus.getVenID());
                        entityManager.merge(venStatus);
                        entityManager.getTransaction().commit();
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public static void persistFromCreatedEvent(OadrCreatedEvent createdEvent){
        createNewEm();
        List<VENStatus> venStatuses = new ArrayList<VENStatus>();
        Logger.info("VEN - " + createdEvent.getEiCreatedEvent().getVenID());
        venStatuses = (List<VENStatus>)entityManager.createQuery("SELECT status FROM VENStatus " +
                "status WHERE status.venID = :ven")
                .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                .getResultList();
        for(VENStatus status : venStatuses){
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
