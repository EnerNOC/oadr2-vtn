package controllers;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import models.StatusObject;
import models.CustomerForm;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.w3c.dom.Document;


import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import service.*;

public class Requests extends Controller{
	static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
	static EntityManager entityManager = entityManagerFactory.createEntityManager();
	
	@Inject static HttpService httpService;
	private static XmppService xmppService;
			
	@Transactional
	public static Result requests(String program) {
	    //List<ProgramForm> programs = entityManager.createQuery("FROM Program").getResultList();
	    List<EiEvent> programs = entityManager.createQuery("From EiEvent").getResultList();
		return ok(views.html.requests.render(program, programs));
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public static Result renderAjaxTable(String program){
	    List<StatusObject> listStatusObjects;
	    /*
        if(program != null){
    		listStatusObjects = entityManager.createQuery("SELECT status " +
    				"FROM StatusObject status WHERE status.program = :market")
    				.setParameter("market", program)
    		        .getResultList();
        }
        */        
	    if(program != null){
	        listStatusObjects = entityManager.createQuery("SELECT status " +
	                "FROM StatusObject status WHERE status.eventID = :event")
	                .setParameter("event", program)
	                .getResultList();
	    }
        else{
            listStatusObjects = entityManager.createQuery("FROM StatusObject").getResultList();
        }
	    
		class StatusObjectComparator implements Comparator<StatusObject>{
			public int compare(StatusObject statusOne, StatusObject statusTwo){
				return statusTwo.getTime().compareTo(statusOne.getTime());
			}
		}
		
		Collections.sort(listStatusObjects, new StatusObjectComparator());
		
		return ok(views.html.responseTable.render(listStatusObjects, program));
	}
	
	  @Transactional
	  //Deletes an event based on the id
	  public static Result deleteRequest(String program, Long id){
		  createNewEm();
		  entityManager.remove(entityManager.find(StatusObject.class, id));
		  entityManager.getTransaction().commit();
	      return redirect(routes.Requests.requests(program));
	  }
	
	@Transactional
	public static Result marshalRequest()  throws Exception{
	    return ok(httpService.test());
	    /*
		Document document = null;
		try {
			String xmlString = new String(request().body().asRaw().asBytes());
			if(!xmlString.equals("")){
				document = loadXmlFromString(new ByteArrayInputStream(request().body().asRaw().asBytes()));
			}
			else{
				return badRequest("XML is null");
			}
		} catch (Exception e) {
			return badRequest("Invalid xml\n");
		}
		
		if(document != null){
		    JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Object o = unmarshaller.unmarshal(document);
			
			if(o instanceof OadrRequestEvent){
			    //if http request
				return sendDistributeFromRequest(o);
				//else if xmpp request
			}
			//if its a created event send out a distribute event
			else if(o instanceof OadrCreatedEvent){
			    //if http request
			    return sendResponseFromCreated(o);
			    //else if xmpp request
			}
		}
		//throw an exception here!
		return badRequest();*/
	}
	
	public static Result sendResponseFromCreated(Object o) throws Exception{
        OadrCreatedEvent oCreatedEvent = (OadrCreatedEvent) o;
        oCreatedEvent.getEiCreatedEvent().getVenID();

        onCreatedEvent(oCreatedEvent);
        
        createNewEm();
        entityManager.persist(oCreatedEvent);
        entityManager.getTransaction().commit();
        
        //Be sure to set the response code correctly to 1-5 depending on how the event is handled, temp 200 for now
        EiResponse eiResponse = new EiResponse()
            .withRequestID(oCreatedEvent.getEiCreatedEvent().getEiResponse().getRequestID())
            .withResponseCode("200")
            .withResponseDescription("Optional description!"); 
        
        return ok(marshalObject(eiResponse));
	}
	
	public static Result sendDistributeFromRequest(Object o) throws Exception{
	    OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
        EiResponse eiResponse = new EiResponse();
        
        eiResponse.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
        eiResponse.setResponseCode("200");

        createNewEm();
        entityManager.persist(oRequestEvent);
        entityManager.getTransaction().commit();
        
        onRequestEvent(oRequestEvent);
        
        OadrDistributeEvent response = new OadrDistributeEvent().withEiResponse(eiResponse);
        //Need to find out how to get EiEvent(s) and add them to this Distribute Event
        //Possibly from the Market Context, but need XPath to access, or find through VEN and Customers
        
        //response.withOadrEvent(null);
        response.withRequestID(eiResponse.getRequestID());

        return ok(marshalObject(response));
	}
	
	public static String marshalObject(Object o) throws Exception{	
	    JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");    
	    Marshaller marshaller = jaxbContext.createMarshaller();      
        StringWriter sw = new StringWriter();
        marshaller.marshal(o, sw);
        response().setContentType("application/xml");
        //probably should not return a string and find a way to format xml
        return sw.toString() + '\n';
	}
	
	public static Document loadXmlFromString(ByteArrayInputStream xmlByteStream){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;
		try{
			builder = factory.newDocumentBuilder();			
			return builder.parse(xmlByteStream);
		}
		catch(Exception e){
			return null;
		}
	}
	
	public static void createNewEm(){
		entityManager = entityManagerFactory.createEntityManager();
		if(!entityManager.getTransaction().isActive()){
			entityManager.getTransaction().begin();
		}
	}
	
	@Transactional
	public static void onRequestEvent(OadrRequestEvent requestEvent){
		StatusObject statusObject = new StatusObject();
		statusObject.setTime(new Date());
		createNewEm();
		statusObject.setVenID(requestEvent.getEiRequestEvent().getVenID());
		
		CustomerForm customer = null;
		EiEvent event = null;
		
		try{
		customer = (CustomerForm)entityManager.createQuery("SELECT c FROM Customers c WHERE c.venID = :ven")
				.setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
				.getSingleResult();
		
	    statusObject.setProgram(customer.getProgramId());
		
		event = (EiEvent)entityManager.createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor$EiMarketContext " +
				"marketContext WHERE marketContext.marketContext = :market and event.hjid = marketContext.hjid")
				.setParameter("market", statusObject.getProgram())
				.getSingleResult();
		}catch(Exception e){}
				
		if(customer != null && event != null){  
            statusObject.setEventID(event.getEventDescriptor().getEventID());
            
			entityManager.persist(statusObject);
			entityManager.getTransaction().commit();
		}
	}

	@Transactional
	public static void onCreatedEvent(OadrCreatedEvent createdEvent){
		createNewEm();
		StatusObject status = null;
		try{
		status = (StatusObject)entityManager.createQuery("SELECT status FROM StatusObject " +
				"status WHERE status.venID = :ven")
				.setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
				.getSingleResult();
		}catch(Exception e){}
		if(status != null){
			status.setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponse().get(0).getOptType().toString());
			status.setTime(new Date());
			entityManager.merge(status);
			entityManager.getTransaction().commit();			
		}
	}	
}
