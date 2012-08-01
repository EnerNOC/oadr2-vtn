package controllers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import models.ProgramForm;
import models.StatusObject;
import models.CustomerForm;

import org.enernoc.open.oadr2.model.EiEvent.*;
import org.enernoc.open.oadr2.model.EiEvent;

import play.Logger;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.*;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

public class Programs extends Controller {
  static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
  static EntityManager entityManager = entityManagerFactory.createEntityManager();
    
  //redirects to the events page
  public static Result index() {
	  return redirect(routes.Programs.programs());
  }
  
@SuppressWarnings("unchecked")
@Transactional
public static Result programs(){
	  createNewEm();
	  List<ProgramForm> programs = entityManager.createQuery("FROM Program").getResultList();
	  
	  class ProgramFormComparator implements Comparator<ProgramForm>{
			public int compare(ProgramForm programOne, ProgramForm programTwo){
				return programOne.getProgramName().compareTo(programTwo.getProgramName());
			}
	  }
	  Collections.sort(programs, new ProgramFormComparator());
	  
	  return ok(views.html.programs.render(programs));
  }
  
  public static Result blankProgram(){
	  return ok(views.html.newProgram.render(form(ProgramForm.class)));
  }
  
  @Transactional
  public static Result newProgram(){
	  createNewEm();
	  Form<ProgramForm> filledForm = form(ProgramForm.class).bindFromRequest();
	  if(filledForm.hasErrors()){
		  //do some error handling
		  return badRequest();
	  }
	  else{
		  ProgramForm newProgram = filledForm.get();
		  //Logger.info(newProgram.getProgramName() + " " + newProgram.getProgramURI() + " " + newProgram.getId());
		  entityManager.persist(newProgram);
		  entityManager.getTransaction().commit();
		  flash("success", "Program as been created");
	  }
	  return redirect(routes.Programs.programs());
  }
  
  @SuppressWarnings("unchecked")
public static Result deleteProgram(Long id){
	  createNewEm();
	  ProgramForm program = entityManager.find(ProgramForm.class, id);
	  //Logger.info("Program: " + program.getProgramURI());
	  
	  //List<ProgramEventRelation> relations = entityManager.createQuery("FROM ProgramEvent").getResultList();
	  List<CustomerForm> customers = entityManager.createQuery("FROM Customers").getResultList();
	  /*for(ProgramEventRelation relation : relations){
		  if(relation.getProgramId() == id){
			  flash("failure", "Cannot delete program. Please delete events using the program first");
			  return redirect(routes.Programs.programs());
		  }
	  } 
	  */
	  
	  for(CustomerForm customer : customers){
		  //Logger.info("Customer: " + customer.getProgramId());
		  if(Long.parseLong(customer.getProgramId()) == id){
			  flash("failure", "Cannot delete program. Please delete customers using the program first");
			  return redirect(routes.Programs.programs());
		  }
	  }
	  flash("success", "Program has been deleted");
	  entityManager.remove(program);
	  entityManager.getTransaction().commit();
	  return redirect(routes.Programs.programs());
  }
 
  public static void createNewEm(){
	  entityManager = entityManagerFactory.createEntityManager();
	  if(!entityManager.getTransaction().isActive()){
		  entityManager.getTransaction().begin();
	  }
  }
  
 
}