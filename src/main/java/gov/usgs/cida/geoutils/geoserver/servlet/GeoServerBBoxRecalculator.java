package gov.usgs.cida.geoutils.geoserver.servlet;



public interface GeoServerBBoxRecalculator {


   /**
     * Recalculate a bounding box for a feature type
     * @param workspace
     * @param storeName
     * @param layerName
     * @param calculationMode
     * @param enabled
     * @return true if successful, false otherwise
     */
    public boolean recalculateFeatureTypeBBox(String workspace, String storeName, String layerName, BBoxRecalculationMode calculationMode, boolean enabled);
}
