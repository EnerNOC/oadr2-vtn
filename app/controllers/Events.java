package controllers;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;

import models.VEN;
import models.Event;
import models.Program;
import models.VENStatus;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiEvent.EventDescriptor.EiMarketContext;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.joda.time.DateTime;

import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import service.EiEventService;
import service.XmppService;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1
//grep -r "" ./

public class Events extends Controller {
    //
    @Inject static XmppService xmppService;
    
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
    	  
    	  return ok(views.html.events.render(eiEvents, new Event()));
      }
      
    public static Result blankEvent(){
    	  Event newForm = new Event();
    	  return ok(views.html.newEvent.render(form(Event.class).fill(newForm), newForm, makeProgramMap()));
      }
      
      @Transactional
      //Method to create a new event once on the newEvent page
      public static Result newEvent() throws JAXBException{
    	  Form<Event> filledForm = form(Event.class).bindFromRequest();
          if(filledForm.hasErrors()) {
        	  addFlashError(filledForm.errors());
              return badRequest(views.html.newEvent.render(filledForm, new Event(), makeProgramMap()));
          }
    	  else{		  
    		  Event newEventForm = filledForm.get();
    		  EiEvent newEvent = newEventForm.toEiEvent();
    		  String contextName = JPA.em().find(Program.class, Long.parseLong(newEventForm.marketContext)).getProgramName();
    		  newEvent.getEventDescriptor().setEiMarketContext(new EiMarketContext(contextName));
    		  JPA.em().persist(newEvent);	  
    		  //EiEventService.persistFromEiEvent(newEvent);
    		  
    		  flash("success", "Event as been created");

    		  populateFromPush(newEvent);
    		  
    		  XmppService.populateThreadPool(newEvent);
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
    	  Form<Event> eventForm = form(Event.class).bindFromRequest();
          if(eventForm.hasErrors()) {
              return badRequest(views.html.editEvent.render(id, eventForm, new Event(), makeProgramMap()));
          }
          Event eiEventForm= eventForm.get();
          eiEventForm.copyEvent(JPA.em().find(EiEvent.class, id));
          EiEvent event = JPA.em().find(EiEvent.class, id); //is actually used, eclipse is a liar
          event = eiEventForm.getEiEvent();
          JPA.em().merge(event);
          flash("success", "Event has been updated");
    	  return redirect(routes.Events.events());
      }
      
      @Transactional
      public static Result editEvent(Long id){
    	  Event form = new Event(JPA.em().find(EiEvent.class, id));
    	  //form.marketContext = getRelationFromEvent(id).getProgramId() + "";
    	  return ok(views.html.editEvent.render(id, form(Event.class).fill(form), form, makeProgramMap()));
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
          long endMillis = currentTime.getMillis() + Event.minutesFromXCal(event.getEiActivePeriod().getProperties().getDuration().getDuration());
          if(currentTime.getMillis() < startTime.getMillis()){
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.FAR);
          }
          else if(currentTime.getMillis() >= startTime.getMillis() && currentTime.getMillis() < endMillis){
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.ACTIVE);
          }
          else if(currentTime.getMillis() >= endMillis){
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.COMPLETED);
          }
          else{
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.NONE);
          }
      }
      
      @SuppressWarnings("unchecked")
      @Transactional
      public static void populateFromPush(EiEvent e){
          List<VEN> customers = JPA.em().createQuery("SELECT c from Customers c WHERE c.programId = :program and c.clientURI != ''")
                  .setParameter("program", e.getEventDescriptor().getEiMarketContext().getMarketContext())
                  .getResultList();
          for(VEN c : customers){
              VENStatus v = new VENStatus();
              v.setOptStatus("Pending 1");
              //TODO Need to make the Request ID a UNIQUE Alpha Numeric string! Ask Brian/Thom if that is correct
              v.setRequestID(e.getEventDescriptor().getEventID());
              v.setEventID(e.getEventDescriptor().getEventID());
              v.setProgram(c.getProgramId());
              v.setVenID(c.getVenID());
              v.setTime(new Date());
              JPA.em().persist(v);              
          }          
      }
     
      @SuppressWarnings("unchecked")
      @Transactional
      public static Map<String, String> makeProgramMap(){
          //JPA.em() doesn't work...
          List<Program> programList = Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("FROM Program").getResultList();
          Map<String, String> programMap = new HashMap<String, String>();
    	  for(Program program : programList){
    		  programMap.put(program.getId() + "", program.getProgramName());
    	  }
    	  return programMap;
      }
      
}