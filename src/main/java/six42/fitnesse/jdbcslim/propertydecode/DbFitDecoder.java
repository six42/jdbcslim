package six42.fitnesse.jdbcslim.propertydecode;

import dbfit.util.crypto.CryptoFactories;
import dbfit.util.crypto.CryptoKeyStore;
import dbfit.util.crypto.CryptoService;

import java.io.File;

/**
 * Decodes encoded properties using DbFit's {@link dbfit.util.crypto.CryptoService}.
 */
public class DbFitDecoder extends AbstractKeyStoreBasedPropertyDecoder {
  private CryptoService cryptoService;

  @Override
  protected boolean canDecrypt() {
    return cryptoService != null;
  }

  @Override
  protected String decrypt(String encodedValue) {
    try {
      String decryptedValue = cryptoService.decrypt(encodedValue);
      return decryptedValue;
    } catch (Exception e) {
      throw new RuntimeException("decrypt of " + encodedValue + " failed. Wrong Key Store used?", e);
    }
  }

  @Override
  public void setKeyStore(File keyStoreFile) {
    if (keyStoreFile == null) {
      setCryptoService(null);
    } else {
      CryptoKeyStore keyStore = CryptoFactories.getCryptoKeyStoreFactory().newInstance(keyStoreFile);
      CryptoService crypto = CryptoFactories.getCryptoServiceFactory().getCryptoService(keyStore);
      setCryptoService(crypto);
    }
  }

  public void setCryptoService(CryptoService cryptoService) {
    this.cryptoService = cryptoService;
  }
}
