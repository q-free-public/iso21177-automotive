package com.qfree.its.iso21177poc.common.geoflow.thick_client;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BillingPeriod {
    public LocalDateTime periodStart = null;
    public LocalDateTime periodEnd = null;
    static private int                 intvCount;
    static private InvoiceIntervalType intvType;

    public BillingPeriod(LocalDateTime now) {
        switch (intvType) {
            case Month:
                periodStart = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atTime(LocalTime.MIN);
                periodEnd = periodStart.plusMonths(1);
                break;
            case Week:
                periodStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atTime(LocalTime.MIN);
                periodEnd = periodStart.plusDays(7 * intvCount);
                break;
            case Day:
                periodStart = now.toLocalDate().atTime(LocalTime.MIN);
                periodEnd = periodStart.plusHours(24 * intvCount);
                break;
            case Hour:
                periodStart = now.minusMinutes(now.getMinute()).minusSeconds(now.getSecond()).minusNanos(now.getNano()).minusHours(now.getHour()%intvCount);
                periodEnd = periodStart.plusHours(intvCount);
                break;
            case Minute:
                periodStart = now.minusSeconds(now.getSecond()).minusNanos(now.getNano()).minusHours(now.getHour()%intvCount);
                periodEnd = periodStart.plusMinutes(intvCount);
        }

        System.out.println("InvoiceInterval: " + intvCount + "    t=" + intvType + "    " + periodStart + "     " + periodEnd);
    }

    public String toString() {
        return intvType + "=" + intvCount + "  " + periodStart + " - " + periodEnd;
    }

    public String getInterval(){
        return String.format(Locale.ROOT, "%d %s", intvCount, intvType);
    }

    public static void parse(String strInvoiceInterval) throws Exception {
        Pattern pattern = Pattern.compile("([0-9]+)([khdwm])");
        Matcher m = pattern.matcher(strInvoiceInterval);
        if (m.matches()) {
            intvCount = Integer.parseInt(m.group(1));
            switch (m.group(2)) {
                case "k": intvType = InvoiceIntervalType.Minute; break;
                case "h": intvType = InvoiceIntervalType.Hour; break;
                case "d": intvType = InvoiceIntervalType.Day; break;
                case "w": intvType = InvoiceIntervalType.Week; break;
                case "m": intvType = InvoiceIntervalType.Month; break;
                default:
                    throw new Exception("InvoiceInterval \"" + strInvoiceInterval + "\" is invalid.");
            }
            System.out.println("InvoiceInterval: " + intvCount + "    t=" + intvType);
        } else {
            throw new Exception("InvoiceInterval \"" + strInvoiceInterval + "\" is invalid.");
        }
    }
}
