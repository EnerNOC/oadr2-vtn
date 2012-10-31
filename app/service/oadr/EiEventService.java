package service.oadr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import models.Event;
import models.VEN;
import models.VENStatus;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.EventResponses.EventResponse;
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
        String responseCode = verifyOadrCreated(oadrCreatedEvent);
        if(oadrCreatedEvent.getEiCreatedEvent().getEiResponse().getResponseCode().getValue().charAt(0) == '2'){
            persistFromCreatedEvent(oadrCreatedEvent);
            createNewEm();
            entityManager.persist(oadrCreatedEvent);
            entityManager.getTransaction().commit();
            
            return new OadrResponse()
                .withEiResponse(new EiResponse()
                    .withRequestID("TH_REQUEST_ID")
                    .withResponseCode(new ResponseCode(responseCode))
                    .withResponseDescription("Optional description!"));
        }
        else{
            return new OadrResponse()
                .withEiResponse(new EiResponse()
                        .withRequestID("TH_REQUEST_ID")
                        .withResponseCode(new ResponseCode("200"))
                        .withResponseDescription("Incoming event contained errors"));
        }
    }
    
    @SuppressWarnings("unchecked")
    @Transactional
    public static String verifyOadrCreated(OadrCreatedEvent oadrCreatedEvent){
        createNewEm();
        if(oadrCreatedEvent.getEiCreatedEvent().getEventResponses() != null){
            String eventId = oadrCreatedEvent.getEiCreatedEvent().getEventResponses().getEventResponses().get(0).getQualifiedEventID().getEventID();
            long modificationNumber = oadrCreatedEvent.getEiCreatedEvent().getEventResponses().getEventResponses().get(0).getQualifiedEventID().getModificationNumber();
            ArrayList<EiEvent> events = (ArrayList<EiEvent>) entityManager.createQuery("SELECT event FROM EiEvent event WHERE event.eventDescriptor.eventID = :event AND event.eventDescriptor.modificationNumber = :modNumber")
                    .setParameter("event", eventId)
                    .setParameter("modNumber", modificationNumber)
                    .getResultList();
            if(events.size() > 0){
                return "200";
            }
        }
        return "400";
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
        //TODO see if setting this to 200 breaks the 0710
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
            List<OadrEvent> oadrEvents = new ArrayList<OadrEvent>();
            for(EiEvent e : events){
                oadrEvents.add(new OadrEvent()
                    .withEiEvent(e)
                    .withOadrResponseRequired(ResponseRequiredType.ALWAYS) //TODO Not sure if set to always
                );
            }
            Collections.sort(oadrEvents, new OadrEventComparator());
            //oadrEvents = listReduce(oadrEvents);
            if(oadrRequestEvent.getEiRequestEvent().getReplyLimit() != null){
                oadrEvents = removeEventsOverLimit(oadrEvents, oadrRequestEvent.getEiRequestEvent().getReplyLimit().intValue());
            }
            oadrDistributeEvent.withOadrEvents(oadrEvents);
        }
        return oadrDistributeEvent;
    }
    
    public static ArrayList<OadrEvent> removeEventsOverLimit(List<OadrEvent> events, int replyLimit){
        ArrayList<OadrEvent> returnList = new ArrayList<OadrEvent>();
        for(int i = 0; i < replyLimit && i < events.size(); i++){
            returnList.add(events.get(i));
        }
        return returnList;
    }
    
    /**
     * 
     * @param oadrEvents - List of OadrEvent containing all events within Market Contexts
     * @return - The reduced ArrayList containing no overlapping events within the same MarketContext
     */
    public static ArrayList<OadrEvent> listReduce(List<OadrEvent> oadrEvents){
        Map<String, OadrEvent> eventMap = new HashMap<String, OadrEvent>();
        for(OadrEvent event : oadrEvents){
            String marketContext = event.getEiEvent().getEventDescriptor().getEiMarketContext().getMarketContext().getValue();
            XMLGregorianCalendar eventOneStartDt = event.getEiEvent().getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue();
            XMLGregorianCalendar eventOneEndDt = event.getEiEvent().getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue();
            if(eventMap.containsKey(marketContext)){
                OadrEvent mappedEvent = eventMap.get(marketContext);
                eventOneEndDt.add(getDuration(event.getEiEvent()));
                //TODO Do stuff to check if they overlap, if they do then add the earlier start date ex first date + duration compared to second date == 1 make sure the first one is added, else add the second one
                XMLGregorianCalendar eventTwoDt = eventMap.get(marketContext).getEiEvent().getEiActivePeriod().getProperties()
                        .getDtstart().getDateTime().getValue();
                int comparedDt = eventOneEndDt.compare(eventTwoDt);
                if(comparedDt > 0){
                    //return the lesser start date
                    if(eventOneStartDt.compare(eventTwoDt) != 1){
                        eventMap.put(marketContext, event);
                    }
                }
                
            }
            else{
                eventMap.put(marketContext, event);
            }
        }
        return new ArrayList<OadrEvent>(eventMap.values());
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
                Logger.info("Above testing the EiEvent query");
                List<EiEvent> events = (List<EiEvent>)entityManager.createQuery("SELECT event FROM EiEvent event WHERE event.eventDescriptor.eiMarketContext.marketContext.value = :market")
                        .setParameter("market", venStatus.getProgram())
                        .getResultList();
                
                if(customer != null){  
                    for(EiEvent event : events){
                        venStatus.setEventID(event.getEventDescriptor().getEventID());
                        venStatus.setOptStatus("Pending 2");
                        createNewEm();
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
        venStatuses = (List<VENStatus>)entityManager.createQuery("SELECT status FROM VENStatus " +
                "status WHERE status.venID = :ven")
                .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                .getResultList();
        for(VENStatus status : venStatuses){
            if(createdEvent.getEiCreatedEvent().getEventResponses() != null){
                for(EventResponse eventResponse : createdEvent.getEiCreatedEvent().getEventResponses().getEventResponses()){
                    status.setOptStatus(eventResponse.getOptType().toString());
                }
            }
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
    
    public static Duration getDuration(EiEvent event){
        DatatypeFactory df = null;
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
          e.printStackTrace();
        }
        return df.newDuration(Event.minutesFromXCal(event.getEiActivePeriod().getProperties().getDuration().getDuration().getValue()) * 60000);
    }
    
}
