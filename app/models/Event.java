package models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.Valid;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.enernoc.open.oadr2.model.DateTime;
import org.enernoc.open.oadr2.model.Dtstart;
import org.enernoc.open.oadr2.model.DurationPropType;
import org.enernoc.open.oadr2.model.DurationValue;
import org.enernoc.open.oadr2.model.EiActivePeriod;
import org.enernoc.open.oadr2.model.EiEvent;
import org.enernoc.open.oadr2.model.EiEventSignal;
import org.enernoc.open.oadr2.model.EiEventSignals;
import org.enernoc.open.oadr2.model.EventDescriptor;
import org.enernoc.open.oadr2.model.EventStatusEnumeratedType;
import org.enernoc.open.oadr2.model.Interval;
import org.enernoc.open.oadr2.model.Intervals;
import org.enernoc.open.oadr2.model.Properties;

import play.data.validation.Constraints.Min;
import play.data.validation.Constraints.Required;

public class Event{
	
	@Required(message = "Must enter an Event ID")
	private String eventID;
	@Required(message = "Must enter a Priority")
	@Min(message = "Priority must be greater than or equal to 0", value = 0)
	@Valid
	private long priority;
	private String status = "none";
	@Required(message = "Must enter a Start Date")
	private String startDate;
	@Required(message = ("Must enter a Start Time"))
	private String startTime;	
	@Required(message = ("Must enter an End Date"))
	private String endDate;
	@Required(message = ("Must enter an End Time"))
	private String endTime;	
	@Required(message = ("Must enter the number of intervals"))
	@Min(message = "Priority must be greater than or equal to one", value = 1)
	@Valid
	private long intervals = 1;
	@Required(message = ("Must select a program, if one is not available please create one."))
	private String marketContext;
	
	
	private String duration;	
	private String start;
	
	public Map<String, String> statusTypes;
	
	private EiEvent eiEvent;
	
	//constructor for the blank form
	public Event(){
		DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date date = new Date();
		startDate = dateFormat.format(date);
		endDate = dateFormat.format(date);
		createStatusTypes();
	}
	
	//constructor for the edit form 
	public Event(EiEvent e){
		createStatusTypes();
		eiEvent = e;
		this.eventID = e.getEventDescriptor().getEventID();
		this.priority = e.getEventDescriptor().getPriority();
		this.status = e.getEventDescriptor().getEventStatus().value();
		//TODO This line below could be a bit picky come time for Edit
		this.start = e.getEiActivePeriod().getProperties().getDtstart().getDateTime().getValueItem().toString();
		this.duration = e.getEiActivePeriod().getProperties().getDuration().getDuration().getValue();
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
	    final XMLGregorianCalendar startDttm = xmlDataTypeFac.newXMLGregorianCalendar(start).normalize();
	    return eiEvent = new EiEvent()	                    
	  					.withEventDescriptor(new EventDescriptor()
  							.withEventID(eventID)
  							.withPriority(priority)
  							.withCreatedDateTime(new DateTime(startDttm))
  							.withModificationNumber(0))
	  					.withEiActivePeriod(new EiActivePeriod()
  							.withProperties(new Properties()
								.withDtstart(new Dtstart(new org.enernoc.open.oadr2.model.DateTime(startDttm)))
								.withDuration(new DurationPropType(new DurationValue(duration)))))
	  					.withEiEventSignals(new EiEventSignals()
	  						.withEiEventSignals(new EiEventSignal()
	  							.withIntervals(new Intervals()
  									.withIntervals(new Interval()
  									.withDuration( new DurationPropType(new DurationValue(duration)))))));
	}
	
	public EiEvent getEiEvent(){
		return this.eiEvent;
	}
	
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
	    final XMLGregorianCalendar startDttm = xmlDataTypeFac.newXMLGregorianCalendar(start).normalize(); //NEED TO ADD START
		eiEvent.getEiActivePeriod().getProperties().getDtstart().setDateTime(new org.enernoc.open.oadr2.model.DateTime(startDttm));
		eiEvent.getEiActivePeriod().getProperties().getDuration().setDuration(new DurationValue(this.duration));
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
	public DateTime createDateTime(String date, String time){
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
		DateTime dateTime = new DateTime();		
        DatatypeFactory xmlDataTypeFac = null;
        try {
            xmlDataTypeFac = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e1) {
            e1.printStackTrace();
        }
        XMLGregorianCalendar calendar = xmlDataTypeFac.newXMLGregorianCalendar();
        calendar.setYear(startYear);
        calendar.setMonth(startMonth);
        calendar.setDay(startDay);
        calendar.setHour(startHour);
        calendar.setMinute(startMinute);
		dateTime.setValue(calendar);
		
		return dateTime;
	}
	
	// returns a difference in minutes between the start and end of the requested times
	private long getMinutesDuration(){		
		DateTime startDateTime = createDateTime(startDate, startTime);
		DateTime endDateTime = createDateTime(endDate, endTime);

        long milliseconds = endDateTime.getValue().toGregorianCalendar().getTimeInMillis() - startDateTime.getValue().toGregorianCalendar().getTimeInMillis();
        long minutes = milliseconds / 60000;
        
        return minutes;
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


        DatatypeFactory xmlDataTypeFac = null;
        try {
            xmlDataTypeFac = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e1) {
            e1.printStackTrace();
        }
	    
		DateTime endDateTime = createDateTime(startDate, startTime); // temp == start time
		Duration duration = xmlDataTypeFac.newDuration(this.duration.toString());
		endDateTime.getValue().add(duration);
		String endString = endDateTime.toString();
		
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
	public static int minutesFromXCal(String xCal){
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
		DateTime dtStart = createDateTime(startDate, startTime);
		DateTime dtEnd = createDateTime(endDate, endTime);
		return dtStart.getValue().toGregorianCalendar().getTimeInMillis() <= dtEnd.getValue().toGregorianCalendar().getTimeInMillis();	
	}
	
	public void convertDateTimeToJoda(){
	    
	}

    public String getMarketContext() {
        return marketContext;
    }

    public void setMarketContext(String marketContext) {
        this.marketContext = marketContext;
    }
    
    public String getEventID(){
        return eventID;
    }
    
    public void setEventID(String eventID) {
        this.eventID = eventID;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStartDate(){
        return startDate;
    }
    
    public void setStartDate(String startDate){
        this.startDate = startDate;
    }
    
    public String getEndDate(){
        return endDate;
    }
    
    public void setEndDate(String endDate){
        this.endDate = endDate;
    }
    
    public String getStartTime(){
        return startDate;
    }
    
    public void setStartTime(String startTime){
        this.startTime = startTime;
    }
    
    public String getendTime(){
        return endDate;
    }
    
    public void setendTime(String endTime){
        this.endTime = endTime;
    }
    
    public long getPriority(){
        return priority;
    }
    
    public void setPriority(long priority){
        this.priority = priority;
    }

    public long getIntervals() {
        return intervals;
    }

    public void setIntervals(long intervals) {
        this.intervals = intervals;
    }
}
