package org.crf.utilities.log4j;

import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A layout for printing messages by Log4j. It prints the log message with prefix like "INFO - ", "DEBUG - ", etc.
 * 
 * @author Asher Stern
 * Date: Nov 4, 2014
 *
 */
public class VerySimpleLayout extends SimpleLayout
{
	public String format(LoggingEvent event)
	{
		stringBuffer.setLength(0);
		stringBuffer.append(event.getLevel().toString()).append(" - ");
		stringBuffer.append(event.getRenderedMessage());
		stringBuffer.append(LINE_SEP);
		return stringBuffer.toString();
	}


	private static final int STRING_BUFFER_INITIAL_LENGTH = 1024;
	private StringBuffer stringBuffer = new StringBuffer(STRING_BUFFER_INITIAL_LENGTH); 

}
