package gov.usgs.cida.geoutils.geoserver.servlet;

import static gov.usgs.cida.geoutils.geoserver.servlet.GeoServerAwareServlet.defaultWorkspaceName;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;


public class BoundingBoxRecalculationServlet extends GeoServerAwareServlet {

    private static final long serialVersionUID = 2L;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BoundingBoxRecalculationServlet.class);
    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
    }
    
     @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, FileNotFoundException {
        doPost(request, response);
    }
    
     @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, FileNotFoundException {
        
        Map<String, String> responseMap = new HashMap<>();
        String layerName = request.getParameter("layer");
        String workspaceName = request.getParameter("workspace");
        
        RequestResponse.ResponseType responseType = RequestResponse.ResponseType.XML;
        String responseEncoding = request.getParameter("response.encoding");
        if (StringUtils.isBlank(responseEncoding) || responseEncoding.toLowerCase(Locale.getDefault()).contains("json")) {
            responseType = RequestResponse.ResponseType.JSON;
        }
        LOG.debug("Response type set to " + responseType.toString());
        
        if (StringUtils.isBlank(workspaceName)) {
            workspaceName = defaultWorkspaceName;
        }
        LOG.debug("Layer name set to " + layerName);
        
        if (StringUtils.isBlank(workspaceName)) {
            responseMap.put("error", "Parameter \"workspace\" is mandatory");
            RequestResponse.sendErrorResponse(response, responseMap, responseType);
            return;
        }
        LOG.debug("Workspace name set to " + workspaceName);
        
        String storeName = request.getParameter("store");
        if (StringUtils.isBlank(storeName)) {
            storeName = defaultStoreName;
        }
        if (StringUtils.isBlank(storeName)) {
            storeName = layerName;
        }
        LOG.debug("Store name set to " + storeName);
        
        //regardless of recalculation success or failure, include debug info
        responseMap.put("layer", layerName);
        responseMap.put("workspace", workspaceName);
        responseMap.put("store", storeName);
        
        try{
            String message = recalculateFeatureTypeBBox(workspaceName, storeName, layerName, BBoxRecalculationMode.NATIVE_AND_LAT_LON_BBOX, true);
            responseMap.put("success", "bounding box recalcuated");
            responseMap.put("geoServerMessage", message);
            RequestResponse.sendSuccessResponse(response, responseMap, responseType);
        } catch(RuntimeException e){
            responseMap.put("error", "Error recalculating bounding box.");
            responseMap.put("geoServerMessage", e.getMessage());
            RequestResponse.sendErrorResponse(response, responseMap, responseType);
        }
    }
    /**
     * Recalculate a bounding box for a feature type
     *
     * @param workspace
     * @param storeName
     * @param layerName
     * @param calculationMode
     * @param enabled
     * @return the response body if successful
     * @throws RuntimeException if the request to GeoServer fails
     */
    public String recalculateFeatureTypeBBox(String workspace, String storeName, String layerName, BBoxRecalculationMode calculationMode, boolean enabled) {
        String baseUrl = geoserverEndpoint + "/rest/workspaces/" + workspace + "/"
                + "datastores/" + storeName + "/"
                + "featuretypes/"
                + layerName + ".xml";

        String sUrl = baseUrl + "?recalculate=" + calculationMode.getParamValue();
        LOG.debug("Constructed the following url for bounding box recalculation: " + sUrl);
        String xmlElementName = "featureType";
        String body = "<" + xmlElementName + "><name>" + layerName + "</name>"
                + "<enabled>" + enabled + "</enabled></" + xmlElementName + ">";
        
        HttpClient client = new HttpClient();
        
        //AuthScope.ANY is ok in this case because the target for the request
        //is read from config files, not from the user of this servlet.
        client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(geoserverUsername, geoserverPassword));
        
        PutMethod put = new PutMethod(sUrl);
        
        RequestEntity entity;
        try {
            entity = new StringRequestEntity(body, "application/xml", "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        put.setRequestEntity(entity);
        put.setDoAuthentication(true);
        
        String response = null;
        
        try{
            int statusCode = client.executeMethod(put);
            response = put.getResponseBodyAsString();
            if(statusCode != 200){
                throw new RuntimeException(response);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            put.releaseConnection();
        }
        
        return response;
    }
}
