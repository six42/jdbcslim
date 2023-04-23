// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

public interface PropertiesInterface {

  public String getProperty(String propertyName);

  public String getProperty(ConfigurationParameters propertyName);

  public String getPropertyOrDefault(String propertyName, String defaultValue);

  public String getPropertyOrDefault(ConfigurationParameters propertyName, String defaultValue);

  public PropertiesLoader getSubProperties(String subPropertyName);

  public boolean isDebug();

  /**
   * Checks for encrypted properties and returns
   * then unencrypted values. If no encrypted property exists
   * return the value of a not encrypted property
   *
   * @param propertyName
   * @return
   */
  public String getSecretProperty(String propertyName);

  public String getSecretProperty(ConfigurationParameters dbpassword);

  public boolean getBooleanPropertyOrDefault(
    ConfigurationParameters outputflagunusedinputcolumns, boolean flagUnused);
}