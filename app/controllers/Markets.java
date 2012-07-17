package controllers;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import models.ProjectEventRelation;
import models.ProjectForm;

import org.enernoc.open.oadr2.model.EiEvent.*;
import org.enernoc.open.oadr2.model.EiEvent;

import play.Logger;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.*;

//export PATH=$PATH:/Users/jlajoie/Documents/play-2.0.1

public class Markets extends Controller {
  static EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("Events");
  static EntityManager entityManager = entityManagerFactory.createEntityManager();
    
  //redirects to the events page
  public static Result index() {
	  return redirect(routes.Markets.projects());
  }
  
@SuppressWarnings("unchecked")
@Transactional
public static Result projects(){
	  createNewEm();
	  List<ProjectForm> blankArray = entityManager.createQuery("FROM Project").getResultList();	  
	  return ok(views.html.projects.render(blankArray));
  }
  
  public static Result blankProject(){
	  return ok(views.html.newProject.render(form(ProjectForm.class)));
  }
  
  @Transactional
  public static Result newProject(){
	  createNewEm();
	  Form<ProjectForm> filledForm = form(ProjectForm.class).bindFromRequest();
	  if(filledForm.hasErrors()){
		  //do some error handling
		  return badRequest();
	  }
	  else{
		  ProjectForm newProject = filledForm.get();
		  //Logger.info(newProject.getProjectName() + " " + newProject.getProjectURI() + " " + newProject.getId());
		  entityManager.persist(newProject);
		  entityManager.getTransaction().commit();
		  flash("success", "Program as been created");
	  }
	  return redirect(routes.Markets.projects());
  }
  
  @SuppressWarnings("unchecked")
public static Result deleteProject(Long id){
	  createNewEm();
	  ProjectForm p = entityManager.find(ProjectForm.class, id);
	  List<ProjectEventRelation> relations = entityManager.createQuery("FROM ProjectEvent").getResultList();
	  
	  for(ProjectEventRelation relation : relations){
		  if(relation.getProjectId() == id){
			  flash("failure", "Cannot delete program. Please delete events using the program first");
			  return redirect(routes.Markets.projects());
		  }
	  }
	  flash("success", "Program has been deleted");
	  entityManager.remove(p);
	  entityManager.getTransaction().commit();
	  return redirect(routes.Markets.projects());
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