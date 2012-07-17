package controllers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import models.ProjectEventRelation;
import models.ProjectForm;

import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.OadrCreatedEvent;
import org.enernoc.open.oadr2.model.OadrDistributeEvent;
import org.enernoc.open.oadr2.model.OadrRequestEvent;
import org.enernoc.open.oadr2.model.OadrResponse;

import play.Logger;
import play.data.Form;
import play.data.validation.ValidationError;
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
	  createNewEm();	  
	  List<EiEvent> eiEvents = entityManager.createQuery("FROM org.enernoc.open.oadr2.model.EiEvent").getResultList();
	  List<EiEventForm> eiEventForms = new ArrayList<EiEventForm>();
	  for(EiEvent event : eiEvents){
		  EiEventForm tempForm = new EiEventForm(event);
		  ProjectEventRelation eventRelation = getRelationFromEvent(event.getHjid());
		  tempForm.marketContext = entityManager.find(ProjectForm.class, (long)eventRelation.getProjectId()).getProjectName();
		  eiEventForms.add(tempForm);
	  }
	  Comparator<EiEvent> comparator = new EiEventComparator();
	  Collections.sort(eiEvents, comparator);
	  
	  return ok(views.html.events.render(eiEventForms, new EiEventForm())); //not actually an error
  }
  
public static Result blankEvent(){
	  EiEventForm newForm = new EiEventForm();
	  return ok(views.html.createEvent.render(form(EiEventForm.class).fill(newForm), newForm, makeProjectMap()));
  }//
  
  @Transactional
  //Method to create a new event once on the newEvent page
  public static Result newEvent(){
	  createNewEm();
	  Form<EiEventForm> filledForm = form(EiEventForm.class).bindFromRequest();
      if(filledForm.hasErrors()) {
    	  addFlashError(filledForm.errors());
          return badRequest(views.html.createEvent.render(filledForm, new EiEventForm(), makeProjectMap()));
      }
	  else{		  
		  EiEventForm newEventForm = filledForm.get();
		  EiEvent newEvent = newEventForm.toEiEvent();
		  entityManager.persist(newEvent);
		  entityManager.getTransaction().commit();
		  flash("success", "Event as been created");
		  
		  ProjectEventRelation newRelation = new ProjectEventRelation(newEvent.getHjid(), 
				  Integer.parseInt(newEventForm.marketContext));
		  createNewEm();
		  entityManager.persist(newRelation);
		  entityManager.getTransaction().commit();
		  
		  return redirect(routes.Events.newEvent());		  
	  }
  }
  
  @Transactional
  //Deletes an event based on the id
  public static Result deleteEvent(Long id){
	  createNewEm();
	  entityManager.remove(entityManager.find(EiEvent.class, id));
	  entityManager.remove(entityManager.find(ProjectEventRelation.class, getRelationFromEvent(id).getId()));
	  entityManager.getTransaction().commit();
      flash("success", "Event has been deleted");
      return redirect(routes.Events.events());
  }
  
  @Transactional
  public static Result updateEvent(Long id){
	  createNewEm();
	  Form<EiEventForm> eventForm = form(EiEventForm.class).bindFromRequest();
      if(eventForm.hasErrors()) {
          return badRequest(views.html.editEvent.render(id, eventForm, new EiEventForm(), makeProjectMap()));
      }
      EiEventForm eiEventForm= eventForm.get();
      eiEventForm.copyEvent(entityManager.find(EiEvent.class, id));
      EiEvent event = entityManager.find(EiEvent.class, id); //is actually used, eclipse is a liar
      event = eiEventForm.getEiEvent();
      entityManager.getTransaction().commit();
      flash("success", "Event has been updated");
      ProjectEventRelation theId = getRelationFromEvent(id);
      ProjectEventRelation relation = entityManager.find(ProjectEventRelation.class, theId.getId());
      relation.setProjectId(Integer.parseInt(eiEventForm.marketContext));
      
	  createNewEm();
	  entityManager.merge(relation);
	  entityManager.getTransaction().commit();
	  return redirect(routes.Events.events());
  }
  
@Transactional
  public static Result editEvent(Long id){
	  createNewEm();
	  EiEventForm form = new EiEventForm(entityManager.find(EiEvent.class, id));
	  form.marketContext = getRelationFromEvent(id).getProjectId() + "";
	  return ok(views.html.editEvent.render(id, form(EiEventForm.class).fill(form), form, makeProjectMap()));
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
			  flash(key, error.message());
		  }
	  }	  
  }
 
  @SuppressWarnings("unchecked")
public static Map<String, String> makeProjectMap(){
	  List<ProjectForm> projectList = entityManager.createQuery("FROM Project").getResultList();
	  Map<String, String> projectMap = new HashMap<String, String>();
	  for(ProjectForm project : projectList){
		  projectMap.put(project.getId() + "", project.getProjectName());
	  }
	  return projectMap;
  }
  
  public static ProjectEventRelation getRelationFromEvent(long eventId){
	  return (ProjectEventRelation) entityManager.createQuery("FROM ProjectEvent WHERE eventId=" 
			  + eventId).getResultList().get(0);
  }
  
}