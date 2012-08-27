package service;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.inject.Inject;

import models.CustomerForm;
import models.VENStatus;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent.OadrEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.w3c.dom.Document;

import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

public class EiEventService{

    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();

    public EiEventService(){
    }
    
    public static Result sendMarshalledObject(Object o) throws JAXBException{
        if(o instanceof OadrRequestEvent){
            return sendDistributeFromRequest(o);
        }
        else if(o instanceof OadrCreatedEvent){
            return sendResponseFromCreated(o);
        }
        else if(o instanceof OadrResponse){
            return play.mvc.Action.ok("Got a response");
        }
        else{
            return play.mvc.Action.badRequest("Object was not of correct class");
        }
    }    
    
    @Transactional
    public static Result sendResponseFromCreated(Object o) throws JAXBException{
        
        OadrCreatedEvent oCreatedEvent = (OadrCreatedEvent) o;
        oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID();

        persistFromCreatedEvent(oCreatedEvent);
        createNewEm();
        entityManager.persist(oCreatedEvent);
        entityManager.getTransaction().commit();
        
        EiResponse eiResponse = new EiResponse()        
            //TODO Need to handle non 200 responses
            .withResponseCode("200")
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
        eiResponse.setResponseCode("200");        
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
        }catch(NoResultException e){};
        response.withOadrEvent(new OadrEvent().withEiEvent(event));
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
        VENStatus venStatus = null;
        try{
            venStatus = (VENStatus)Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                    .getSingleResult();
        }catch(NoResultException e){};
        if(venStatus == null){
            venStatus = new VENStatus();
        }
        venStatus.setTime(new Date());
        venStatus.setVenID(requestEvent.getEiRequestEvent().getVenID());
        
        CustomerForm customer = null;
        EiEvent event = null;
        createNewEm();
        
        //TODO Change this to throw an exception then catch it and return a 500/400 error
        customer = (CustomerForm)entityManager.createQuery("SELECT c FROM Customers c WHERE c.venID = :ven")
                .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();
        
        List<VENStatus> statuses = entityManager.createQuery("SELECT v FROM StatusObject v WHERE v.venID = :ven")
            .setParameter("ven", customer.getVenID())
            .getResultList();
        
        if(statuses.size() == 0){    
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
    }

    @Transactional
    public static void persistFromCreatedEvent(OadrCreatedEvent createdEvent){
        VENStatus status = null;        
        try{
            status = (VENStatus)Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                    .getSingleResult();
        }catch(NoResultException e){};
        if(status != null){
            status.setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponse().get(0).getOptType().toString());
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);    
            entityManager.getTransaction().commit();
        }
    }
    
    public static void persistFromEiEvent(EiEvent eiEvent){        
        VENStatus status = null;
        try{
            status = (VENStatus)Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("SELECT status FROM StatusObject " +
                    "status WHERE status.venID = :ven")
                    .setParameter("ven", eiEvent.getEventDescriptor().getEventID())
                    .getSingleResult();
        }catch(NoResultException e){};
        if(status == null){
            status = new VENStatus();
            status.setTime(new Date());
            createNewEm();
            entityManager.merge(status);
        }
    }
    
    public static Object unmarshalRequest(byte[] chars) throws JAXBException{    
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
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
