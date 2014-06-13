package com.github.blir.convosync.application;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.LogRecord;

/**
 *
 * @author Blir
 */
public class ClientFormatter extends java.util.logging.Formatter {

    private final DateFormat format;
    
    private final String LINE_SEPARATOR;
    
    public ClientFormatter(String format) {
        this.format = new SimpleDateFormat(format);
        this.LINE_SEPARATOR = System.getProperty("line.separator");
    }
    
    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb
                .append("[Client] ")
                .append(format.format(record.getMillis()))
                .append(" [")
                .append(record.getLevel())
                .append("] ");
        String msg = record.getMessage();
        if (msg != null) {
            Object[] parameters = record.getParameters();
            if (parameters != null) {
                for (int idx = 0; idx < parameters.length; idx++) {
                    msg = msg.replace("{" + idx + "}",
                                      String.valueOf(parameters[idx]));
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
                sb
                        .append(LINE_SEPARATOR)
                        .append(sw.toString());
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
        return sb.append(LINE_SEPARATOR).toString();
    }
}
