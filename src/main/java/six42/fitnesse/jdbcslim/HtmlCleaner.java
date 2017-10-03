package six42.fitnesse.jdbcslim;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper to remove wiki formatting from strings.
 */
public class HtmlCleaner {
    private static final Pattern PRE_FORMATTED_PATTERN = Pattern.compile("<pre>\\s*(.*?)\\s*</pre>", Pattern.DOTALL);

    /**
     * Removes HTML preformatting (if any).
     * @param value value (possibly pre-formatted)
     * @return value without HTML preformatting.
     */
    public static String cleanupPreFormatted(String value) {
        String result = value;
        if (value != null) {
            Matcher matcher = PRE_FORMATTED_PATTERN.matcher(value);
            if (matcher.matches()) {
                String escapedBody = matcher.group(1);
                result = StringEscapeUtils.unescapeHtml(escapedBody);
            }
        }
        return result;
    }
}
