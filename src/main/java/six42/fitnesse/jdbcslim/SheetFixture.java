// Copyright (C) 2015 by six42, All rights reserved. Contact the author via http://github.com/six42
package six42.fitnesse.jdbcslim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.slim.results.SlimTestResult;

public class SheetFixture {

  private String _rawCommand = "";
  private final String _pre_fix = "%";
  private final String _post_fix = "%";
  private final String _nullStrOut;
  private final String _nullStrIn;
  private SheetCommandInterface commandExecuter;
  private RetryManager retry = null;

  public SheetFixture(String rawCommand, SheetCommandInterface commandExecuter) {

    this._rawCommand = rawCommand;
    this.commandExecuter = commandExecuter;
    _nullStrOut = commandExecuter.Properties().getPropertyOrDefault(ConfigurationParameters.outputNullString, "#null#");
    _nullStrIn = commandExecuter.Properties().getPropertyOrDefault(ConfigurationParameters.inputNullString, _nullStrOut);
  }


  public List<List<String>> doTable(List<List<String>> parameterTable) {
    List<List<String>> result = null;
    List<String> header = null;
    boolean hasParameters = false;
    boolean hasHeader = (parameterTable.size() >= 1);


    if (hasHeader) {
      header = parameterTable.get(0);
      hasParameters = HeaderLine.hasHeaderParameters(header);
    }

    // If the output contains QUERY than we execute as a single query and don't check for parameters
    if (commandExecuter.Properties().getProperty("query") != null) hasParameters = false;

    List<List<String>> expectedTable = replaceNullInExpected(parameterTable);

    commandExecuter.table(expectedTable);
    commandExecuter.beginTable();


    for (retry = new RetryManager(commandExecuter.Properties().getPropertyOrDefault(ConfigurationParameters.RETRY, "")); retry.shouldTryAgain(); ) {
      if (!hasParameters) {
        // Select Table Command
        result = processCommandOnceAndCompareToExpected(expectedTable, hasHeader);

      } else {

        result = processCommandLineByLine(expectedTable, header);
      }
    }
    if (commandExecuter.Properties().isDebug()) System.out.println(retry.toString());

    // Add one empty line at the end to program around a bug in the table fixture
    // Likely extra logic required until Fitnesse#1055 is merged
    result.add(new ArrayList<String>());

    commandExecuter.endTable();

    return result;
  }


  protected List<List<String>> processCommandOnceAndCompareToExpected(
    List<List<String>> parameterTable, boolean hasHeader) {

    List<List<String>> result = new ArrayList<List<String>>();

    commandExecuter.reset();

    commandExecuter.execute(_rawCommand);

    if (commandExecuter.success()) {
      if (!hasHeader) {
        result = commandExecuter.resultSheet();
      } else {
        // TODO If expected has a header than check the order and values of the same
        result = compareTableWithSort(parameterTable, commandExecuter.resultSheet(), (commandExecuter.Properties().getProperty("sort") != null));
      }
    } else {
      List<String> line = new ArrayList<String>();
      line.add("fail: " + commandExecuter.rawResult());
      result.add(line);

    }
    return result;
  }


  private List<List<String>> processCommandLineByLine(
    List<List<String>> parameterTable, List<String> header) {
    String lineCommand;
    List<String> line;
    List<String> resultHeader;
    List<List<String>> result = new ArrayList<List<String>>();

    // Copy for A) result and B) to add additional columns if required
    resultHeader = new ArrayList<String>(header);

    // Get InputDefaults if defined
    String defaultName = commandExecuter.Properties().getProperty(ConfigurationParameters.inputDefaults);
    PropertiesLoader defaults = (defaultName == null) ? null : commandExecuter.Properties().getSubProperties(defaultName);

    // Start at line 1, after the header
    for (int l = 1; l < parameterTable.size(); l++) {

      commandExecuter.reset();

      line = parameterTable.get(l);
      lineCommand = _rawCommand;
      for (int p = 0; p < header.size(); p++) {
        // If there are less parameter values than variables then add empty values
        // to avoid errors
        if (p >= line.size()) line.add("");
        String rawColumnName = header.get(p);
        if (HeaderLine.isInputColumn(rawColumnName)) {
          String value = line.get(p);
          // Value could be null convert into a string  to avoid null pointer exception in replace for command
          String sqlValue = (value == null) ? "null" : value;
          if (l == parameterTable.size() - 1) {
            if (Pattern
              .compile(
                "(?i)" + _pre_fix
                  + HeaderLine.plainColumnName(rawColumnName) + _post_fix)
              .matcher(lineCommand).find(0)) {
              // Dirty hack to add these to the list of used headers
              commandExecuter.get(HeaderLine.plainColumnName(rawColumnName));
            }
          }
          try {
            lineCommand = lineCommand.replaceAll("(?i)" + _pre_fix + HeaderLine.plainColumnName(rawColumnName) + _post_fix, Matcher.quoteReplacement(sqlValue));
          } catch (IllegalArgumentException e) {
            throw new RuntimeException("Replacement failed. The given value '" + sqlValue + "' can't be used", e);
          }
          // Allows null for value
          commandExecuter.set(HeaderLine.plainColumnName(rawColumnName), value);
        }
      }
      lineCommand = replaceAllDefaults(lineCommand, defaults, _pre_fix, _post_fix);

      // Execute the command
      commandExecuter.execute(lineCommand);

      if (commandExecuter.success()) {
        List<List<String>> LineResultSheet = commandExecuter.resultSheet();
        if (LineResultSheet.size() == 0) {
          // Command without results
          line.add("pass:" + lineCommand);
        }
        if (LineResultSheet.size() != 2) {
          line.add("fail:" + lineCommand);
          line.add("fail:Got " + LineResultSheet.size() + " result rows; expected exactly one: " + commandExecuter.rawResult());
        } else {
          line = compareLine(resultHeader, LineResultSheet.get(0), line, LineResultSheet.get(1));
        }
      } else {
        line.add("fail:" + lineCommand);
        line.add("fail:" + commandExecuter.rawResult());
      }

      result.add(line);

    }
    // Color the header Column and update the total result set
    result.add(
      0,
      HeaderLine.formatHeader(header, resultHeader,
        commandExecuter.getUsedColumnNames(), _rawCommand,
        commandExecuter.Properties()));

    return result;
  }


