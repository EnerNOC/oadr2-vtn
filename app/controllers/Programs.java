package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import models.Program;
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
	  List<Program> programs = JPA.em().createQuery("FROM Program").getResultList();
	  
	  class ProgramFormComparator implements Comparator<Program>{
			public int compare(Program programOne, Program programTwo){
				return programOne.getProgramName().compareTo(programTwo.getProgramName());
			}
	  }
	  Collections.sort(programs, new ProgramFormComparator());
	  
	  return ok(views.html.programs.render(programs));
  }
  
  public static Result blankProgram(){
	  return ok(views.html.newProgram.render(form(Program.class)));
  }
  
  @Transactional
  public static Result newProgram(){
	  Form<Program> filledForm = form(Program.class).bindFromRequest();
	  if(filledForm.hasErrors()){
		  //do some error handling
		  return badRequest();
	  }
	  else{
		  Program newProgram = filledForm.get();
		  JPA.em().persist(newProgram);
		  flash("success", "Program as been created");
	  }
	  return redirect(routes.Programs.programs());
  }
  
  @SuppressWarnings("unchecked")
  
  @Transactional
  public static Result deleteProgram(Long id){
	  Program program = JPA.em().find(Program.class, id);
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