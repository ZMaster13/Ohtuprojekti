package fi.ohtu.mobilityprofile.suggestions.locationHistory;

import java.util.ArrayList;
import java.util.List;

import fi.ohtu.mobilityprofile.data.PlaceDao;
import fi.ohtu.mobilityprofile.suggestions.Suggestion;
import fi.ohtu.mobilityprofile.suggestions.SuggestionSource;

/**
 * This class creates suggestions based on data collected about user's movement and visited places.
 */
public class VisitSuggestions implements SuggestionSource {
    private PlaceDao placeDao;

    /**
     * Creates the VisitSuggestions.
     *
     * @param placeDao DAO for visits user has made
     */
    public VisitSuggestions(PlaceDao placeDao) {
        this.placeDao = placeDao;
    }

    /**
     * Returns a list of probable locations the user could want to visit based on the data collected
     * from previous visits. A visit is considered valid if it was max 1 hour earlier or max 3 hours
     * later that the current time.
     *
     * @return List of probable destinations
     */
    @Override
    public List<Suggestion> getSuggestions(String startLocation) {
        List<Suggestion> suggestions = new ArrayList<>();

//        for (Place place : placeDao.getAll()) {
//            if (Util.aroundTheSameTime(new Time(place.getTimestamp()), 1, 3)) {
//                Suggestion suggestion = new Suggestion(place.getOriginalLocation(), SuggestionAccuracy.HIGH, VISIT_SUGGESTION);
//                suggestions.add(suggestion);
//                // This returns location with coordinates : "Liisankatu 1, Helsinki, Finland!00.0000!00.0000!"
//            }
//        }

        return suggestions;
    }
}
