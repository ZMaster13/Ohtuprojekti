package fi.ohtu.mobilityprofile;

import android.content.Context;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fi.ohtu.mobilityprofile.data.*;
import fi.ohtu.mobilityprofile.domain.CalendarTag;
import fi.ohtu.mobilityprofile.domain.FavouritePlace;
import fi.ohtu.mobilityprofile.domain.RouteSearch;
import fi.ohtu.mobilityprofile.domain.Visit;

/**
 * This class is used for calculating the most likely trips the user is going to make.
 */
public class DestinationLogic {
    private boolean defaultSource = false;
    private boolean calendarSource = false;
    private boolean routeSource = false;
    private boolean visitSource = false;
    private boolean favouritesSource = false;

    private CalendarTagDao calendarTagDao;
    private VisitDao visitDao;
    private RouteSearchDao routeSearchDao;
    private FavouritePlaceDao favouritePlaceDao;

    private CalendarConnection calendar;

    private String latestStartLocation;
    private String latestDestination;
    private ArrayList<String> latestDestinations;

    /**
     * Creates the MobilityProfile.
     *
     * @param context        Context of the calling app. Used when getting events from calendars.
     * @param calendarTagDao DAO for calendar tags
     * @param visitDao       DAO for visits
     * @param routeSearchDao DAO for used searches
     * @param favouritePlaceDao DAO for favourite places
     */
    public DestinationLogic(Context context, CalendarTagDao calendarTagDao, VisitDao visitDao,
                           RouteSearchDao routeSearchDao, FavouritePlaceDao favouritePlaceDao) {
        this.calendarTagDao = calendarTagDao;
        this.visitDao = visitDao;
        this.routeSearchDao = routeSearchDao;
        this.favouritePlaceDao = favouritePlaceDao;

        this.calendar = new CalendarConnection(context);
    }

    /**
     * Returns the most probable destination, when the user is in startLocation.
     *
     * The algorithm will first check if there are marked events in the calendar within a few hours.
     * If none was found, it will then check places where the user has previously visited.
     * If that data is not available, previously used searches are suggested.
     * Lastly, it will check saved favorite location and suggest one of those.
     *
     * TODO: the algorithm shouldn't always take the first decent suggestion.
     * It should instead compare suggestions from all the different sources and take the best
     * one of those.
     *
     * @param startLocation Location where the user is starting
     * @return Most probable destination
     */
    public String getMostLikelyDestination(String startLocation) {
        this.latestStartLocation = startLocation;
        String nextDestination;

        String calendarSuggestion = searchFromCalendar();
        String visitsSuggestion = searchFromPreviousVisits();
        String routesSuggestion = searchFromUsedRoutes(startLocation);
        String favoritesSuggestion = searchFromFavorites();

        if (calendarSuggestion != null) {
            nextDestination = calendarSuggestion;
            calendarSource = true;
        }
        else if (visitsSuggestion != null) {
            nextDestination = visitsSuggestion;
            visitSource = true;
        }
        else if (routesSuggestion != null) {
            nextDestination = routesSuggestion;
            routeSource = true;
        }
        else if (favoritesSuggestion != null) {
            nextDestination = favoritesSuggestion;
            favouritesSource = true;
        }
        else {
            nextDestination = "Home";
            defaultSource = true;
        }

        latestDestination = nextDestination;
        return nextDestination;
    }

    /**
     * Returns a list of most probable destinations, when the user is in startLocation.
     *
     * The algorithm will first check if there are marked events in the calendar within a few hours.
     * It will then check previously used searches and places where the user has previously visited.
     * Lastly, it will check saved favorite locations and suggest one of those.
     *
     * TODO: the algorithm shouldn't always take the first decent suggestion.
     * It should instead compare suggestions from all the different sources and take the best
     * one of those.
     *
     * @param startLocation Location where the user is starting
     * @return List of most probable destinations
     */
    public ArrayList<String> getListOfMostLikelyDestinations(String startLocation) {
        ArrayList<String> nextDestinations = new ArrayList<>();
        this.latestStartLocation = startLocation;

        String calendarSuggestion = searchFromCalendar();
        String visitsSuggestion = searchFromPreviousVisits();
        String routesSuggestion = searchFromUsedRoutes(startLocation);
        String favoritesSuggestion = searchFromFavorites();

        if (calendarSuggestion != null) {
            nextDestinations.add(calendarSuggestion);
            calendarSource = true;
        }
        if (visitsSuggestion != null) {
            nextDestinations.add(visitsSuggestion);
            visitSource = true;
        }
        if (routesSuggestion != null) {
            nextDestinations.add(routesSuggestion);
            routeSource = true;
        }
        if (favoritesSuggestion != null) {
            nextDestinations.add(favoritesSuggestion);
            favouritesSource = true;
        }
        if (nextDestinations.isEmpty()) {
            nextDestinations.add("Home");
            defaultSource = true;
        }

        latestDestinations = nextDestinations;
        return nextDestinations;
    }

