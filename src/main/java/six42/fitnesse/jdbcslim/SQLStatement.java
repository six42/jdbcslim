package six42.fitnesse.jdbcslim;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SQLStatement {

  private String initialCommandString;
  private String finalCommandString;
  private List<List<String>> inputParamterList;
  private Map<String, String> defaultList;

  public SQLStatement(String sqlCommand) {
    this.initialCommandString = sqlCommand;
    inputParamterList = new ArrayList<List<String>>();
    defaultList = new HashMap<String, String>();

    this.finalCommandString = this.initialCommandString;
  }

  public void extractParametersFromCmd() {
    // ?{Input Column>Output Column:Type(Scale)=Default Value}
    // "\?\{([\p{L}_ \-\d]+)?(?:>([\p{L}_
    // \-\d]+))?(?::([\w-]+)(?:\((\d+)\))?)?(?s:=([^{]+))?\}";
    String parameterPattern = "\\?\\{([\\p{L}_ \\-\\d]+)?(?:>([\\p{L}_ \\-\\d]+))?(?::([\\w-]+)(?:\\((\\d+)\\))?)?(?s:=([^{]+))?\\}";
    Pattern p = Pattern.compile(parameterPattern);
    Matcher m = p.matcher(initialCommandString);
    int o = 1;
    int i = 1;
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String inputColumnName = m.group(1);
      String outputColumnName = m.group(2);
      String type = m.group(3);
      String scale = m.group(4);
      String defaultValue = m.group(5);

      inputColumnName = (inputColumnName != null ? inputColumnName.trim()
          .toLowerCase() : "");
      outputColumnName = (outputColumnName != null ? outputColumnName.trim()
          .toLowerCase() : "");
      scale = (scale != null ? ":" + scale : "");
      if (type == null) {
        type = "4";
        scale = "";
      }
      if (inputColumnName.isEmpty() && outputColumnName.isEmpty()) {
        throw new RuntimeException(
            "A SQL parameter must be either mapped to an input or output column. Your pattern has none: "
                + m.group(0));
      }
      if (!inputColumnName.isEmpty()) {
        List<String> line = new ArrayList<String>();
        line.add(inputColumnName);
        line.add("I:" + i + ":" + type + scale);
        inputParamterList.add(line);
        i++;

        if (defaultValue != null) {
          defaultList.put(inputColumnName, defaultValue);
        }
      }
      if (!outputColumnName.isEmpty()) {
        List<String> line = new ArrayList<String>();
        line.add(outputColumnName);
        line.add("O:" + o + ":" + type + scale);
        inputParamterList.add(line);
        o++;
      }
      m.appendReplacement(sb, "?");

    }
    m.appendTail(sb);
    this.finalCommandString = sb.toString();

  }

  public void addParametersFromProperties(PropertiesInterface properties,
      ConfigurationParameters configurationName) {
    String parameterName = properties.getPropertyOrDefault(configurationName,
        "");
    if (!parameterName.isEmpty()) {
      PropertiesLoader queryParameters = properties
          .getSubProperties(parameterName);
      List<List<String>> table = queryParameters.toTable();
      table.remove(/* header line */0);
      inputParamterList.addAll(table);
    }
  }

  public void addDefaultsFromProperties(PropertiesInterface properties,
      ConfigurationParameters configurationName) {
    String parameterName = properties.getPropertyOrDefault(configurationName,
        "");
    if (!parameterName.isEmpty()) {
      PropertiesLoader queryParameters = properties
          .getSubProperties(parameterName);
      List<List<String>> table = queryParameters.toTable();
      table.remove(/* header line */0);
      for (List<String> keyValue : table) {
        defaultList.put(keyValue.get(0), keyValue.get(1));
      }
    }
  }

  public String sqlCommand() {
    return this.finalCommandString;
  }

  public SortedMap<Integer, String> setInputParameters(CallableStatement cstmt,
      SheetCommandInterface sqlCommand) {
    SortedMap<Integer, String> outputParamterMap = new TreeMap<Integer, String>();
    for (int i = 0; i < inputParamterList.size(); i++) {
      try {
        String columnName = inputParamterList.get(i).get(0);
        String[] paramValues = inputParamterList.get(i).get(1).split(":");
        boolean inParameter = paramValues.length < 1 ? false : paramValues[0]
            .toUpperCase().contains("I");
        boolean outParameter = paramValues.length < 1 ? false : paramValues[0]
            .toUpperCase().contains("O");
        int parameterIndex = paramValues.length < 2 ? 0 : Integer
            .parseInt(paramValues[1]);
        int sqlType = paramValues.length < 3 ? 0 : Integer
            .parseInt(paramValues[2]);
        int scale = paramValues.length < 4 ? -1 : Integer
            .parseInt(paramValues[3]);

        if (inParameter) {
          if (sqlCommand.containsKey(columnName)) {
            Object obj = sqlCommand.get(columnName);
            String value = (obj != null) ? obj.toString() : null;

            if (scale == -1)
              cstmt.setObject(parameterIndex, value, sqlType);
            else
              cstmt.setObject(parameterIndex, value, sqlType, scale);
          } else if (defaultList.containsKey(columnName)) {
            String value = defaultList.get(columnName);
            if (scale == -1)
              cstmt.setObject(parameterIndex, value, sqlType);
            else
              cstmt.setObject(parameterIndex, value, sqlType, scale);
          } else {
            if (sqlCommand.Properties().isDebug()) {
              System.out
                .println("Unused Parameter (" + i + "):'" + columnName + "'");
            }
          }
        }
        if (outParameter) {
          if (scale == -1)
            cstmt.registerOutParameter(parameterIndex, sqlType);
          else
            cstmt.registerOutParameter(parameterIndex, sqlType, scale);
          outputParamterMap.put(parameterIndex, columnName);
        }
      } catch (NumberFormatException e) {
        throw new RuntimeException("Failed processing Query Parameter:"
            + inputParamterList.get(i).get(0) + "="
            + inputParamterList.get(i).get(1), e);
      } catch (SQLException e) {
        throw new RuntimeException("Failed setting Query Parameter:"
            + inputParamterList.get(i).get(0) + "="
            + inputParamterList.get(i).get(1), e);
      }

    }
    return outputParamterMap;
  }

}
