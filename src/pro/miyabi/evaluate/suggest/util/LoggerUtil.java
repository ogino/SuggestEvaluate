/**
 * 
 */
package pro.miyabi.evaluate.suggest.util;

import org.apache.logging.log4j.Logger;

/**
 * @author OGINO, Tadashi <tadashi.ogino@syncthought.com>
 *
 */
public class LoggerUtil {

	public static void logException(final Logger logger, final Exception exception) {
		logger.error("Throwable Class : " + exception.toString());
//		logger.error("Message: " + exception.getLocalizedMessage());
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : exception.getStackTrace())
			sb.append("    at " + element.toString() + "\n");
		logger.error("Message: " + exception.getLocalizedMessage() + "\n" + sb.toString());
	}
}
