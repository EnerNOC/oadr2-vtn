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
import javax.xml.bind.Unmarshaller;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import models.VENStatus;
import models.CustomerForm;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiResponse;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.w3c.dom.Document;


import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import service.*;

public class OadrEvents extends Controller{
    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();
	
	@Inject static EiEventService httpService;
	@Inject static XmppService xmppService;
			
	@SuppressWarnings("unchecked")
    @Transactional
	public static Result requests(String program) {
	    //List<ProgramForm> programs = JPA.em().createQuery("FROM Program").getResultList();
	    List<EiEvent> programs = JPA.em().createQuery("From EiEvent").getResultList();
		return ok(views.html.requests.render(program, programs));
	}
	
	@SuppressWarnings({ "unchecked" })
    @Transactional
	public static Result renderAjaxTable(String program){
	    List<VENStatus> listStatusObjects;
	    if(program != null){
	        listStatusObjects = JPA.em().createQuery("SELECT status " +
	                "FROM StatusObject status WHERE status.eventID = :event")
	                .setParameter("event", program)
	                .getResultList();
	    }
        else{
            listStatusObjects = JPA.em().createQuery("FROM StatusObject").getResultList();
        }
	    
		class StatusObjectComparator implements Comparator<VENStatus>{
			public int compare(VENStatus statusOne, VENStatus statusTwo){
				return statusTwo.getTime().compareTo(statusOne.getTime());
			}
		}
		
		Collections.sort(listStatusObjects, new StatusObjectComparator());
		
		return ok(views.html.responseTable.render(listStatusObjects, program));
	}
	
	  @Transactional
	  //Deletes an event based on the id
	  public static Result deleteRequest(String program, Long id){
		  JPA.em().remove(JPA.em().find(VENStatus.class, id));
	      return redirect(routes.OadrEvents.requests(program));
	  }
	
	  @Transactional
	  public static Result sendHttpResponse()  throws Exception{
	      return(httpService.sendMarshalledObject(unmarshalRequest()));
	  }
	  	  
	  /* *************************************
	   * Parsing of the XML from HTTP response
	   * ************************************* */	 
	  
    public static Object unmarshalRequest()  throws Exception{    
        JAXBContext jaxbContext = JAXBContext.newInstance("org.enernoc.open.oadr2.model");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object o = unmarshaller.unmarshal(new ByteArrayInputStream(play.mvc.Controller.request().body().asRaw().asBytes()));
        return o;
    }
	
}
