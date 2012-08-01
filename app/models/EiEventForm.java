package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Valid;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.enernoc.open.oadr2.model.DurationPropType;
import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiEvent.EventDescriptor.EiMarketContext;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.enernoc.open.oadr2.model.Interval;
import org.enernoc.open.oadr2.model.Intervals;
import org.enernoc.open.oadr2.model.Properties;
import org.enernoc.open.oadr2.model.EiEvent.EiActivePeriod;
import org.enernoc.open.oadr2.model.EiEvent.EiEventSignals;
import org.enernoc.open.oadr2.model.EiEvent.EventDescriptor;
import org.enernoc.open.oadr2.model.EiEvent.EiEventSignals.EiEventSignal;
import org.enernoc.open.oadr2.model.Properties.Dtstart;

import org.joda.time.*;

import play.Logger;
import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;

/*
 * RegEx example
 * DURATION_PAT = r'([+-])?P(?:(\d+)W)?(?:(\d+)D)?T?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?'
 * DURATION_REX = re.compile(DURATION_PAT)
 *    Example:
   `parse_duration('P15DT5H20S')` -> `(None, None, '15', '5', None, '20')`
   '''
   groups = DURATION_REX.match(dur_str).groups()
   vals = tuple(int(i) if i is not None else 0 for i in groups[1:])
   return (groups[0] or '+',) + val
 */

//Other method for adding custom validation by creating your own notations
//http://stackoverflow.com/questions/8115106/how-to-create-a-custom-validator-in-play-framework-2-0

public class EiEventForm{
	
	@Required(message = "Must enter an Event ID")
	public String eventID;
	@Required(message = "Must enter a Priority")
	@Min(message = "Priority must be greater than zero", value = 1)
	@Valid
	public long priority;
	@Required(message = "Must select a status")
	public String status;
	@Required(message = "Must enter a Start Date")
	public String startDate;
	@Required(message = ("Must enter a Start Time"))
	public String startTime;	
	@Required(message = ("Must enter an End Date"))
	public String endDate;
	@Required(message = ("Must enter an End Time"))
	public String endTime;	
	@Required(message = ("Must enter a Signal ID"))
	public String signalID;
	@Required(message = ("Must select a program, if one is not available please create one."))
	public String marketContext;
	
	
	private String duration;	
	private String start;
	
	public Map<String, String> statusTypes;
	
	private EiEvent eiEvent;
	
	//constructor for the blank form
	public EiEventForm(){
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date date = new Date();
		startDate = dateFormat.format(date);
		endDate = dateFormat.format(date);
		createStatusTypes();
	}
	
	//constructor for the edit form 
	public EiEventForm(EiEvent e){
		createStatusTypes();
		eiEvent = e;
		this.eventID = e.getEventDescriptor().getEventID();
		this.priority = e.getEventDescriptor().getPriority();
		this.status = e.getEventDescriptor().getEventStatus().value();
		this.start = e.getEiActivePeriod().getProperties().getDtstart().getDateTime().toString();
		this.duration = e.getEiActivePeriod().getProperties().getDuration().getDuration();
		this.signalID = e.getEiEventSignals().getEiEventSignal().get(0).getSignalID();
		
		setStartDateTime(this.start);
		setEndDateTime();
	}
	
	// returns the map for the @select statement for forms in .scala.html
	public Map<String, String> getStatusTypes(){
		return this.statusTypes;
	}
	
	// creates a map to send to the forms for use in the @select statement
	public void createStatusTypes(){
		statusTypes = new HashMap<String, String>();
		statusTypes.put("active", "Active");
		statusTypes.put("cancelled", "Cancelled");
		statusTypes.put("completed", "Completed");
		statusTypes.put("far", "Far");
		statusTypes.put("near", "Near");
		statusTypes.put("none", "None");
	}
	
