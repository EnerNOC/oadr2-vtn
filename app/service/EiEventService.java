package service;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

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

import play.Logger;
import play.db.jpa.Transactional;
import play.mvc.Result;

public class EiEventService{

    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();

    public EiEventService(){
    }
    
    //change to handle xxxx oadrpayload
    public static Result handleOadrPayload(Object o) throws JAXBException{
        if(o instanceof OadrRequestEvent){
            //cast to a request event before passing to send()
            return sendDistributeFromRequest(o);
        }
        else if(o instanceof OadrCreatedEvent){
            return handleOadrCreated((OadrCreatedEvent)o);
        }
        else if(o instanceof OadrResponse){
            persistFromResponse((OadrResponse)o);
            return play.mvc.Action.ok();
        }
        else{
            //move to controller for http specific, have it throw Exception
            return play.mvc.Action.badRequest("Object was not of correct class");
        }
    }    
    
    @Transactional
    public static Result handleOadrCreated(OadrCreatedEvent oadrCreatedEvent) throws JAXBException{
        persistFromCreatedEvent(oadrCreatedEvent);
        createNewEm();
        entityManager.persist(oadrCreatedEvent);
        entityManager.getTransaction().commit();
        
        EiResponse eiResponse = new EiResponse()        
            //TODO Need to handle non 200 responses
            .withResponseCode(new ResponseCode("200"))
            .withResponseDescription("Optional description!"); 
        
        return play.mvc.Action.ok(marshalObject(eiResponse));
    }
    
    @Transactional
    public static Result sendDistributeFromRequest(Object o) throws JAXBException{
        OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
        EiResponse eiResponse = new EiResponse(); 
        if(oRequestEvent.getEiRequestEvent().getRequestID() != null){
            eiResponse.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
        }
        
        //TODO Need to handle non 200 responses
        eiResponse.setResponseCode(new ResponseCode("200"));        
        createNewEm();
        entityManager.persist(oRequestEvent);  
        entityManager.getTransaction().commit();        
        persistFromRequestEvent(oRequestEvent);    
        OadrDistributeEvent response = new OadrDistributeEvent().withEiResponse(eiResponse);
        
        String eventId = null;
        EiEvent event = null;
        
        try{
            eventId = (String)entityManager.createQuery("SELECT s.eventID FROM StatusObject s WHERE s.venID = :ven")
                .setParameter("ven", oRequestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();        
            
            event = (EiEvent)entityManager.createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor " +
                    "descriptor WHERE descriptor.eventID = :id and event.hjid = descriptor.hjid")
                    .setParameter("id", eventId)
                    .getSingleResult();   
            
        }catch(NoResultException e){e.printStackTrace();};
        response.withOadrEvents(new OadrEvent().withEiEvent(event));
        response.withRequestID(eiResponse.getRequestID());
        return play.mvc.Action.ok(marshalObject(response));
    }
    
    public static String marshalObject(Object o) throws JAXBException{  
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");    
        Marshaller marshaller = jaxbContext.createMarshaller();      
        StringWriter sw = new StringWriter();
        marshaller.marshal(o, sw);
        play.mvc.Controller.response().setContentType("application/xml");
        return sw.toString() + '\n';
    }
        
    @SuppressWarnings("unchecked")
    @Transactional
    public static void persistFromRequestEvent(OadrRequestEvent requestEvent){
        createNewEm();
        VENStatus venStatus = null;
        try{
            venStatus = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
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
        
        //TODO Change this to throw an exception then catch it and return a 500/400 error
        customer = (VEN)entityManager.createQuery("SELECT c FROM Customers c WHERE c.venID = :ven")
                .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
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

    @Transactional
    public static void persistFromCreatedEvent(OadrCreatedEvent createdEvent){
        createNewEm();
        VENStatus status = null;        
        try{
            status = (VENStatus)entityManager.createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                    .getSingleResult();
        }catch(Exception e){Logger.warn("Caught exception, either NoResult or NonUnique from persistFromCreatedEvent() in EiEventService");};
        if(status != null){
            status.setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponses().get(0).getOptType().toString());
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);    
            entityManager.getTransaction().commit();
        }
    }
    
    public static void persistFromResponse(OadrResponse response){
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
    
    public static Object unmarshalRequest(byte[] chars) throws JAXBException{    
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Logger.info(new String(chars));
        Object o = unmarshaller.unmarshal(new ByteArrayInputStream(chars));
        return o;
    }
    
    public static void createNewEm(){
        entityManager = entityManagerFactory.createEntityManager();
        if(!entityManager.getTransaction().isActive()){
            entityManager.getTransaction().begin();
        }
    }
    
}
