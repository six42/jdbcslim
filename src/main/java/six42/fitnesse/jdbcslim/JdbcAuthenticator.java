package six42.fitnesse.jdbcslim;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.authentication.Authenticator;
import fitnesse.authentication.HashingCipher;
import fitnesse.authentication.PasswordCipher;
import fitnesse.components.ComponentInstantiationException;
import fitnesse.html.template.HtmlPage;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;

/**
 * <p><strong>jdbc authenticator</strong></p>
 * <p>Authenticates FitNesse users via a login in a database.
 * All users which have a valid login in a database can also access the FitNesse Wiki.</p>
 * <p><strong>How to enable?</strong></p>
 * <p>Enable this plugin by editing plugins.properties and adding the line:</p>
 * <pre>
 * Authenticator = six42.fitnesse.jdbcslim.jdbcAuthenticator
 * jdbcAuthenticator.properties = <file name with database properties>
 * </pre>
*/

public class JdbcAuthenticator extends Authenticator {
  private final String propertiesFileName = "JdbcAuthenticator.properties";
  private final String dbUrl;
  private Properties dbProperties;
  
  private PwCache pwCache = new PwCache();
  
  private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(JdbcAuthenticator.class.getName());

  public JdbcAuthenticator() {
    String jdbcDriver;
    
    dbProperties = new Properties();
    try {
      dbProperties.load(new FileInputStream(new File(propertiesFileName)));
    } catch (FileNotFoundException e) {
      dbProperties = null;
      throw new ComponentInstantiationException("Failed to load properties required for JdbcAuthenticator '" + propertiesFileName + "'", e);
    } catch (IOException e) {
      dbProperties = null;
      throw new ComponentInstantiationException("Failed to load properties required for JdbcAuthenticator '" + propertiesFileName + "'", e);
    }
    jdbcDriver = dbProperties.getProperty("JDBCDRIVER");
    if(jdbcDriver == null) jdbcDriver = "";
    try {
      Class.forName(jdbcDriver);
    } catch (ClassNotFoundException e1) {
      dbProperties = null;
      throw new ComponentInstantiationException("Failed to load JDBCDRIVER required for JdbcAuthenticator '" + jdbcDriver + "'", e1);
      //LOG.severe("Failed to load jdbc driver required for Authenticator '" + jdbcDriver + "'");
      //LOG.severe(e1.toString());
    }
    dbUrl = dbProperties.getProperty("DBURL");
     if(dbUrl == null)  throw new RuntimeException("DBURL not defined. Required for JdbcAuthenticator. Check config file '" + propertiesFileName + "'");

  }

  @Override
  protected Responder unauthorizedResponder(FitNesseContext context, Request request) {
    return new UnauthorizedResponder(dbUrl);
  }
  
  @Override
  public boolean isAuthenticated(String username, String password) {
    if (username == null || password == null || username.isEmpty() || password.isEmpty()){
      return false;
    }
    if(dbProperties==null) return false;

    if(pwCache.hasUser(username, password)) return true;

    try {
      dbProperties.setProperty("USER", username);
      dbProperties.setProperty("PASSWORD", password);
      Connection dbConnection= DriverManager.getConnection(dbUrl, dbProperties);
      dbConnection.close();
      LOG.fine("Authenticated User '"+ username + "'");
      pwCache.addUser(username, password);
      return true;
    }catch (SQLException e) {
      LOG.warning("Failed to authenticate User '"+ username + "'");
      LOG.warning(e.toString() + e.getMessage());
      return false;
    }finally{
      // Clear the pw and user in memory
      dbProperties.setProperty("PASSWORD", "");
      dbProperties.setProperty("USER", "");
    }
  }

  private class PwCache{
    private class PwCacheEntry{
      public String password;
      public long creationTime;
      
      public PwCacheEntry( String password){
        this.password = password;
        this.creationTime = System.currentTimeMillis();
      }
    }

    private Map<String, PwCacheEntry> passwordMap = new HashMap<String, PwCacheEntry>();
    private PasswordCipher cipher = new HashingCipher();
    private long timeout = 1000*60*20;
    
    public boolean hasUser(String username, String password) {
      PwCacheEntry c = passwordMap.get(username);
      if(c != null){
        if (System.currentTimeMillis()-  c.creationTime < timeout && c.password.equals(cipher.encrypt(password))){
          LOG.finest("Authenticated User from Cache '"+ username + "'");
          return true;
        }
        else{
          passwordMap.remove(username);
          LOG.fine("Removed User from Cache '"+ username + "'");
          return false;
        }
      }
      return false;
    }

    public void addUser(String username, String password) {
      passwordMap.put(username, new PwCacheEntry(cipher.encrypt(password)));
    }
  }

  private class UnauthorizedResponder implements Responder {
    private final String realm;
    
    public UnauthorizedResponder(String realm) {
      super();
      this.realm = realm;
    }

    @SuppressWarnings("unused")
    public UnauthorizedResponder() {
      this("FitNesse");
    }

    @Override
    public Response makeResponse(FitNesseContext context, Request request) {
      SimpleResponse response = new SimpleResponse(401);
      response.addHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");

      HtmlPage page = context.pageFactory.newPage();
      page.addTitles("401 Unauthorized");
      page.put("resource", request.getResource());
      page.setMainTemplate("unauthorized.vm");
      response.setContent(page.html());

      return response;
    }
  }
}
