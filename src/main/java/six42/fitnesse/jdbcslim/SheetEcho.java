// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.List;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SheetEcho implements SheetCommandInterface {

  protected String command = "";
  protected SheetFixture myFixture;
  protected String rawResult = null;
  protected List<List<String>> resultSheet;
  protected boolean success = true;
  private PropertiesLoader loader = new PropertiesLoader();


  public SheetEcho(List<List<String>> ParameterTable) throws FileNotFoundException, IOException {
    loader.loadFromString(ConfigurationParameters.QUERY.toString());
    resultSheet = ParameterTable;
    myFixture = new SheetFixture("", this);
  }

  public List<?> doTable(List<List<String>> ParameterTable) {
    // Always do this
    return myFixture.doTable(ParameterTable);
  }

  @Override
  public void setCommand(String Command) {
    // TODO Auto-generated method stub

  }

  @Override
  public void execute() {
    // Nothing must be done
    success = true;

  }

  @Override
  public void execute(String Command) {
    setCommand(Command);
    execute();

  }

  @Override
  public boolean run(String Command) {
    boolean result = false;
    beginTable();
    try {
      execute(Command);
      result = success();
    } finally {
      endTable();
    }
    return result;
  }

  @Override
  public void reset() {
    // TODO Auto-generated method stub

  }

  @Override
  public void table(List<List<String>> table) {
    // TODO Auto-generated method stub

  }

  @Override
  public void beginTable() {
    // TODO Auto-generated method stub

  }

  @Override
  public void endTable() {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean success() {
    return success;
  }

  @Override
  public String rawResult() {
    if (rawResult != null) return this.rawResult;
    else return this.resultSheet.toString();
  }

  @Override
  public String command() {
    return this.command;
  }

  @Override
  public List<List<String>> resultSheet() {
    return this.resultSheet;
  }

  @Override
  public PropertiesInterface Properties() {
    return this.loader;
  }

  @Override
  public void set(String columnName, String value) {
    // TODO Auto-generated method stub

  }

  @Override
  public String get(String columnName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean containsKey(String columnName) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Set<String> getUsedColumnNames() {
    // TODO Auto-generated method stub
    return null;
  }

}
