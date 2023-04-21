package six42.fitnesse.jdbcslim;

import static fitnesse.testsystems.slim.tables.ComparatorUtil.approximatelyEqual;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.slim.CustomComparator;
import fitnesse.testsystems.slim.CustomComparatorRegistry;
import fitnesse.testsystems.slim.results.SlimTestResult;

class CellComparator {
  private final String expression;
  private final String actual;
  private final String expected;
  private final Pattern simpleComparison = Pattern.compile(
    "\\A\\s*_?\\s*(!?(?:(?:[<>]=?)|(?:[~]?=)))\\s*(-?\\d*\\.?\\d+)\\s*\\Z"
  );
  private final Pattern range = Pattern.compile(
    "\\A\\s*(-?\\d*\\.?\\d+)\\s*<(=?)\\s*_\\s*<(=?)\\s*(-?\\d*\\.?\\d+)\\s*\\Z"
  );

  private Pattern regexPattern = Pattern.compile("\\s*=~/(.*)/");
  private Pattern customComparatorPattern = Pattern.compile("\\s*(\\w*):(.*)");
  private double v;
  private double arg1;
  private double arg2;
  public String operation;
  private String arg1Text;

  public String toString() {
    return "CellComparator(Expression(" + expression + "),actual(" + actual + "),expected(" + expected + "),operation(" + operation + "))";
  }


  public CellComparator(String actual, String expected) {
    // TODO enable Symbols in Comparator
    // this.expression = replaceSymbols(expected);
    this.expression = expected;
    this.actual = actual;
    this.expected = expected;
  }

  public CellComparator(String expression, String actual, String expected) {
    this.expression = expression;
    this.actual = actual;
    this.expected = expected;
  }

  public boolean matches() {
    TestResult testResult = evaluate();
    return testResult != null && testResult.getExecutionResult() == ExecutionResult.PASS;
  }

  public SlimTestResult evaluate() {
    SlimTestResult message = evaluateRegularExpressionIfPresent();
    if (message != null)
      return message;

    message = evaluateCustomComparatorIfPresent();
    if (message != null)
      return message;

    operation = matchSimpleComparison();
    if (operation != null)
      return doSimpleComparison();

    Matcher matcher = range.matcher(expression);
    if (matcher.matches() && canUnpackRange(matcher)) {
      return doRange(matcher);
    } else if (actual.equals(expected)) { // Do simple string comparison
      return SlimTestResult.pass(actual);
    } else {
      return SlimTestResult.fail("[" + actual + "] expected [" + expected + "]");
    }
  }

  private SlimTestResult evaluateCustomComparatorIfPresent() {
    SlimTestResult message = null;

    // TODO this can't be run on he Slim Server Side only in TestSystem
/*      Matcher customComparatorMatcher = customComparatorPattern.matcher(expression);
      if (customComparatorMatcher.matches()) {
        String prefix = customComparatorMatcher.group(1);
        CustomComparator customComparator = CustomComparatorRegistry.getCustomComparatorForPrefix(prefix);
        if (customComparator != null) {
          String expectedString = customComparatorMatcher.group(2);
          try {
            if (customComparator.matches(actual, expectedString)) {
              message = SlimTestResult.pass(expectedString + " matches " + actual);
            } else {
              message = SlimTestResult.fail(expectedString + " doesn't match " + actual);
            }
          } catch (Throwable t) {
            message = SlimTestResult.fail(expectedString + " doesn't match " + actual + ":\n" + t.getMessage());
          }
        }
      }
*/
    return message;
  }

  private SlimTestResult evaluateRegularExpressionIfPresent() {
    Matcher regexMatcher = regexPattern.matcher(expression);
    SlimTestResult message = null;
    if (regexMatcher.matches()) {
      String pattern = regexMatcher.group(1);
      message = evaluateRegularExpression(pattern);
    }
    return message;
  }

  private SlimTestResult evaluateRegularExpression(String pattern) {
    SlimTestResult message;
    Matcher patternMatcher = Pattern.compile(pattern).matcher(actual);
    if (patternMatcher.find()) {
      message = SlimTestResult.pass(String.format("/%s/ found in: %s", pattern, actual));
    } else {
      message = SlimTestResult.fail(String.format("/%s/ not found in: %s", pattern, actual));
    }
    return message;
  }

  private SlimTestResult doRange(Matcher matcher) {
    boolean closedLeft = matcher.group(2).equals("=");
    boolean closedRight = matcher.group(3).equals("=");
    boolean pass = (arg1 < v && v < arg2) || (closedLeft && arg1 == v) || (closedRight && arg2 == v);
    return rangeMessage(pass);
  }

  private SlimTestResult rangeMessage(boolean pass) {
    String[] fragments = expected.replaceAll(" ", "").split("_");
    String message = String.format("%s%s%s", fragments[0], actual, fragments[1]);
    // TODO enable Symbols in Comparator
    // message = replaceSymbolsWithFullExpansion(message);
    return pass ? SlimTestResult.pass(message) : SlimTestResult.fail(message);
  }

  private boolean canUnpackRange(Matcher matcher) {
    try {
      arg1 = Double.parseDouble(matcher.group(1));
      arg2 = Double.parseDouble(matcher.group(4));
      v = Double.parseDouble(actual);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  private SlimTestResult doSimpleComparison() {
    if (operation.equals("<") || operation.equals("!>="))
      return simpleComparisonMessage(v < arg1);
    else if (operation.equals(">") || operation.equals("!<="))
      return simpleComparisonMessage(v > arg1);
    else if (operation.equals(">=") || operation.equals("!<"))
      return simpleComparisonMessage(v >= arg1);
    else if (operation.equals("<=") || operation.equals("!>"))
      return simpleComparisonMessage(v <= arg1);
    else if (operation.equals("!="))
      return simpleComparisonMessage(v != arg1);
    else if (operation.equals("="))
      return simpleComparisonMessage(v == arg1);
    else if (operation.equals("~="))
      return simpleComparisonMessage(approximatelyEqual(arg1Text, actual));
    else if (operation.equals("!~="))
      return simpleComparisonMessage(!approximatelyEqual(arg1Text, actual));
    else
      return null;
  }

  private SlimTestResult simpleComparisonMessage(boolean pass) {
    String message = String.format("%s%s", actual, expected.replaceAll(" ", ""));
    // TODO enable Symbols in Comparator
    // message = replaceSymbolsWithFullExpansion(message);
    return pass ? SlimTestResult.pass(message) : SlimTestResult.fail(message);

  }

  private String matchSimpleComparison() {
    Matcher matcher = simpleComparison.matcher(expression);
    if (matcher.matches()) {
      try {
        v = Double.parseDouble(actual);
        arg1Text = matcher.group(2);
        arg1 = Double.parseDouble(arg1Text);
        return matcher.group(1);
      } catch (NumberFormatException e1) {
        return null;
      }
    }
    return null;
  }

}
