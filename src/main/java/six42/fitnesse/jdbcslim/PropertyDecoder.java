package six42.fitnesse.jdbcslim;

import java.util.Map;

/**
 * Interface to decode encoded properties (using 'ENC()')
 */
public interface PropertyDecoder {
    Map<String,String> process(Map<String, String> props);
}
