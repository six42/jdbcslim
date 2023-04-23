// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SheetCommandBase implements SheetCommandInterface {

  private String command = "";
  protected SheetFixture myFixture;
  protected String rawResult = null;
  protected List<List<String>> resultSheet;
  protected boolean success = true;
  private PropertiesLoader loader = new PropertiesLoader();


  public SheetCommandBase(String configurationOptions, String rawCommand, String outputFormatOptions) throws FileNotFoundException, IOException {
    parseConfiguration(configurationOptions);
    if (outputFormatOptions != null) {
      parseConfigurationString(outputFormatOptions);
    }
    String commandToUse = getCommandIfMissing(configurationOptions, rawCommand);
    setCommand(commandToUse);
    myFixture = new SheetFixture(command(), this);
  }

  public SheetCommandBase(String configurationOptions, String rawCommand) throws FileNotFoundException, IOException {
    this(configurationOptions, rawCommand, null);
  }

  public SheetCommandBase(String configurationOptions) throws FileNotFoundException, IOException {
    this(configurationOptions, null);
  }


  private String getCommandIfMissing(String propertiesFile, String rawCommand) {
    if (rawCommand == null || rawCommand.isEmpty()) {
      rawCommand = loader.getProperty("cmd");
      if (rawCommand == null) {
        throw new RuntimeException("Mandatory parameter 'cmd' not found in properties  (" + propertiesFile + ").");
      }
    }
    return rawCommand;
  }


  private void parseConfiguration(String configurationOptions) throws FileNotFoundException, IOException {

    loader.loadFromDefintionOrFile(configurationOptions);

  }

  private void parseConfigurationString(String configurationOptions) throws FileNotFoundException, IOException {

    loader.loadFromString(configurationOptions);
  }


  public List<?> doTable(List<List<String>> parameterTable) {
    try {
      // Always do this
      return myFixture.doTable(parameterTable);
    } catch (Exception e) {
      throw new RuntimeException("message:<<" + e.getMessage() + "\nResult: " + rawResult() + ">>", e);
    }
  }

  @Override
  public void setCommand(String Command) {
    this.command = HtmlCleaner.cleanupPreFormatted(Command);
  }

  @Override
  public void execute() {
    // This is an implementation just for demonstration and testing purposes
    resultSheet = loader.toTable();

    success = true;
  }

  @Override
  public void execute(String Command) {

    setCommand(Command);
    execute();
  }

  @Override
  public boolean run(String Command) {
    beginTable();
    boolean result = false;
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
    // Nothing to be done

  }

  @Override
  public void table(List<List<String>> table) {
    // Nothing to be done

  }

  @Override
  public void beginTable() {
    // Nothing to be done

  }

  @Override
  public void endTable() {
    // Nothing to be done

  }

  @Override
  public boolean success() {
    return this.success;
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
    return loader;
  }

  public String getCellValue(int row, int column) {
    return this.resultSheet.get(row)
      .get(column);
  }

  public String getColumnValue(int column) {
    return this.resultSheet.get(1)
      .get(column);
  }

  public String getColumnValueByName(String columnName) {
    return getColumnValueByNameFromRow(columnName, 1);
  }

  public String getColumnValueByNameFromRow(String columnName, int row) {
    List<String> Data = getRowValues(row);
    List<String> Header = this.resultSheet.get(0);

    for (int i = 0; i < Header.size(); i++) {
      if (HeaderLine.isHeaderNameEqual(Header.get(i), columnName)) return Data.get(i);
    }
    throw new RuntimeException("Column not found   (" + columnName + ").");
  }

  public int getRowCount() {
    List<List<String>> sheet = this.resultSheet;


    return sheet == null || sheet.isEmpty() ? 0 : sheet.size() - 1;
  }

  public List<String> getRowValues(int row) {
    validateRowIndex(row);
    return this.resultSheet.get(row);
  }

  public Map<String, String> getRow(int row) {
    Map<String, String> result = new LinkedHashMap<String, String>();
    validateRowIndex(row);
    List<List<String>> sheet = resultSheet();
    List<String> headers = sheet.get(0);
    List<String> values = sheet.get(row);
    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);
      String value = values.get(i);
      result.put(header, value);
    }
    return result;
  }

  private void validateRowIndex(int row) {
    int rowCount = getRowCount();
    if (rowCount < row) {
      throw new RuntimeException("No row: " + row + ". Rowcount was: " + rowCount);
    }
  }

  public void ResetConfiguration(String configurationOptions) throws FileNotFoundException, IOException {

    loader = new PropertiesLoader();
    loader.loadFromDefintionOrFile(configurationOptions);

  }

  public void ResetConfigurationFromString(String configurationOptions) throws FileNotFoundException, IOException {

    loader = new PropertiesLoader();
    loader.loadFromString(configurationOptions);
  }


  public List<?> compareSheet(List<List<String>> parameterTable) {
    // Always do this
    return doTable(parameterTable);
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