    /**
     * Returns the most probable destination from the calendar.
     *
     * @return Destination from calendar
     */
    private String searchFromCalendar() {
        String eventLocation = calendar.getEventLocation();

        if (eventLocation != null) {
            CalendarTag calendarTag = calendarTagDao.findTheMostUsedTag(eventLocation);
            if (calendarTag != null) {
                eventLocation = calendarTag.getValue();
            }
        }

        return eventLocation;
    }

    /**
     * Selects destination based on previously used routes.
     * Checks if the user has gone to some destination at the same time in the past,
     * max 2 hours earlier or max 2 hours later than current time.
     * Searches from previously used routes.
     *
     * @param startLocation Starting location
     * @return Previously used destination
     *
     */
    private String searchFromUsedRoutes(String startLocation) {
        List<RouteSearch> routes = routeSearchDao.getRouteSearchesByStartlocation(startLocation);

        for (RouteSearch route : routes) {
            if (aroundTheSameTime(new Time(route.getTimestamp()), 2, 2)) {
                return route.getDestination();
            }
        }

        return null;
    }

    /**
     * Checks if the selected location was visited or set as destination around the same time in the past,
     * max x hours earlier or max y hours later than current time.
     *
     * @param visitTime    timestamp of the location
     * @param hoursEarlier hours earlier than current time
     * @param hoursLater   hours later than current time
     * @return true if location was visited within the time frame, false if not.
     */
    private boolean aroundTheSameTime(Time visitTime, int hoursEarlier, int hoursLater) {
        Date currentTime = new Date(System.currentTimeMillis());

        int visitHour = visitTime.getHours();
        int visitMin = visitTime.getMinutes();
        int currentHour = currentTime.getHours();
        int currentMin = currentTime.getMinutes();

        if ((visitHour > currentHour - hoursEarlier || (visitHour == currentHour - hoursEarlier && visitMin >= currentMin))
                && (visitHour < currentHour + hoursLater || (visitHour == currentHour + hoursLater && visitMin <= currentMin))) {
            return true;
        }
        return false;
    }

    /**
     * Selects destination based on previous visits.
     * Checks if the user has visited some location around the same time in the past,
     * max 1 hour earlier or max 3 hours later than current time.
     * Searches from visits.
     *
     * @return Previously visited place
     */
    private String searchFromPreviousVisits() {
        List<Visit> visits = visitDao.getAllVisits();

        for (Visit visit : visits) {
            if (aroundTheSameTime(new Time(visit.getTimestamp()), 1, 3)) {
                return visit.getOriginalLocation();
                // This returns location with coordinates : "Liisankatu 1, Helsinki, Finland!00.0000!00.0000!"
                //return visit.getOriginalLocation() + "!" + visit.getLatitude() + "!" + visit.getLongitude();
            }
        }

        return null;
    }

    /**
     * Returns the first saved favorite place.
     *
     * @return First favorite place
     */
    private String searchFromFavorites() {
        List<FavouritePlace> favouritePlaces = favouritePlaceDao.findAllOrderByCounter();

        return favouritePlaces.isEmpty() ? null : favouritePlaces.get(favouritePlaces.size() - 1).getAddress();
        // TODO: Add some logic to choosing from favorite places.
        // E.g. add a counter that increases every time user uses the favorite place.
    }

    /**
     * Saves a calendar event.
     *
     * @param event an event
     */
    public void setCalendarEventLocation(String event) {
        calendar.setEventLocation(event);
    }

    /**
     * Returns the latest destination that was sent to the client.
     *
     * @return Latest given destination
     */
    public String getLatestDestination() {
        return latestDestination;
    }

    /**
     * Returns a list of the latest destinations that were sent to the client.
     * @return List of latest given destinations
     */
    public ArrayList<String> getListOfLatestDestinations() {
        return latestDestinations;
    }

    /**
     * Tells if the latest given location was retrieved from the calendar.
     *
     * @return True if the location was from calendar, false otherwise
     */
    public boolean isCalendarDestination() {
        return calendarSource;
    }
}
