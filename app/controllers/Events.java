package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import models.Event;
import models.Program;
import models.VEN;
import models.VENStatus;

import org.enernoc.open.oadr2.model.DateTime;
import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EventDescriptor.EiMarketContext;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.enernoc.open.oadr2.model.MarketContext;

import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import service.PushService;
import service.XmppService;

public class Events extends Controller {
      
      @Inject static XmppService xmppService;
      @Inject static PushService pushService;
    
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
    			  return eventOne.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().compare(
    			          eventTwo.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue());
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
    		  String contextName = JPA.em().find(Program.class, Long.parseLong(newEventForm.getMarketContext())).getProgramName();
    		  newEvent.getEventDescriptor().setEiMarketContext(new EiMarketContext(new MarketContext(contextName)));
    		  newEvent.getEiActivePeriod().getProperties().getDtstart().getDateTime().setValue(newEvent.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().normalize());
    		  JPA.em().persist(newEvent);	      		  
    		  flash("success", "Event as been created");
    		  List<VEN> vens = getVENs(newEvent);
    		  populateFromPush(newEvent);
    		  pushService.pushNewEvent(newEvent, vens);
    		  return redirect(routes.VENStatuses.venStatuses(newEvent.getEventDescriptor().getEventID()));
    	  }
      }
      
      @Transactional
      //Deletes an event based on the id
      public static Result deleteEvent(Long id){
    	  JPA.em().remove(JPA.em().find(EiEvent.class, id));
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
          EiEvent event = JPA.em().find(EiEvent.class, id);
          event = eiEventForm.getEiEvent();
          JPA.em().merge(event);
          flash("success", "Event has been updated");
    	  return redirect(routes.Events.events());
      }
      
      @Transactional
      public static Result editEvent(Long id){
    	  Event form = new Event(JPA.em().find(EiEvent.class, id));
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
          DatatypeFactory df = null;
          try {
              df = DatatypeFactory.newInstance();
          } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
          }
          
          Date currentDate = new Date();          
          GregorianCalendar calendar = new GregorianCalendar();
          calendar.setTime(currentDate);
          XMLGregorianCalendar xCalendar = df.newXMLGregorianCalendar(calendar);
          xCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
          
          DateTime currentTime = new DateTime().withValue(xCalendar);
          DateTime startTime = new DateTime().withValue(event.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().normalize());
          DateTime endTime = new DateTime().withValue(event.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().normalize());
          
          Duration d = df.newDuration(Event.minutesFromXCal(event.getEiActivePeriod().getProperties().getDuration().getDuration().getValue()) * 60000);

          endTime.getValue().add(d);
                  
          if(currentTime.getValue().compare(startTime.getValue()) == -1){
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.FAR);
          }
          else if(currentTime.getValue().compare(startTime.getValue()) > 0 && currentTime.getValue().compare(endTime.getValue()) == -1){
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.ACTIVE);              
          }
          else if(currentTime.getValue().compare(endTime.getValue()) > 0){
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.COMPLETED);              
          }
          else{
              event.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.NONE);              
          }
      }
      
      @SuppressWarnings("unchecked")
      @Transactional
      public static void populateFromPush(EiEvent e){
          List<VEN> customers = getVENs(e);
          prepareVENs(customers, e);
      }
     
      @SuppressWarnings("unchecked")
      @Transactional
      public static Map<String, String> makeProgramMap(){
          List<Program> programList = Persistence.createEntityManagerFactory("Events").createEntityManager().createQuery("FROM Program").getResultList();
          Map<String, String> programMap = new HashMap<String, String>();
    	  for(Program program : programList){
    		  programMap.put(program.getId() + "", program.getProgramName());
    	  }
    	  return programMap;
      }
      
      @SuppressWarnings("unchecked")
      public static List<VEN> getVENs(EiEvent e){
          return JPA.em().createQuery("SELECT c from VEN c WHERE c.programId = :program and c.clientURI != ''")
                  .setParameter("program", e.getEventDescriptor().getEiMarketContext().getMarketContext().getValue())
                  .getResultList();
      }
      
      public static void prepareVENs(List<VEN> vens, EiEvent e){
          for(VEN v : vens){
              VENStatus venStatus = new VENStatus();
              venStatus.setOptStatus("Pending 1");
              venStatus.setRequestID(v.getClientURI());
              venStatus.setEventID(e.getEventDescriptor().getEventID());
              venStatus.setProgram(v.getProgramId());
              venStatus.setVenID(v.getVenID());
              venStatus.setTime(new Date());
              JPA.em().persist(venStatus);              
          }    
      }
      
}