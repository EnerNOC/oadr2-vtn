package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import models.EiEventForm;
import models.ProjectForm;

import org.enernoc.open.oadr2.model.EiEvent;

import play.data.*;
import play.data.validation.ValidationError;
import play.db.jpa.Transactional;
import play.mvc.*;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

/*
 * Known error, trying to update an event and then inputting an incorrect
 * value to any field defaults the Start and End Time to current time.
 */

public class Events extends Controller {
  static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("testUnit");
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
	  createNewEm();
	  List<EiEvent> eiEvents = new ArrayList<EiEvent>();
	  eiEvents = entityManager.createQuery("FROM EiEvent").getResultList();	  
	  Comparator<EiEvent> comparator = new EiEventComparator();
	  Collections.sort(eiEvents, comparator);
	  entityManager.close();
	  EiEventForm blankForm = new EiEventForm();
	  return ok(views.html.events.render(eiEvents, blankForm)); //not actually an error
  }
  
  public static Result blankEvent(){
	  //return ok(views.html.test.render(form(EiEventForm.class), new EiEventForm()));
	  EiEventForm newForm = new EiEventForm();
	  return ok(views.html.createEvent.render(form(EiEventForm.class).fill(newForm), newForm));
  }//
  
  @Transactional
  //Method to create a new event once on the newEvent page
  public static Result newEvent(){
	  createNewEm();
	  Form<EiEventForm> filledForm = form(EiEventForm.class).bindFromRequest();
      if(filledForm.hasErrors()) {
    	  addFlashError(filledForm.errors());
          return badRequest(views.html.createEvent.render(filledForm, new EiEventForm()));
      }
	  else{		  
		  EiEventForm newEventForm = filledForm.get();
		  EiEvent newEvent = newEventForm.toEiEvent();
		  if(!entityManager.getTransaction().isActive()){
			  entityManager.getTransaction().begin();
		  }
		  entityManager.persist(newEvent);
		  entityManager.getTransaction().commit();
		  flash("success", "Event as been created");
		  entityManager.close();
		  return redirect(routes.Events.newEvent());		  
	  }
  }
  
  @Transactional
  //Deletes an event based on the
  public static Result deleteEvent(Long hjid){
	  createNewEm();
	  entityManager.remove(entityManager.find(EiEvent.class, hjid));
	  entityManager.getTransaction().commit();
      flash("success", "Event has been deleted");
      entityManager.close();
      return redirect(routes.Events.events());
  }
  
  @Transactional
  public static Result updateEvent(Long hjid){
	  createNewEm();
	  Form<EiEventForm> eventForm = form(EiEventForm.class).bindFromRequest();
      if(eventForm.hasErrors()) {
          return badRequest(views.html.editEvent.render(hjid, eventForm, new EiEventForm()));
      }
      EiEventForm eiEventForm= eventForm.get();
      eiEventForm.copyEvent(entityManager.find(EiEvent.class, hjid));
      EiEvent event = entityManager.find(EiEvent.class, hjid); //is actually used, eclipse lies
      event = eiEventForm.getEiEvent();
      entityManager.getTransaction().commit();
      flash("success", "Event has been updated");
      entityManager.close();
	  return redirect(routes.Events.events());
  }
  
  @Transactional
  public static Result editEvent(Long hjid){
	  createNewEm();
	  EiEventForm form = new EiEventForm(entityManager.find(EiEvent.class, hjid));
	  entityManager.close();
      return ok(views.html.editEvent.render(hjid, form(EiEventForm.class).fill(form), form));
  }
  
  //since entity managers are cheap, each one is to be made new for each transaction
  //so as to avoid PessimisticLockException
  public static void createNewEm(){
	  entityManager = entityManagerFactory.createEntityManager();
	  if(!entityManager.getTransaction().isActive()){
		  entityManager.getTransaction().begin();
	  }
  }
  
  //Takes the error Map with a string as a key and adds
  //the key and value to the flash() scope to be accessed
  public static void addFlashError(Map<String, List<ValidationError>> errors){
	  for(String key : errors.keySet()){
		  List<ValidationError> currentError = errors.get(key);
		  for(ValidationError error : currentError){
			  if(!key.equals("")){
				  flash(key, error.message());
			  }
			  else{
				  flash(setKey(error.message()), error.message());
			  }
		  }
	  }	  
  }
  
  //takes the message from the error map and adds a key
  //since only String return statements are accepted in 2.0.1
  //could use 2.0.2 to return a map from validate method
  public static String setKey(String message){
	  if(message.equals("Invalid start time.")){
		  return "startTime";
	  }
	  else if(message.equals("Invalid start date.")){
		  return "startDate";
	  }
	  else if(message.equals("Invalid end time.")){
		  return "endTime";
	  }
	  else if(message.equals("Invalid end date.")){
		  return "endDate";
	  }
	  else if(message.equals("End date and time needs to occur after start date and time.")){
		  return "badEnd";
	  }
	  else{
		  return "";
	  }	  
  }
 
}