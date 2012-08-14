package controllers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import models.EiEventForm;
import models.ProgramForm;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiEvent.EventDescriptor.EiMarketContext;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;
import org.joda.time.DateTime;

import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

/*
 * Known error, trying to update an event and then inputting an incorrect
 * value to any field defaults the Start and End Time to current time.
 */

public class Events extends Controller {
  static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
  static EntityManager entityManager = entityManagerFactory.createEntityManager();
  
  //redirects to the events page
  public static Result index() {
	  return redirect(routes.Events.events());
  }
  
  // requests page, displays all events currently in the database
  @SuppressWarnings("unchecked")
  @Transactional
  public static Result events(){

	  class EiEventComparator implements Comparator<EiEvent>{
		  public int compare(EiEvent eventOne, EiEvent eventTwo){
			  return eventOne.getEiActivePeriod().getProperties().getDtstart().getDateTimeItem().compareTo(
					  eventTwo.getEiActivePeriod().getProperties().getDtstart().getDateTimeItem());
		  }
	  } 
	  List<EiEvent> eiEvents = JPA.em().createQuery("FROM EiEvent").getResultList();
	  Collections.sort(eiEvents, new EiEventComparator());
	  for(EiEvent e : eiEvents){
	      updateStatus(e);
	  }
	  
	  return ok(views.html.events.render(eiEvents, new EiEventForm()));
  }
  
public static Result blankEvent(){
	  EiEventForm newForm = new EiEventForm();
	  return ok(views.html.createEvent.render(form(EiEventForm.class).fill(newForm), newForm, makeProgramMap()));
  }
  
  @Transactional
  //Method to create a new event once on the newEvent page
  public static Result newEvent(){
	  Form<EiEventForm> filledForm = form(EiEventForm.class).bindFromRequest();
      if(filledForm.hasErrors()) {
    	  addFlashError(filledForm.errors());
          return badRequest(views.html.createEvent.render(filledForm, new EiEventForm(), makeProgramMap()));
      }
	  else{		  
		  EiEventForm newEventForm = filledForm.get();
		  EiEvent newEvent = newEventForm.toEiEvent();
		  String contextName = JPA.em().find(ProgramForm.class, Long.parseLong(newEventForm.marketContext)).getProgramName();
		  newEvent.getEventDescriptor().setEiMarketContext(new EiMarketContext(contextName));
		  JPA.em().persist(newEvent);
		  flash("success", "Event as been created");		
		  /*
		  ProgramEventRelation newRelation = new ProgramEventRelation(newEvent.getHjid(), 
				  Integer.parseInt(newEventForm.marketContext));
		  createNewEm();
		  JPA.em().persist(newRelation);
		  JPA.em().getTransaction().commit();
		  */
		  return redirect(routes.Events.newEvent());		  
	  }
  }
  
  @Transactional
  //Deletes an event based on the id
  public static Result deleteEvent(Long id){
	  JPA.em().remove(JPA.em().find(EiEvent.class, id));
	  //JPA.em().remove(JPA.em().find(ProgramEventRelation.class, getRelationFromEvent(id).getId()));
      flash("success", "Event has been deleted");
      return redirect(routes.Events.events());
  }
  
  @Transactional
  public static Result updateEvent(Long id){
	  Form<EiEventForm> eventForm = form(EiEventForm.class).bindFromRequest();
      if(eventForm.hasErrors()) {
          return badRequest(views.html.editEvent.render(id, eventForm, new EiEventForm(), makeProgramMap()));
      }
      EiEventForm eiEventForm= eventForm.get();
      eiEventForm.copyEvent(JPA.em().find(EiEvent.class, id));
      EiEvent event = JPA.em().find(EiEvent.class, id); //is actually used, eclipse is a liar
      event = eiEventForm.getEiEvent();
      JPA.em().merge(event);
      flash("success", "Event has been updated");
	  return redirect(routes.Events.events());
  }
  
  @Transactional
  public static Result editEvent(Long id){
	  EiEventForm form = new EiEventForm(JPA.em().find(EiEvent.class, id));
	  //form.marketContext = getRelationFromEvent(id).getProgramId() + "";
	  return ok(views.html.editEvent.render(id, form(EiEventForm.class).fill(form), form, makeProgramMap()));
  }

  //Takes the error Map with a string as a key and adds
  //the key and value to the flash() scope to be accessed
  public static void addFlashError(Map<String, List<ValidationError>> errors){
	  for(String key : errors.keySet()){
		  List<ValidationError> currentError = errors.get(key);
		  for(ValidationError error : currentError){
			  flash(key, error.message());
		  }
	  }	  
  }
  
  @Transactional
  public static void updateStatus(EiEvent event){
      DateTime currentTime = new DateTime();
      DateTime startTime = new DateTime(event.getEiActivePeriod().getProperties().getDtstart().getDateTime().toString());
      long endMillis = currentTime.getMillis() + EiEventForm.minutesFromXCal(event.getEiActivePeriod().getProperties().getDuration().getDuration());
      if(currentTime.getMillis() < startTime.getMillis()){
          event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.FAR);
      }
      else if(currentTime.getMillis() >= startTime.getMillis() && currentTime.getMillis() < endMillis){
          event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.ACTIVE);
      }
      else if(currentTime.getMillis() >= endMillis){
          event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.COMPLETED);
      }
  }
 
  @SuppressWarnings("unchecked")
  @Transactional
  public static Map<String, String> makeProgramMap(){
      //JPA.em() doesn't work...
      List<ProgramForm> programList = Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("FROM Program").getResultList();
      Map<String, String> programMap = new HashMap<String, String>();
	  for(ProgramForm program : programList){
		  programMap.put(program.getId() + "", program.getProgramName());
	  }
	  return programMap;
  }
  
}