	//returns an EiEvent based on the filled EiEventForm to be persisted
	public EiEvent toEiEvent(){
		duration = createXCalString(getMinutesDuration());
		this.start = createXMLTime(startDate, startTime);
		DatatypeFactory xmlDataTypeFac = null;
	    try {
	    	xmlDataTypeFac = DatatypeFactory.newInstance();
		  } catch (DatatypeConfigurationException e1) {
			  e1.printStackTrace();
		  }
	    final XMLGregorianCalendar startDttm = xmlDataTypeFac.newXMLGregorianCalendar(start);
		  return eiEvent = new EiEvent()
		  					.withEiTarget(null)
		  					.withEventDescriptor(new EventDescriptor()
	  							.withEventID(eventID)
	  							.withPriority(priority)
	  							.withEventStatus(EventStatusEnumeratedType.fromValue(status))
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
	
	//old toString method used for testing content of the eiEvent
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
	
	//gets the eiEvent field
	public EiEvent getEiEvent(){
		return this.eiEvent;
	}
	
	//copies an event to a getable eiEvent, with all hjids in tact
	//sets the values in the getable event to a parameter EiEvent
	public void copyEvent(EiEvent e){
		eiEvent = e;
		eiEvent.getEventDescriptor().setEventID(this.eventID);
		eiEvent.getEventDescriptor().setPriority(this.priority);
		eiEvent.getEventDescriptor().setEventStatus(EventStatusEnumeratedType.fromValue(this.status));
		DatatypeFactory xmlDataTypeFac = null;
	    try {
	    	xmlDataTypeFac = DatatypeFactory.newInstance();
		  } catch (DatatypeConfigurationException e1) {
			  e1.printStackTrace();
		  }
		duration = createXCalString(getMinutesDuration());
		this.start = createXMLTime(startDate, startTime);
	    final XMLGregorianCalendar startDttm = xmlDataTypeFac.newXMLGregorianCalendar(start); //NEED TO ADD START
		eiEvent.getEiActivePeriod().getProperties().getDtstart().setDateTime(startDttm);
		eiEvent.getEiActivePeriod().getProperties().getDuration().setDuration(this.duration);
		eiEvent.getEiEventSignals().getEiEventSignal().get(0).setSignalID(this.signalID);
	}
	
	//takes in a string of a date and string of time in specific form
	// date = "MM-dd-yyyy" time = "h:mm" || time = "hh:mm"
	//returns a string accepted by the XMLGregorianCalendar
	private String createXMLTime(String date, String time){
		String year = date.substring(6, 10);
		String month = date.substring(0, 2);
		String day = date.substring(3, 5);
		int hour = 0;
		String tempString = time;
		if(time.charAt(1) == ':'){ // if single digit time make 0X:XX
			tempString = ("0" + time);
		}
		hour = Integer.parseInt(tempString.substring(0, 2));
		if(hour == 12){ // if it's noon/midnight set hour to 0
			hour = 0;
		}
		if(time.charAt(6) == 'P'){ // add 12 hours for PM
			hour += 12;
		}
		String hourString = hour + "";
		if(hour < 10){
			hourString = "0" + hour;
		}
		String minute = tempString.substring(3, 5);
		return year + "-" + month + "-" + day + "T" + hourString + ":" + minute + ":00";
	}
	
	// returns a date time obgram from two string inputs
	// date = "MM-dd-yyyy" time = "h:mm" || time = "hh:mm"
	// time must not be 0 start for hours
	public DateTime createJodaTime(String date, String time){
		int startYear = Integer.parseInt(date.substring(6, 10));
		int startMonth = Integer.parseInt(date.substring(0, 2));
		int startDay = Integer.parseInt(date.substring(3, 5));
		int startHour = Integer.parseInt(time.substring(0, 2));
		if(startHour == 12){
			startHour = 0;
		}
		if(time.charAt(6) == 'P'){
			startHour += 12;
		}
		int startMinute = Integer.parseInt(time.substring(3, 5));
		
		return new DateTime(startYear, startMonth, startDay, startHour, startMinute);
	}
	
	// returns a difference in minutes between the start and end of the requested times
	private long getMinutesDuration(){		
		DateTime startJodaTime = createJodaTime(startDate, startTime);
		DateTime endJodaTime = createJodaTime(endDate, endTime);
		
		org.joda.time.Interval i = new org.joda.time.Interval(startJodaTime, endJodaTime);
		
		Duration d = new Duration(i.getStartMillis(), i.getEndMillis());		
				
		return d.getStandardMinutes();
	}
	
	//sets the startDate and startTime based on the string from ical
	private void setStartDateTime(String startString){
		this.startDate = makeStartDate(startString);
		this.startTime = makeStartTime(startString);
	}
	
	public String makeStartDate(String startString){
		return this.startDate = startString.substring(5, 7) + "-" + startString.substring(8, 10) + "-" + startString.substring(0, 4);
	}
	
	public String makeStartTime(String startString){
		int startHours = Integer.parseInt(startString.substring(11, 13));
		String startSuffix = " AM";		
		if(startHours >= 12){
			startSuffix = " PM";
			if(startHours > 12){
				startHours -= 12;
			}
		}		
		String startHoursString = "";
		if(startHours < 10){
			startHoursString = "0" + startHours;
		}
		else{
			startHoursString = "" + startHours;
		}
		startHoursString = startHoursString.replace("00", "12");
		return (startHoursString + ":" + startString.substring(14, 16) + startSuffix);
	}
	
	//sets the end date based on the startDate and startTime
	//should also make this extensible somehow
	private void setEndDateTime(){

		DateTime endJodaTime = createJodaTime(startDate, startTime); // temp == start time
		endJodaTime = endJodaTime.plusMinutes(minutesFromXCal(duration));
		String endString = endJodaTime.toString();
		
		this.endDate = endString.substring(5, 7) + "-" + endString.substring(8, 10) + "-" + endString.substring(0, 4);
		int startHours = Integer.parseInt(endString.substring(11, 13));

		String startSuffix = " AM";
		
		if(startHours >= 12){
			startSuffix = " PM";
			if(startHours > 12){
				startHours -= 12;
			}
		}
		String startHoursString = startHours + "";
		if(startHours == 0){
			startHoursString = "12";
		}
		this.endTime = (startHoursString + ":" + endString.substring(14, 16) + startSuffix);
	}
	
	// takes a long of minutes and creates it into an XCal string
	// only positive and does not account for seconds
	private String createXCalString(long minutes){
		int weeks = (int) (minutes / 10080);
		minutes -= weeks * 10080;
		int days = (int) (minutes / 1440);
		minutes -= days * 1440;
		int hours = (int) (minutes / 60);
		minutes -= hours * 60;
		String returnString = "P";
		if(weeks > 0){
			returnString += (weeks + "W");
		}
		if(days > 0){
			returnString += (days + "D");
		}
		returnString += "T";
		if(hours > 0){
			returnString += (hours + "H");
		}
		if(minutes > 0){
			returnString += (minutes + "M");
		}
		return returnString;
	}
	
	//uses the regex to take the ISO8601 string and return
	//the number of minutes contained in it
	private int minutesFromXCal(String xCal){
		Pattern p = Pattern.compile("P(?:(\\d+)W)?(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?");
		Matcher m = p.matcher(xCal);
		int returnMinutes = 0;
		m.find();
		if(m.group(1) != null){
			returnMinutes += Integer.parseInt(m.group(1)) * 10080;
		}
		if(m.group(2) != null){
			returnMinutes += Integer.parseInt(m.group(2)) * 1440;
		}
		if(m.group(3) != null){
			returnMinutes += Integer.parseInt(m.group(3)) * 60;
		}
		if(m.group(4) != null){
			returnMinutes += Integer.parseInt(m.group(4));
		}
		
		return returnMinutes;	
	}
	
	public String displayReadableDuration(String s){
		return "" + minutesFromXCal(s);
	}
	
	public String displayReadableStart(String s){
		String time = makeStartTime(s);
		time = time.replace("00:", "12:");
		return makeStartDate(s) + " @ " + time;
	}
	
	public String validate(){
		String errorMessage = validation(this.startDate, this.endDate, this.startTime, this.endTime);
		if(errorMessage != null){
			return errorMessage;
		}
		else{
			return null;
		}
	}
	
	public String validation(String startDate, String endDate, String startTime, String endTime){
		String dateRegEx = "^([0|1]\\d)-([0-3]\\d)-(\\d+)$";
		Pattern datePattern = Pattern.compile(dateRegEx);
		Matcher dateMatcher = datePattern.matcher(startDate);
		if(!dateMatcher.find()){
			return "Invalid start date.";
		}
		dateMatcher = datePattern.matcher(endDate);
		if(!dateMatcher.find()){
			return "Invalid end date.";
		}
		
		String timeRegEx = "^(1[0-2]|0[1-9]):([0-5][0-9])(\\s)?(?i)(am|pm)$";
		Pattern timePattern = Pattern.compile(timeRegEx);
		Matcher timeMatcher = timePattern.matcher(startTime);
		if(!timeMatcher.find()){
			return "Invalid start time.";
		}
		timeMatcher = timePattern.matcher(endTime);
		if(!timeMatcher.find()){
			return "Invalid end time.";
		}
		if(!startIsBeforeEnd(startDate, startTime, endDate, endTime)){
			return "End date and time needs to occur after start date and time.";
		}
		
		return null;
	}
	
	public boolean startIsBeforeEnd(String startDate, String startTime, String endDate, String endTime){		
		DateTime jodaStart = createJodaTime(startDate, startTime);
		DateTime jodaEnd = createJodaTime(endDate, endTime);
		if(jodaStart.getMillis() > jodaEnd.getMillis()){
			return false;
		}
		
		return true;
	}
}
