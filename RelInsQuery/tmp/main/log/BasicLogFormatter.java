package main.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class BasicLogFormatter extends Formatter{

	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_WHITE = "\u001B[37m";
	public static final String ANSI_RED = "\u001B[31m";
	
	private boolean m_useColors;
	
	public BasicLogFormatter(boolean useColors) {
		m_useColors = useColors;
	}
	
	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		
		String color = ANSI_WHITE;
		if(record.getLevel().intValue() >= Level.WARNING.intValue()){
			color = ANSI_RED;
		}
		if(m_useColors)
			sb.append(color);
		
		sb.append("[");
		sb.append(timeFormat.format(new Date(record.getMillis())));
		sb.append("] - ");
		
		sb.append(record.getLevel() + " - ");
		
		String className = record.getSourceClassName();
		className = className.substring(className.lastIndexOf(".")+1, className.length());
		
		sb.append(className + " - [" + record.getSourceMethodName() + "]: ");
		
		sb.append(record.getMessage());
		
		if(m_useColors)
			sb.append(ANSI_RESET);
		
		sb.append("\n");
		
		return sb.toString();
	}

}
