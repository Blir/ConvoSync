package com.minepop.servegame.convosync;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Blir
 */
public class CustomFormatter extends Formatter {

    private final Calendar CALENDAR;
    private final String LINE_SEPARATOR;

    public CustomFormatter() {
        CALENDAR = Calendar.getInstance();
        LINE_SEPARATOR = System.getProperty("line.separator");
    }

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        Date date = new Date(record.getMillis());
        CALENDAR.setTime(date);
        sb.append(CALENDAR.get(Calendar.HOUR_OF_DAY))
                .append(":")
                .append(CALENDAR.get(Calendar.MINUTE))
                .append(":")
                .append(CALENDAR.get(Calendar.SECOND))
                .append(" [")
                .append(record.getLevel())
                .append("] ");
        if (record.getMessage() != null) {
            String msg = record.getMessage();
            if (record.getParameters() != null) {
                for (int idx = 0; idx < record.getParameters().length; idx++) {
                    msg = msg.replace("{" + idx + "}", String.valueOf(record.getParameters()[idx]));
                }
            }
            sb.append(msg);
        }
        if (record.getThrown() != null) {
            StringWriter sw;
            PrintWriter pw = null;
            try {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                sb.append(LINE_SEPARATOR)
                        .append(sw.toString());
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
        sb.append(LINE_SEPARATOR);
        return sb.toString();
    }
}
