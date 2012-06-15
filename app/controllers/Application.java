package controllers;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import models.EiEventForm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.internal.util.xml.Origin; //insanely necessary, DO NOT REMOVE
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.HibernatePersistence;

import org.enernoc.open.oadr2.model.EiEvent.EiActivePeriod;
import org.enernoc.open.oadr2.model.EiEvent;

import play.Logger;
import play.data.*;
import play.db.DB;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.*;

public class Application extends Controller {
  static Form<EiEventForm> eventForm = form(EiEventForm.class);
  
  public static Result index() {
	  return redirect(routes.Application.events());
  }
  
  // requests page, creates an empty list and redirects to the same page
  public static Result events(){
	  List<EiEvent> emptyList = new ArrayList<EiEvent>();
	  return ok(views.html.index.render(eventForm)); //not actually an error
  }
  
  //@PersistenceContext
  @Transactional
  public static Result newEvent(){
	  Form<EiEventForm> filledForm = eventForm.bindFromRequest();
	  if(filledForm.hasErrors()){
		  return badRequest(views.html.index.render(filledForm)); //not an error
	  }
	  else{	  
		  EiEventForm newEventForm = filledForm.get();
		  EiEvent newEvent = newEventForm.toEiEvent();
		  EntityManager em = Persistence.createEntityManagerFactory("testUnit").createEntityManager();
		  //newEvent.setHjid(new Long(1));
		  em.persist(newEvent);
		  em.close();

		  Logger.info(newEvent.toString());
		  
		  em = Persistence.createEntityManagerFactory("testUnit").createEntityManager();
		  Logger.info(em.find(EiEvent.class, new Long(1)).toString());
		  return redirect(routes.Application.events());
	  }
  }
  
  public static Result deleteEvent(String eventID){
	  return TODO;
  }
  
  /*//custom builder for an EiEvent from the form parsed array, not a permanent solution
  public static EiEvent eventFromStringArray(String[] inputParameters){
	  DatatypeFactory xmlDataTypeFac = null;
	  try {
		  xmlDataTypeFac = DatatypeFactory.newInstance();
	  } catch (DatatypeConfigurationException e1) {
		  Logger.info("Exception Caught.");
	  }
	  XMLGregorianCalendar startDttm = xmlDataTypeFac.newXMLGregorianCalendar((inputParameters[2]));
	  EiEvent e = new EiEvent()
	  					.withEiTarget(null)
	  					.withEventDescriptor(new EventDescriptor()
	  							.withEventID(inputParameters[4])
	  							//eventStatus not working, have a constant for now
	  							//.withEventStatus(EventStatusEnumeratedType.fromValue(inputParameters[0]))
	  							.withPriority(Long.getLong(inputParameters[5]))
	  							.withEventStatus(EventStatusEnumeratedType.FAR)
	  							.withCreatedDateTime(startDttm))
	  					.withEiActivePeriod(new EiActivePeriod()
	  							.withProperties(new Properties()
	  									.withDtstart(new Dtstart(startDttm))
	  									.withDuration(new DurationPropType(inputParameters[3]))))
	  					.withEiEventSignals(new EiEventSignals()
	  						.withEiEventSignal(new EiEventSignal()
	  							.withSignalID(inputParameters[1])
	  							.withIntervals(new Intervals()
	  									.withInterval(new Interval()
	  									.withDuration( new DurationPropType(inputParameters[3]))))));
	  return e;
  }*/
  
  /*
  //takes the string uniquely generated from the form and parses it
  //{status, signalid, start, duration, eventid, priority
  public static String[] customTokenizer(String input) {
	  
	  int statusInt = input.indexOf("Status=", 0);
	  int comma = input.indexOf(",", statusInt);
	  String status = input.substring(statusInt+7, comma);
	  
	  int signalIDInt = input.indexOf("SignalID=", statusInt);
	  comma = input.indexOf(",", signalIDInt);
	  String signalID = input.substring(signalIDInt+9, comma);
	  
	  int startInt = input.indexOf("Start=", signalIDInt);
	  comma = input.indexOf(",", startInt);
	  String start = input.substring(startInt+6, comma);
	  
	  int durationInt = input.indexOf("Duration=", startInt);
	  comma = input.indexOf(",", durationInt);
	  String duration = input.substring(durationInt+9, comma);
	  	  	  
	  int eventIDInt = input.indexOf("EventID=", durationInt);
	  comma = input.indexOf(",", eventIDInt);
	  String eventID = input.substring(eventIDInt+8, comma);
	  
	  int priorityInt = input.indexOf("Priority=", eventIDInt);
	  comma = input.indexOf(",", priorityInt);
	  String priority = input.substring(priorityInt+9, comma);

	  	  
	  //Logger.info(status + " " + signalID + " " + start + " " + duration + " " + eventID + " " + priority);
	  String[] temp = {status, signalID, start, duration, eventID, priority};
	  return temp;
  }*/
  
}