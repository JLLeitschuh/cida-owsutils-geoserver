package gov.usgs.cida.geoutils.geoserver.servlet;

import static gov.usgs.cida.geoutils.geoserver.servlet.GeoServerAwareServlet.defaultWorkspaceName;
import gov.usgs.cida.owsutils.commons.communication.RequestResponse;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
        
        GeoServerRESTPublisher publisher;
        publisher = (GeoServerRESTPublisher) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{GeoServerRESTPublisher.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if(method.getName().equals("recalculateFeatureTypeBBox")){
                            return this.recalculateFeatureTypeBBox((String)args[0], (String)args[1], (String)args[2], (BBoxRecalculationMode)args[3], (boolean)args[4]);
                            
                        } else {
                            return method.invoke(proxy, args);
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
                     * @return true if successful, false otherwise
                     */
                    public boolean recalculateFeatureTypeBBox(String workspace, String storeName, String layerName, BBoxRecalculationMode calculationMode, boolean enabled) {
                        String baseUrl = geoserverEndpoint + "/rest/workspaces/" + workspace + "/"
                                + "datastores/" + storeName + "/"
                                + "featuretypes/"
                                + layerName + ".xml" ;
                        
                        String sUrl = baseUrl + "?recalculate=" + calculationMode.getParamValue();
                        LOG.debug("Constructed the following url for bounding box recalculation: " + sUrl);
                        String xmlElementName = "featureType";
                        String body = "<" + xmlElementName +"><name>" + layerName + "</name>" + 
                                "<enabled>" + enabled + "</enabled></" + xmlElementName + ">";
                        
                        return false;
                    }
                });
        
        boolean success = publisher.recalculateFeatureTypeBBox(workspaceName, storeName, layerName, BBoxRecalculationMode.NATIVE_AND_LAT_LON_BBOX, true);
        if(success) {
            responseMap.put("success", "bounding box recalcuated");
            RequestResponse.sendSuccessResponse(response, responseMap, responseType);
        } else {
            responseMap.put("error", "Error recalculating bounding box.");
            RequestResponse.sendErrorResponse(response, responseMap, responseType);
        }
    }
}
