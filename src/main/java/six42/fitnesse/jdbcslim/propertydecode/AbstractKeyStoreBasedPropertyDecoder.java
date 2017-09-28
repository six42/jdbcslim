package six42.fitnesse.jdbcslim.propertydecode;

import java.io.File;

/**
 * Base class for property decoder using key store file.
 */
public abstract class AbstractKeyStoreBasedPropertyDecoder extends AbstractPropertyDecoder {
    protected boolean configureBasedOn(String key, String value) {
        boolean removeKey = false;
        if (key.equalsIgnoreCase(".keyStoreLocation")) {
            if (value.equals("")) {
                setKeyStore(null);
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

    protected abstract void setKeyStore(File keyStore);
}
