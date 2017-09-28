package six42.fitnesse.jdbcslim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for property decoder.
 */
public abstract class AbstractPropertyDecoder implements PropertyDecoder {
    private boolean debugFlag = false;

    @Override
    public Map<String, String> process(Map<String, String> props) {
        setDebugFromProperties(props);
        configure(props);

        if (canDecrypt()) {
            return processProperties(props);
        } else {
            return props;
        }
    }

    protected abstract boolean canDecrypt();

    protected void configure(Map<String, String> props) {
        List<String> keysToRemove = new ArrayList<String>(1);
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String key = entry.getKey();
            if (configureBasedOn(key, entry.getValue())) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            props.remove(key);
        }
    }

    protected abstract boolean configureBasedOn(String key, String value);

    protected Map<String, String> processProperties(Map<String, String> props) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String encodedValue = PropertiesLoader.unwrapEncryptedValue(value);
            if (encodedValue != null) {
                value = decrypt(encodedValue);
            }
            result.put(key, value);
        }
        return result;
    }

    protected abstract String decrypt(String encodedValue);

    protected void setDebugFromProperties(Map<String, String> props) {
        String debugProp = props.get(ConfigurationParameters.DEBUG.toString().toLowerCase());
        if (debugProp != null && !"false".equalsIgnoreCase(debugProp)) {
            setDebug(true);
        }
    }

    public boolean isDebug(){
        return debugFlag;
    }

    public void setDebug(boolean debugFlag){
        this.debugFlag = debugFlag;
    }
}
