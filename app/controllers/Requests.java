package controllers;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import models.ProjectEventRelation;
import models.ProjectForm;
import models.StatusObject;
import models.UserForm;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.libs.Comet;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.XPath;
import play.Logger;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class Requests extends Controller{
	static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
	static EntityManager entityManager = entityManagerFactory.createEntityManager();
	
	public static Result requests() {
		return ok(views.html.requests.render());
	}
	
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public static Result displayPage(){
		List<StatusObject> listStatusObjects = entityManager.createQuery("FROM StatusObject").getResultList();
		
		class StatusObjectComparator implements Comparator<StatusObject>{
			public int compare(StatusObject statusOne, StatusObject statusTwo){
				return statusTwo.getTime().compareTo(statusOne.getTime());
			}
		}
		
		Collections.sort(listStatusObjects, new StatusObjectComparator());
		
		return ok(views.html.responseTable.render(listStatusObjects));
	}
	
	  @Transactional
	  //Deletes an event based on the id
	  public static Result deleteRequest(Long id){
		  createNewEm();
		  entityManager.remove(entityManager.find(StatusObject.class, id));
		  entityManager.getTransaction().commit();
	      return redirect(routes.Requests.requests());
	  }
	
	@Transactional
	//need to break apart, too big, each instanceof separate method
	public static Result marshalRequest() throws JAXBException{
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
			Marshaller marshaller = jaxbContext.createMarshaller();
			Object o = unmarshaller.unmarshal(document);
				
			//if RequestEvent send out a response!
			if(o instanceof OadrRequestEvent){						
				OadrRequestEvent oRequestEvent = (OadrRequestEvent) o;
				EiResponse eiResponse = new EiResponse();
				/*
				 * 1xx Informational - Request received, continuing process
				 * 2xx Success - The request was successfully received, understood, and accepted
				 * 3xx Pending - Further action must be taken in order to complete the request
				 * 4xx Requester Error - The request contains bad syntax or cannot be fulfilled
				 * 5xx Responder Error - The responder failed to fulfill an apparently valid request
				 */
				eiResponse.setRequestID(oRequestEvent.getEiRequestEvent().getRequestID());
				eiResponse.setResponseCode("200");
				eiResponse.setResponseDescription("Optional! But I'm here <3");

				createNewEm();
				entityManager.persist(oRequestEvent);
				entityManager.getTransaction().commit();
				
				onRequestEvent(oRequestEvent);
				
				OadrDistributeEvent response = new OadrDistributeEvent().withEiResponse(eiResponse);
				//Need to find out how to get EiEvent(s) and add them to this Distribute Event
				//Possibly from the Market Context, but need XPath to access, or find through VEN and Users
				//response.withOadrEvent(null);
				response.withRequestID(eiResponse.getRequestID());
				
				StringWriter sw = new StringWriter();
				marshaller.marshal(response, sw);
				response().setContentType("application/xml");
				//probably should not return a string and find a way to format xml
				return ok(sw.toString() + '\n');
			}
			//if its a created event send out a distribute event
			else if(o instanceof OadrCreatedEvent){
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
				
				StringWriter sw = new StringWriter();
				marshaller.marshal(new OadrResponse(eiResponse), sw);
				response().setContentType("application/xml");
				return ok(sw.toString() + '\n');
			}
			/*
			//occurs after sending out a DistributeEvent
			else if(o instanceof OadrResponse){
				OadrResponse oResponse = (OadrResponse) o;
				
				createNewEm();
				entityManager.persist(oResponse);
				entityManager.getTransaction().commit();
			}
			*/
		}
		return redirect(routes.Requests.requests());
	}
	
	/*
	 * seems insanely fking stupid to have to parse an application/xml as bytes then go through the doc builder
	 * in order to get a Document, would MUCH prefer to use .getXml() but also stupid and returns null 
	 * as only works for text/xml
	 */
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
	//hackish way to do this, i'm too stupid to figure out how to add a date
	//and use the queries properly
	public static void onRequestEvent(OadrRequestEvent requestEvent){
		StatusObject statusObject = new StatusObject();
		statusObject.setTime(new Date());
		createNewEm();
		statusObject.setVenID(requestEvent.getEiRequestEvent().getVenID());
		
		List<UserForm> users = entityManager.createQuery("SELECT u FROM Users u WHERE u.venID = :ven")
				.setParameter("ven", requestEvent.getEiRequestEvent().getVenID())
				.getResultList();
		
		if(users.size() > 0){
			ProjectForm p = entityManager.find(ProjectForm.class, Long.parseLong(users.get(0).getProjectId()));
			statusObject.setProgram(p.getProjectName());
		}
		List<EiEvent> events = entityManager.createQuery("SELECT event FROM EiEvent event, EiEvent$EventDescriptor$EiMarketContext " +
				"marketContext WHERE marketContext.marketContext = :market and event.hjid=marketContext.hjid")
				.setParameter("market", statusObject.getProgram())
				.getResultList();
		if(events.size() > 0){
			statusObject.setEventID(events.get(0).getEventDescriptor().getEventID());
		}
		
		if(users.size() == 1 && events.size() == 1){
			entityManager.persist(statusObject);
			entityManager.getTransaction().commit();
		}
	}

	@Transactional
	public static void onCreatedEvent(OadrCreatedEvent createdEvent){
		createNewEm();
		List<StatusObject> statusList = entityManager.createQuery("SELECT status FROM StatusObject status WHERE status.venID = :ven")
		.setParameter("ven", createdEvent.getEiCreatedEvent().getVenID())
		.getResultList();
		if(statusList.size() > 0){
			statusList.get(0).setOptStatus(createdEvent.getEiCreatedEvent().getEventResponses().getEventResponse().get(0).getOptType().toString());
			statusList.get(0).setTime(new Date());
			entityManager.merge(statusList.get(0));
			entityManager.getTransaction().commit();			
		}
	}	
}
