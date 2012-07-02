package controllers;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import models.ProjectForm;

import play.mvc.*;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

public class Markets extends Controller {
  static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("testUnit");
  static EntityManager entityManager = entityManagerFactory.createEntityManager();
    
  
  public static Result projects(){
	  List<ProjectForm> blankArray = new ArrayList<ProjectForm>();
	  return ok(views.html.projects.render(blankArray));
  }
  
  public static Result blankProject(){
	  return ok(views.html.newProject.render(form(ProjectForm.class)));
  }
  
  public static Result newProject(){
	  return redirect(routes.Markets.projects());
  }
  
  public static Result deleteProject(Long id){
	  return TODO;
  }
  
  //since entity managers are cheap, each one is to be made new for each transaction
  //so as to avoid PessimisticLockException
  public static void createNewEm(){
	  entityManager = entityManagerFactory.createEntityManager();
	  if(!entityManager.getTransaction().isActive()){
		  entityManager.getTransaction().begin();
	  }
  }
 
}