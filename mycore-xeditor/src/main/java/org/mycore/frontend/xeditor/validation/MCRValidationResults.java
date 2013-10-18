package org.mycore.frontend.xeditor.validation;

import java.util.HashMap;
import java.util.Map;

import org.mycore.common.MCRConfiguration;

public class MCRValidationResults {

    private static final String MARKER_DEFAULT;

    private static final String MARKER_SUCCESS;

    private static final String MARKER_ERROR;

    static {
        String prefix = "MCR.XEditor.Validation.Marker.";
        MARKER_DEFAULT = MCRConfiguration.instance().getString(prefix + "default", "");
        MARKER_SUCCESS = MCRConfiguration.instance().getString(prefix + "success", "has-success");
        MARKER_ERROR = MCRConfiguration.instance().getString(prefix + "error", "has-error");
    }

    private Map<String, String> xPath2Marker = new HashMap<String, String>();

    private Map<String, MCRValidationRule> xPath2FailedRule = new HashMap<String, MCRValidationRule>();

    private boolean isValid = true;

    public boolean hasError(String xPath) {
        return MARKER_ERROR.equals(xPath2Marker.get(xPath));
    }

    public void mark(String xPath, boolean isValid, MCRValidationRule failedRule) {
        if (hasError(xPath))
            return;

        if (isValid)
            xPath2Marker.put(xPath, MARKER_SUCCESS);
        else {
            xPath2Marker.put(xPath, MARKER_ERROR);
            xPath2FailedRule.put(xPath, failedRule);
            this.isValid = false;
        }
    }

    public String getValidationMarker(String xPath) {
        return xPath2Marker.containsKey(xPath) ? xPath2Marker.get(xPath) : MARKER_DEFAULT;
    }

    public MCRValidationRule getFailedRule(String xPath) {
        return xPath2FailedRule.get(xPath);
    }

    public boolean isValid() {
        return isValid;
    }
}
