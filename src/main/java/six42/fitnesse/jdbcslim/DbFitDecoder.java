package six42.fitnesse.jdbcslim;

import dbfit.util.crypto.CryptoFactories;
import dbfit.util.crypto.CryptoKeyStore;
import dbfit.util.crypto.CryptoService;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decodes encoded properties using DbFit's {@link dbfit.util.crypto.CryptoService}.
 */
public class DbFitDecoder implements PropertyDecoder {
    private CryptoService cryptoService;
    private boolean debugFlag = false;

    @Override
    public Map<String, String> process(Map<String, String> props) {
        setDebugFromProperties(props);
        configure(props);

        if (cryptoService == null) {
            return props;
        } else {
            return processProperties(props);
        }
    }

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

    protected boolean configureBasedOn(String key, String value) {
        boolean removeKey = false;
        if (key.equalsIgnoreCase(".keyStoreLocation")) {
            if (value.equals("")) {
                setCryptoService(null);
            } else {
                File keyStoreFile = new File(value);
                setKeyStore(keyStoreFile);
                if (isDebug()) {
                    System.out.println("New Key Store Location " + keyStoreFile.getAbsolutePath());
                }
            }
            removeKey = true;
        }
        return removeKey;
    }

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

    protected String decrypt(String encodedValue) {
        try {
            String decryptedValue = cryptoService.decrypt(encodedValue);
            return decryptedValue;
        } catch (Exception e) {
            throw new RuntimeException("decrypt of " + encodedValue + " failed. Wrong Key Store used?", e);
        }
    }

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

    public void setKeyStore(File keyStoreFile) {
        CryptoKeyStore keyStore = CryptoFactories.getCryptoKeyStoreFactory().newInstance(keyStoreFile);
        CryptoService crypto = CryptoFactories.getCryptoServiceFactory().getCryptoService(keyStore);
        setCryptoService(crypto);
    }

    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
}
