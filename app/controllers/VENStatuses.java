package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;
import com.google.inject.Inject;
import models.VENStatus;
import org.enernoc.open.oadr2.model.EiEvent;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import service.*;
import service.oadr.EiEventService;

public class VENStatuses extends Controller{
    
    static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
    static EntityManager entityManager = entityManagerFactory.createEntityManager();
	
	@Inject static EiEventService eiEventService;
	@Inject static XmppService xmppService;
			
	@SuppressWarnings("unchecked")
    @Transactional
	public static Result venStatuses(String program) {
	    List<EiEvent> programs = JPA.em().createQuery("From EiEvent").getResultList();
		return ok(views.html.venStatuses.render(program, programs));
	}
	
	@SuppressWarnings({ "unchecked" })
    @Transactional
	public static Result renderAjaxTable(String program){
	    List<VENStatus> listStatusObjects;
	    if(program != null){
	        listStatusObjects = JPA.em().createQuery("SELECT status " +
	                "FROM VENStatus status WHERE status.eventID = :event")
	                .setParameter("event", program)
	                .getResultList();
	    }
        else{
            listStatusObjects = JPA.em().createQuery("FROM VENStatus").getResultList();
        }
	    
		class StatusObjectComparator implements Comparator<VENStatus>{
			public int compare(VENStatus statusOne, VENStatus statusTwo){
				return statusTwo.getTime().compareTo(statusOne.getTime());
			}
		}				
		Collections.sort(listStatusObjects, new StatusObjectComparator());		
		return ok(views.html.venStatusTable.render(listStatusObjects, program));
	}
	
    @Transactional
	public static Result sendXMPPCreated(String event){
        return redirect(routes.VENStatuses.venStatuses(event));
	}
	
	
	@Transactional
	//Deletes an event based on the id
	public static Result deleteStatus(String program, Long id){
	    JPA.em().remove(JPA.em().find(VENStatus.class, id));
	    return redirect(routes.VENStatuses.venStatuses(program));
	}
}