  private String replaceAllDefaults(String lineCommand,
                                    PropertiesLoader defaults, String _pre_fix2, String _post_fix2) {

    if (defaults == null) return lineCommand;
    List<List<String>> defaultList = defaults.toTable();
    //Skip the header start i at 1
    for (int i = 1; i < defaultList.size(); i++)
      //(?i) = ignore case - required as keys are all lower case
      lineCommand = lineCommand.replaceAll("(?i)" + _pre_fix + defaultList.get(i).get(0) + _post_fix, defaultList.get(i).get(1));
    return lineCommand;
  }


  /**
   * @param expectedHeader the resulting header line this is an in/out parameter. The header might get extended in the procedure
   *                       if actual has columns which expected doesn't have
   * @param actualHeader   The header of the result set of the executed command
   * @param expectedLine   The data of an expected line
   * @param actualLine     One line of the result set of the executed command
   * @return the resulting line generated by comparing expected and actual with formatting. Column with the same name are compared.
   * The names are taken from the header.
   */
  public List<String> compareLine(List<String> expectedHeader, List<String> actualHeader, List<String> expectedLine, List<String> actualLine) {
    // Add the result to the correct columns
    List<String> lineResult = new ArrayList<String>();

    int[] H = HeaderLine.mapActualHeaderToExpectedHeader(expectedHeader, actualHeader);
    if (commandExecuter.Properties().isDebug()) {
      System.out.println(expectedHeader);
      System.out.println(actualHeader);
      System.out.println(Arrays.toString(H));
    }

    int maxActual = expectedHeader.size();
    if (commandExecuter.Properties().isDebug()) System.out.println("maxActual:" + maxActual);

    for (int i = 0; i < H.length; i++) {
      if (i < maxActual) {
        int lineResultIndex = H[i];
        String expected = expectedLine.size() > i ? expectedLine.get(i) : "";
        if (expected == null) expected = _nullStrOut;
        if (lineResultIndex != -1) { // There is an actual output column with the same name
          String actual = actualLine.size() > lineResultIndex ? actualLine.get(lineResultIndex) : "";
          if (actual == null) actual = _nullStrOut;
          if (expectedLine.size() <= i) {
            // Additional Value
            retry.runFailed();
            lineResult.add("fail:[+]" + actual);
          } else if (expected.isEmpty()) {
            lineResult.add("ignore:" + actual);
          } else {
            CellComparator cc = new CellComparator(actual, expected);
            if (commandExecuter.Properties().isDebug()) System.out.println("CellComparator: " + cc.toString());
            SlimTestResult str = cc.evaluate();
            if (str == null) System.out.println("cc.evalute() is null");
            if (commandExecuter.Properties().isDebug()) System.out.println("SlimTestResult: " + str.toString());
            ExecutionResult er = str.getExecutionResult();
            if (commandExecuter.Properties().isDebug()) System.out.println("ExecutionResult: " + er.toString());
            if (er == ExecutionResult.PASS) {
              lineResult.add("pass:" + str.getMessage());
            } else {
              retry.runFailed();
              if (actualLine.size() <= lineResultIndex) {
                // Missing Actual Column, mark cell as missing
                lineResult.add("fail:[-]" + expected);
              } else {
                lineResult.add("fail:" + str.getMessage());
              }
            }
          }
        } else { // No actual output column with the same name
          if (expected.compareTo("") == 0 || !HeaderLine.isOutputColumn(expectedHeader.get(i))) {
            lineResult.add("report:" + expected);
          } else {
            retry.runFailed();
            lineResult.add("fail:" + expected);
          }

        }
      } else {
        // Add Additional Output columns which have no matching expected
        int lineResultIndex = H[i];
        if (lineResultIndex == -1) {
          // Extra Cell

          // Safety check on index, expected to never see the else part text
          String actual = actualLine.size() > i - maxActual ? actualLine.get(i - maxActual) : "Actual header without actual Data?";
          lineResult.add("fail:Extra:" + actual);
          retry.runFailed();
          expectedHeader.add(actualHeader.get(i - maxActual));

        } else {
          // Value has already been added
          // nothing needs to be done

        }

      }
    }
    if (commandExecuter.Properties().isDebug()) {
      System.out.println("InputLine:  " + expectedLine);
      System.out.println("ActualLine: " + actualLine);
      System.out.println("OutputLine: " + lineResult);
    }

    return lineResult;
  }

