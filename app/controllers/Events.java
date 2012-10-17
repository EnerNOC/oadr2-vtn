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
import javax.xml.bind.JAXBElement;
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

import org.enernoc.open.oadr2.model.CurrentValue;
import org.enernoc.open.oadr2.model.DateTime;
import org.enernoc.open.oadr2.model.Dtstart;
import org.enernoc.open.oadr2.model.DurationPropType;
import org.enernoc.open.oadr2.model.DurationValue;
import org.enernoc.open.oadr2.model.EiActivePeriod;
import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiEventSignal;
import org.enernoc.open.oadr2.model.EiEventSignals;
import org.enernoc.open.oadr2.model.EiTarget;
import org.enernoc.open.oadr2.model.EventDescriptor;
import org.enernoc.open.oadr2.model.EventDescriptor.EiMarketContext;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.enernoc.open.oadr2.model.Interval;
import org.enernoc.open.oadr2.model.Intervals;
import org.enernoc.open.oadr2.model.MarketContext;
import org.enernoc.open.oadr2.model.ObjectFactory;
import org.enernoc.open.oadr2.model.PayloadFloat;
import org.enernoc.open.oadr2.model.Properties;
import org.enernoc.open.oadr2.model.Properties.Tolerance;
import org.enernoc.open.oadr2.model.Properties.Tolerance.Tolerate;
import org.enernoc.open.oadr2.model.SignalPayload;
import org.enernoc.open.oadr2.model.SignalTypeEnumeratedType;
import org.enernoc.open.oadr2.model.Uid;

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
      static ObjectFactory objectFactory = new ObjectFactory();
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
    	      e.getEventDescriptor().setEventStatus(updateStatus(e));
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
          
    	  Form<Event> filledForm = form(Event.class).bindFromRequest();
          if(filledForm.hasErrors()) {
        	  addFlashError(filledForm.errors());
              return badRequest(views.html.newEvent.render(filledForm, new Event(), makeProgramMap()));
          }
    	  else{		  
    	      
              JAXBElement<SignalPayload> signalPayload = objectFactory.createSignalPayload(new SignalPayload(new PayloadFloat(1)));
              
    		  Event newEventForm = filledForm.get();
              String contextName = JPA.em().find(Program.class, Long.parseLong(newEventForm.getMarketContext())).getProgramName();
    		  EiEvent newEvent = newEventForm.toEiEvent();
    		  newEvent
    		      .withEiActivePeriod(new EiActivePeriod()
    		          .withProperties(new Properties()
    	                  .withDtstart(new Dtstart()
    	                      .withDateTime(new DateTime()
                                  .withValue(newEvent.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().normalize())))
                          .withDuration(new DurationPropType()
                              .withDuration(new DurationValue()
                                  .withValue(formatDuration(getDuration(newEvent)))))
                          .withTolerance(new Tolerance()
                              .withTolerate(new Tolerate()
                                  .withStartafter(new DurationValue()
                                      .withValue(formatDuration(getDuration(newEvent))))))
                          .withXEiNotification(new DurationPropType()
                              .withDuration(new DurationValue()
                                  .withValue(formatDuration(getDuration(newEvent)))))
                          .withXEiRampUp(new DurationPropType()
                              .withDuration(new DurationValue()
                                  .withValue(formatDuration(getDuration(newEvent)))))
                          .withXEiRecovery(new DurationPropType()
                              .withDuration(new DurationValue()
                                  .withValue(formatDuration(getDuration(newEvent)))))))
                  .withEiEventSignals(new EiEventSignals()
                          .withEiEventSignals(new EiEventSignal()
                                  .withCurrentValue(new CurrentValue()
                                          .withPayloadFloat(new PayloadFloat()
                                                  .withValue(0))) //TODO Not sure what this value is supposed to be, must be 0 when NEAR
                                  .withIntervals(new Intervals()
                                      .withIntervals(new Interval()
                                          .withDuration(new DurationPropType()
                                                  .withDuration(new DurationValue()
                                                          .withValue(formatDuration(getDuration(newEvent)))))
                                          .withUid(new Uid()
                                                  .withText("0"))
                                                  .withStreamPayloadBase(signalPayload)))
                                  .withSignalID("TH_SIGNAL_ID")
                                  .withSignalName("simple")
                                  .withSignalType(SignalTypeEnumeratedType.LEVEL)))
                  .withEiTarget(new EiTarget())
                  .withEventDescriptor(new EventDescriptor()
                          .withCreatedDateTime(new DateTime().withValue(xCalendar))
                          .withEiMarketContext(new EiMarketContext()
                                  .withMarketContext(new MarketContext()
                                          .withValue(contextName)))
                          .withEventID(newEventForm.getEventID())
                          .withEventStatus(updateStatus(newEvent))//TODO Probably doesn't need to be set to Far automagically
                          .withModificationNumber(0)
                          .withPriority(newEventForm.getPriority())
                          .withTestEvent("False")
                          .withVtnComment("No VTN Comment"));    		      		  
    		  
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
      public static EventStatusEnumeratedType updateStatus(EiEvent event){
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
          
          DateTime rampUpTime = new DateTime().withValue(event.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValue().normalize());
          rampUpTime.getValue().add(getDuration(event.getEiActivePeriod().getProperties().getXEiRampUp().getDuration().getValue()));
          Duration d = getDuration(event);
          endTime.getValue().add(d);
                  
          if(currentTime.getValue().compare(startTime.getValue()) == -1){
              if(true){
                  return EventStatusEnumeratedType.NEAR;
              }
              else{
                  return EventStatusEnumeratedType.FAR;                 
              }
          }
          else if(currentTime.getValue().compare(startTime.getValue()) > 0 && currentTime.getValue().compare(endTime.getValue()) == -1){
              return EventStatusEnumeratedType.ACTIVE;        
          }
          else if(currentTime.getValue().compare(endTime.getValue()) > 0){
              return EventStatusEnumeratedType.COMPLETED;
          }
          else{
              return EventStatusEnumeratedType.NONE;
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
      
      public static Duration getDuration(EiEvent event){
          DatatypeFactory df = null;
          try {
              df = DatatypeFactory.newInstance();
          } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
          }
          return df.newDuration(Event.minutesFromXCal(event.getEiActivePeriod().getProperties().getDuration().getDuration().getValue()) * 60000);

      }
      
      public static Duration getDuration(String duration){
          DatatypeFactory df = null;
          try {
              df = DatatypeFactory.newInstance();
          } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
          }
          return df.newDuration(duration);
      }
      
      public static String formatDuration(Duration duration){
          return duration.toString().replaceAll(".000", "");
      }
      
}