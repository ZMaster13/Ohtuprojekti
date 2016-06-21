package fi.ohtu.mobilityprofile.data;

import com.orm.SugarRecord;

public class Visit extends SugarRecord {

    long timestamp;
    String originallocation;             // Accurate point the user visited.
    UserLocation nearestknownlocation;   // Closest known nearestknownlocation that is within 50 meters (value may change) from the actual nearestknownlocation.

    /**
     *
     */
    public Visit() {
    }

    /**
     * @param timestamp
     * @param originallocation
     */
    public Visit(long timestamp, String originallocation) {
        this.timestamp = timestamp;
        this.originallocation = originallocation;
        this.nearestknownlocation = null;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getOriginaLocation() {
        return originallocation;
    }

    public UserLocation getNearestKnownLocation() {
        return nearestknownlocation;
    }
}
