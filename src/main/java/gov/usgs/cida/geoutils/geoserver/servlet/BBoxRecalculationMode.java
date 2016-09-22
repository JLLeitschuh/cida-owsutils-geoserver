package gov.usgs.cida.geoutils.geoserver.servlet;

/**
 * Defines multiple modes of recalculating bounding boxes.
 *
 * @author Carl Schroedl - cschroedl@usgs.gov
 */
public enum BBoxRecalculationMode {
    /**
     * Recalculate the native bounding box, but do not recalculate the lat/long
     * bounding box.
     */
    NATIVE_BBOX("nativebbox"),
    /**
     * Recalculate both the native bounding box and the lat/long bounding box.
     */
    NATIVE_AND_LAT_LON_BBOX("nativebbox,latlonbbox"),
    /**
     * Do not calculate any fields, regardless of the projection, projection
     * policy, etc. This might be useful to avoid slow recalculation when
     * operating against large datasets.
     */
    NONE(""),;

    private final String paramValue;

    /**
     * Associates the enum value with a URL query string parameter value
     *
     * @param paramValue
     */
    BBoxRecalculationMode(String paramValue) {
        this.paramValue = paramValue;
    }

    /**
     * Get the URL param value
     *
     * @return The query string parameter value
     */
    public String getParamValue() {
        return paramValue;
    }
}
