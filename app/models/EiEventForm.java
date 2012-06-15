package models;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.enernoc.open.oadr2.model.DurationPropType;
import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.enernoc.open.oadr2.model.Interval;
import org.enernoc.open.oadr2.model.Intervals;
import org.enernoc.open.oadr2.model.Properties;
import org.enernoc.open.oadr2.model.EiEvent.EiActivePeriod;
import org.enernoc.open.oadr2.model.EiEvent.EiEventSignals;
import org.enernoc.open.oadr2.model.EiEvent.EventDescriptor;
import org.enernoc.open.oadr2.model.EiEvent.EiEventSignals.EiEventSignal;
import org.enernoc.open.oadr2.model.Properties.Dtstart;

import play.Logger;

public class EiEventForm {
	
	//still required to add @annotations for these to validate
	//only add fields for the objects in the form
	public String eventID;
	public long priority;
	public String status;
	public String start;
	public String duration;
	public String signalID;
	public EiEvent eiEvent;
	
	public EiEvent toEiEvent(){
		DatatypeFactory xmlDataTypeFac = null;
		  try {
			  xmlDataTypeFac = DatatypeFactory.newInstance();
		  } catch (DatatypeConfigurationException e1) {
			  Logger.info("Exception Caught.");
		  }
		  XMLGregorianCalendar startDttm = xmlDataTypeFac.newXMLGregorianCalendar(start);
		  return eiEvent = new EiEvent()
		  					.withEiTarget(null)
		  					.withEventDescriptor(new EventDescriptor()
		  							.withEventID(eventID)
		  							//eventStatus not working, have a constant for now
		  							//.withEventStatus(EventStatusEnumeratedType.fromValue(status))
		  							.withPriority(priority)
		  							.withEventStatus(EventStatusEnumeratedType.FAR)
		  							.withCreatedDateTime(startDttm))
		  					.withEiActivePeriod(new EiActivePeriod()
		  							.withProperties(new Properties()
		  									.withDtstart(new Dtstart(startDttm))
		  									.withDuration(new DurationPropType(duration))))
		  					.withEiEventSignals(new EiEventSignals()
		  						.withEiEventSignal(new EiEventSignal()
		  							.withSignalID(signalID)
		  							.withIntervals(new Intervals()
		  									.withInterval(new Interval()
		  									.withDuration( new DurationPropType(duration))))));
	}
	
	public String toString(){
		String returnString = "";
		returnString += "\nEventID: " + eiEvent.getEventDescriptor().getEventID() + "\n";
		returnString += "Priority: " + eiEvent.getEventDescriptor().getPriority() + "\n";
		returnString += "Status: " + eiEvent.getEventDescriptor().getEventStatus().toString() + "\n";
		returnString += "Start: " + eiEvent.getEiActivePeriod().getProperties().getDtstart().getDateTime().toString() + "\n";
		returnString += "Duration: " + eiEvent.getEiActivePeriod().getProperties().getDuration().getDuration() + "\n";
		returnString += "SignalID: " + eiEvent.getEiEventSignals().getEiEventSignal().get(0).getSignalID() + "";
		return returnString;		
	}
	

}
