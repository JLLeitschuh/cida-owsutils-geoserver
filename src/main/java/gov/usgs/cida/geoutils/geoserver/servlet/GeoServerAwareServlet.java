package gov.usgs.cida.geoutils.geoserver.servlet;

import gov.usgs.cida.config.DynamicReadOnlyProperties;
import gov.usgs.cida.owsutils.commons.properties.JNDISingleton;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

public abstract class GeoServerAwareServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(GeoServerAwareServlet.class);
    protected static DynamicReadOnlyProperties props = null;
    protected static String applicationName;
    protected static Integer maxFileSize;
    protected static String geoserverEndpoint;
    protected static URL geoserverEndpointURL;
    protected static String geoserverUsername;
    protected static String geoserverPassword;
    protected static GeoServerRESTManager gsRestManager;
    // Defaults
    protected static String defaultWorkspaceName;
    protected static String defaultStoreName;
    protected static String defaultSRS;
    protected static String defaultFilenameParam = "qqfile"; // Legacy to handle jquery fineuploader
    protected static Integer defaultMaxFileSize = Integer.MAX_VALUE;
    protected static boolean defaultUseBaseCRSFallback = true;
    protected static boolean defaultOverwriteExistingLayer = false;
    protected static ProjectionPolicy defaultProjectionPolicy = ProjectionPolicy.REPROJECT_TO_DECLARED;
    protected static ServletConfig servletConfig;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init();
        GeoServerAwareServlet.servletConfig = servletConfig;
        props = JNDISingleton.getInstance();

        applicationName = servletConfig.getInitParameter("application.name");

        Enumeration<String> initParameterNames = servletConfig.getInitParameterNames();
        while (initParameterNames.hasMoreElements()) {
            String initPKey = initParameterNames.nextElement();
            if (StringUtils.isBlank(props.getProperty(applicationName + "." + initPKey))) {
                String initPVal = servletConfig.getInitParameter(initPKey);
                if (StringUtils.isNotBlank(initPVal)) {
                    LOG.debug("Could not find JNDI property for " + applicationName + "." + initPKey + ". Substituting the value from web.xml");
                    props.setProperty(applicationName + "." + initPKey, initPVal);
                }
            }
        }

        // The maximum upload file size allowd by this server, 0 = Integer.MAX_VALUE
        String mfsJndiProp = props.getProperty(applicationName + ".max.upload.file.size");
        if (StringUtils.isNotBlank(mfsJndiProp)) {
            maxFileSize = Integer.parseInt(mfsJndiProp);
        } else {
            maxFileSize = defaultMaxFileSize;
        }
        if (maxFileSize == 0) {
            maxFileSize = defaultMaxFileSize;
        }
        LOG.debug("Maximum allowable file size set to: " + maxFileSize + " bytes");

        String gsepJndiProp = props.getProperty(applicationName + ".geoserver.endpoint");
        if (StringUtils.isNotBlank(gsepJndiProp)) {
            geoserverEndpoint = gsepJndiProp;
            if (geoserverEndpoint.endsWith("/")) {
                geoserverEndpoint = geoserverEndpoint.substring(0, geoserverEndpoint.length() - 1);
            }
        } else {
            throw new ServletException("Geoserver endpoint is not defined.");
        }
        LOG.debug("Geoserver endpoint set to: " + geoserverEndpoint);

        try {
            geoserverEndpointURL = new URL(geoserverEndpoint);
        } catch (MalformedURLException ex) {
            throw new ServletException("Geoserver endpoint (" + geoserverEndpoint + ") could not be parsed into a valid URL.");
        }

        String gsuserJndiProp = props.getProperty(applicationName + ".geoserver.username");
        if (StringUtils.isNotBlank(gsuserJndiProp)) {
            geoserverUsername = gsuserJndiProp;
        } else {
            throw new ServletException("Geoserver username is not defined.");
        }
        LOG.debug("Geoserver username set to: " + geoserverUsername);

        // This should only be coming from JNDI or JVM properties
        String gspassJndiProp = props.getProperty(applicationName + ".geoserver.password");
        if (StringUtils.isNotBlank(gspassJndiProp)) {
            geoserverPassword = gspassJndiProp;
        } else {
            throw new ServletException("Geoserver password is not defined.");
        }
        LOG.debug("Geoserver password is set");

        try {
            gsRestManager = new GeoServerRESTManager(geoserverEndpointURL, geoserverUsername, geoserverPassword);
        } catch (IllegalArgumentException ex) {
            throw new ServletException("Geoserver manager count not be built", ex);
        }

        String dwJndiProp = props.getProperty(applicationName + ".default.upload.workspace");
        if (StringUtils.isNotBlank(dwJndiProp)) {
            defaultWorkspaceName = dwJndiProp;
        } else {
            defaultWorkspaceName = "";
            LOG.warn("Default workspace is not defined. If a workspace is not passed to during the request, the request will fail.");
        }
        LOG.debug("Default workspace set to: " + defaultWorkspaceName);

        String dsnJndiProp = props.getProperty(applicationName + ".default.upload.storename");
        if (StringUtils.isNotBlank(dsnJndiProp)) {
            defaultStoreName = dsnJndiProp;
        } else {
            defaultStoreName = "";
            LOG.warn("Default store name is not defined. If a store name is not passed to during the request, the name of the layer will be used as the name of the store");
        }
        LOG.debug("Default store name set to: " + defaultStoreName);

        String dsrsJndiProp = props.getProperty(applicationName + ".default.srs");
        if (StringUtils.isNotBlank(dsrsJndiProp)) {
            defaultSRS = dsrsJndiProp;
        } else {
            defaultSRS = "";
            LOG.warn("Default SRS is not defined. If a SRS name is not passed to during the request, the request will fail");
        }
        LOG.debug("Default SRS set to: " + defaultSRS);
    }

}
