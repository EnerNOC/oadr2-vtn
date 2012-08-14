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
import models.VENStatus;
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
	  List<ProgramForm> programs = JPA.em().createQuery("FROM Program").getResultList();
	  
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
	  Form<ProgramForm> filledForm = form(ProgramForm.class).bindFromRequest();
	  if(filledForm.hasErrors()){
		  //do some error handling
		  return badRequest();
	  }
	  else{
		  ProgramForm newProgram = filledForm.get();
		  JPA.em().persist(newProgram);
		  flash("success", "Program as been created");
	  }
	  return redirect(routes.Programs.programs());
  }
  
  @SuppressWarnings("unchecked")
  
  @Transactional
  public static Result deleteProgram(Long id){
	  ProgramForm program = JPA.em().find(ProgramForm.class, id);
	  List<CustomerForm> customers = JPA.em().createQuery("FROM Customers").getResultList();
	  
	  for(CustomerForm customer : customers){
		  if(Long.parseLong(customer.getProgramId()) == id){
			  flash("failure", "Cannot delete program. Please delete customers using the program first");
			  return redirect(routes.Programs.programs());
		  }
	  }
	  flash("success", "Program has been deleted");
	  JPA.em().remove(program);
	  return redirect(routes.Programs.programs());
  }
 
  public static void createNewEm(){
	  entityManager = entityManagerFactory.createEntityManager();
	  if(!JPA.em().getTransaction().isActive()){
		  JPA.em().getTransaction().begin();
	  }
  }
  
 
}