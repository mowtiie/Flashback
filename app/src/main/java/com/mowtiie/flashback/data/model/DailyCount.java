package com.mowtiie.flashback.data.model;

/** One bucket of the statistics screen: reviews performed on a given day. */
public class DailyCount {

    /** Day key formatted as yyyy-MM-dd in local time. */
    public String day;

    public int count;
}
