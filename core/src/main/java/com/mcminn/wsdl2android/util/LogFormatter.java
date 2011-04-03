package com.mcminn.wsdl2android.util;

import java.util.logging.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFormatter extends Formatter
{
    private static final SimpleDateFormat sdf = 
        new SimpleDateFormat("MM/dd HH:mm:ss.sss");

    private String getSimpleName(String className)
    {
        return className.substring(className.lastIndexOf(".") + 1);
    }

    private String getSimpleLogString(Level level)
    {
        String rtrn = level.getName().charAt(0) + "";
        if(rtrn.equals("F"))
        {
            String name = level.getName();
            char last = name.charAt(name.length() - 1);
            if(last == 'E')
                rtrn += "1";
            else if(last == 'T')
                rtrn += "3";
            else if(last == 'R')
                rtrn += "2";
        }

        return rtrn;
    }

    private String getParameters(Object[] parameters)
    {
        String rtrn = "";

        if(parameters != null)
            for(Object o : parameters)
                rtrn += ", " + o.toString();

        return rtrn;
    }

    public String format(LogRecord record)
    {
        String rtrn = "[" + sdf.format(new Date(record.getMillis())) + "] "
            + getSimpleLogString(record.getLevel()) + " ["
            + getSimpleName(record.getSourceClassName()) + "."
            + record.getSourceMethodName() 
            + getParameters(record.getParameters()) + "] ";

        if(record.getMessage().equals("THROW"))
        {
            Throwable throwable = record.getThrown();
            StackTraceElement[] trace = throwable.getStackTrace();
            int numSTE = trace.length;

            while(throwable != null)
            {
                rtrn += throwable.getClass().getName() + ": " 
                    + throwable.getLocalizedMessage() + "\n";

                for(int x = 0;x < numSTE;x++)
                    rtrn += "\tat " + trace[x].toString() + "\n";

                throwable = throwable.getCause();
                if(throwable != null)
                {
                    rtrn += "Caused by: ";
                    trace = throwable.getStackTrace();
                    numSTE = Math.min(5, trace.length);
                }
            }
        }
        else
            rtrn += record.getMessage() + "\n";

        return rtrn;
    }
}