  public List<List<String>> replaceNullInExpected(List<List<String>> expected) {
    List<List<String>> result = new ArrayList<List<String>>();
    for (int l = 0; l < expected.size(); l++) {
      List<String> line = new ArrayList<String>(expected.get(l));
      if (l > 0) {
        for (int c = 0; c < line.size(); c++) {
          String cell = line.get(c);
          if (_nullStrIn.equalsIgnoreCase(cell)) line.set(c, null);
        }
      }
      result.add(line);

    }
    return result;
  }

  public List<List<String>> compareTableWithSort(List<List<String>> expected, List<List<String>> actual, boolean sort) {
    List<List<String>> result = new ArrayList<List<String>>();
    final List<String> emptyRow = new ArrayList<String>();

    Boolean isSubQuery = commandExecuter.Properties().getPropertyOrDefault("subquery", "false").compareToIgnoreCase("false") != 0;

    int c;
    List<String> expectedHeader;
    List<String> actualHeader;

    if (actual == null || actual.size() < 1) {
      actualHeader = new ArrayList<String>();
      actualHeader.add("fail: The result has not returned a header line.");
      result.add(actualHeader);
      return result;

    } else {
      actualHeader = actual.get(0);
    }

    if (expected == null || expected.size() < 1) {
      // No expected Data set actual as expected
      expected = actual;
    }
    // Use a copy of the header as the expected header can change and we need the original later
    expectedHeader = new ArrayList<String>(expected.get(0));

    // Prepare Everything for comparing based on the given index columns
    int[] headerMap = HeaderLine.mapActualHeaderToExpectedHeader(expectedHeader, actualHeader);
    List<HeaderCell> expectedSortKeys = HeaderLine.generateSortKeyList(expectedHeader);
    List<HeaderCell> actualSortKeys = HeaderLine.generateActualSortKeyList(expectedSortKeys, headerMap, expectedHeader.size());
    if (commandExecuter.Properties().isDebug()) {
      System.out.println("expectedSortKeys: " + expectedSortKeys);
      System.out.println("actualSortKeys: " + actualSortKeys);
    }

    StringListComparator resultComparator = new StringListComparator(expectedSortKeys, actualSortKeys);

    if (sort) {
      // Sort the actual values except the header line
      //Create a copy to not change the data of the calling function
      List<List<String>> tmp = new ArrayList<List<String>>(actual);
      tmp.remove(0);
      StringListComparator sortComparator = new StringListComparator(actualSortKeys);
      Collections.sort(tmp, sortComparator);
      tmp.add(0, actualHeader);
      actual = tmp;
    }


    // Compare line by  line except the header, therefore index start is 1 and not 0
    int a = 1;
    int e = 1;
    while (a < actual.size()) {
      if (e < expected.size()) {
        c = resultComparator.compare(expected.get(e), actual.get(a));
        if (c == 0) {
          result.add(compareLine(expectedHeader, actualHeader, expected.get(e), actual.get(a)));
          a++;
          e++;
        } else if (c < 0) {
          //Missing expected line
          result.add(compareLine(expectedHeader, actualHeader, expected.get(e), emptyRow));
          e++;

        } else { // c > 0
          //Additional actual line
          if (!isSubQuery)
            result.add(compareLine(expectedHeader, actualHeader, emptyRow, actual.get(a)));
          a++;

        }
      } else {
        //Additional actual lines at the end
        if (!isSubQuery)
          result.add(compareLine(expectedHeader, actualHeader, emptyRow, actual.get(a)));
        a++;

      }
    }
    while (e < expected.size()) {
      //Missing expected lines at the end
      result.add(compareLine(expectedHeader, actualHeader, expected.get(e), emptyRow));
      e++;
    }
    //Color the header and add to result set
    result.add(0, HeaderLine.formatHeader(expected.get(0), expectedHeader));
    return result;
  }
}
