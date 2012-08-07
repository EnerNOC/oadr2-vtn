package service;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import models.CustomerForm;
import models.VENStatus;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
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
    
    public Result sendMarshalledObject(Object o) throws JAXBException{
        if(o instanceof OadrRequestEvent){
            return sendDistributeFromRequest(o);
        }
        //if its a created event send out a distribute event
        else if(o instanceof OadrCreatedEvent){
            return sendResponseFromCreated(o);
        }
        return play.mvc.Action.badRequest(o.getClass().getCanonicalName() + " is not an acceptable incoming payload.");
    }    
    
    @Transactional
    public Result sendResponseFromCreated(Object o) throws JAXBException{
        
        OadrCreatedEvent oCreatedEvent = (OadrCreatedEvent) o;
        oCreatedEvent.getEiCreatedEvent().getVenID();

        onCreatedEvent(oCreatedEvent);
        
        JPA.em().persist(oCreatedEvent);
        
        EiResponse eiResponse = new EiResponse()
            .withRequestID(oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID())
            //TODO Need to handle non 200 responses
            .withResponseCode("200")
            .withResponseDescription("Optional description!"); 
        
        return play.mvc.Action.ok(marshalObject(eiResponse));
    }
    
    @Transactional
    public Result sendDistributeFromRequest(Object o) throws JAXBException{
        
        OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
        EiResponse eiResponse = new EiResponse();
        
        eiResponse.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
        //TODO Need to handle non 200 responses
        eiResponse.setResponseCode("200");

        JPA.em().persist(oRequestEvent);
        
        onRequestEvent(oRequestEvent);
        
        OadrDistributeEvent response = new OadrDistributeEvent().withEiResponse(eiResponse);
        //Need to find out how to get EiEvent(s) and add them to this Distribute Event
        //Possibly from the Market Context, but need XPath to access, or find through VEN and Customers
        
        //response.withOadrEvent(null);
        response.withRequestID(eiResponse.getRequestID());

        return play.mvc.Action.ok(marshalObject(response));
    }
    
    public String marshalObject(Object o) throws JAXBException{  
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");    
        Marshaller marshaller = jaxbContext.createMarshaller();      
        StringWriter sw = new StringWriter();
        marshaller.marshal(o, sw);
        play.mvc.Controller.response().setContentType("application/xml");
        return sw.toString() + '\n';
    }
        
    @SuppressWarnings("unchecked")
    @Transactional
    public void onRequestEvent(OadrRequestEvent requestEvent){
        
        VENStatus venStatus = new VENStatus();
        venStatus.setTime(new Date());
        venStatus.setVenID(requestEvent.getEiRequestEvent().getVenID());
        
        CustomerForm customer = null;
        EiEvent event = null;
        
        customer = (CustomerForm)JPA.em().createQuery("SELECT c FROM Customers c WHERE c.venID = :ven")
                .setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
                .getSingleResult();
        
        
        List<VENStatus> statuses = JPA.em().createQuery("SELECT v FROM StatusObject v WHERE v.venID = :ven")
            .setParameter("ven", customer.getVenID())
            .getResultList();
        
        if(statuses.size() == 0){        
            venStatus.setProgram(customer.getProgramId());
            
            event = (EiEvent)JPA.em().createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor$EiMarketContext " +
                    "marketContext WHERE marketContext.marketContext = :market and event.hjid = marketContext.hjid")
                    .setParameter("market", venStatus.getProgram())
                    .getSingleResult();
                    
            if(customer != null && event != null){  
                venStatus.setEventID(event.getEventDescriptor().getEventID());
                JPA.em().persist(venStatus);
            }
        }
    }

    @Transactional
    public void onCreatedEvent(OadrCreatedEvent createdEvent){
        VENStatus status = null;
        status = (VENStatus)JPA.em().createQuery("SELECT status FROM StatusObject " +
                "status WHERE status.venID = :ven")
                .setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
                .getSingleResult();
        if(status != null){
            status.setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponse().get(0).getOptType().toString());
            status.setTime(new Date());
            JPA.em().merge(status);         
        }
    }
    
    public static Object unmarshalRequest()  throws Exception{    
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object o = unmarshaller.unmarshal(new ByteArrayInputStream(play.mvc.Controller.request().body().asRaw().asBytes()));
        return o;
    }
    
}
