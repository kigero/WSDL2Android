package com.mcminn.wsdl2android.util;

import java.util.logging.*;

public class LogUtil
{
    public static Logger buildLogger(String name)
    {
        Logger rtrn = Logger.getLogger(name);
        rtrn.setLevel(Level.FINEST);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.FINEST);
        handler.setFormatter(new LogFormatter());
        rtrn.addHandler(handler);

        return rtrn;
    }

    public static void dumpConfig()
    {
        for(java.util.Enumeration<String> names = LogManager.getLogManager().getLoggerNames(); names.hasMoreElements();)
        {
            String name = names.nextElement();
            System.out.println("Logger: " + name);
            Logger l = LogManager.getLogManager().getLogger(name);

            System.out.println("* " + l.getHandlers().length + " handlers");
            for(java.util.logging.Handler h : l.getHandlers())
            {
                System.out.println("  * Formatter: " + h.getFormatter().getClass());
            }
        }
    }
}
