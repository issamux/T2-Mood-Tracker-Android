package com.t2.vas;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.database.Cursor;

import com.t2.vas.db.DBAdapter;
import com.t2.vas.view.chart.Label;
import com.t2.vas.view.chart.SeriesAdapterData;
import com.t2.vas.view.chart.Value;
import com.t2.vas.view.chart.Series.SeriesDataAdapter;

public abstract class ResultsSeriesDataAdapter implements SeriesDataAdapter {
	private static final String TAG = ResultsSeriesDataAdapter.class.getName();
	
	protected DBAdapter dbAdapter;
	//protected long scaleId;
	protected int groupBy;
	protected String labelFormat;
	
	public static final int GROUPBY_YEAR = Calendar.YEAR;
	public static final int GROUPBY_MONTH = Calendar.MONTH;
	public static final int GROUPBY_WEEK = Calendar.WEEK_OF_YEAR;
	public static final int GROUPBY_DAY = Calendar.DAY_OF_MONTH;
	public static final int GROUPBY_HOUR = Calendar.HOUR_OF_DAY;
	
	public static final int ORDERBY_ASC = 13;
	public static final int ORDERBY_DESC = 14;
	
	
	public ResultsSeriesDataAdapter(DBAdapter dbAdapter, int groupBy, String labelFormat) {
		this.dbAdapter = dbAdapter;
		//this.scaleId = scaleId;
		this.groupBy = groupBy;
		this.labelFormat = labelFormat;
	}
	
	protected abstract Cursor getCursor(String db_date_format);
	
	protected boolean shouldHilight(long startTime, long endTime) {
		return false;
	}
	
	@Override
	public SeriesAdapterData getData() {
		boolean openForThis = false;
		if(!this.dbAdapter.isOpen()) {
			this.dbAdapter.open();
			openForThis = true;
		}
		
		SimpleDateFormat labelDateFormatter = new SimpleDateFormat(labelFormat);
		String formatter_date_format = "";
		String db_date_format = "";
		
		// Determine the label format to use.
		switch(this.groupBy) {
			case GROUPBY_HOUR:
				formatter_date_format = "yyyy-MM-dd HH";
				db_date_format = "%Y-%m-%d %H";
				break;
			case GROUPBY_MONTH:
				formatter_date_format = "yyyy-MM";
				db_date_format = "%Y-%m";
				break;
			case GROUPBY_WEEK:
				formatter_date_format = "yyyy-ww";
				db_date_format = "%Y-%W";
				break;
			case GROUPBY_YEAR:
				formatter_date_format = "yyyy";
				db_date_format = "%Y";
				break;
			case GROUPBY_DAY:
			default:
				formatter_date_format = "yyyy-MM-dd";
				db_date_format = "%Y-%m-%d";
				break;
		}
		
		Cursor c = this.getCursor(db_date_format);
		
		String rLabelValue = "";
		long rTimestamp = 0;
		double rValue = 0.00;
		boolean rShouldHilight = false;
		
		SeriesAdapterData resultValues = new SeriesAdapterData();
		//ResultValues resultValues = new ResultValues();
		SimpleDateFormat groupByDateFormatter = new SimpleDateFormat(formatter_date_format);
		Date tmpDate;
		Calendar runningCal = null;
		Calendar rowCal = Calendar.getInstance();
		boolean loadNext = true;
		while(true) {
			if(loadNext) {
				if(!c.moveToNext()) {
					break;
				}
				
				rLabelValue = c.getString(c.getColumnIndex("label_value"));
				rTimestamp = c.getLong(c.getColumnIndex("timestamp"));
				rValue = c.getDouble(c.getColumnIndex("value"));
				
				Calendar tmpCal = Calendar.getInstance();
				tmpCal.setTimeInMillis(rTimestamp);
				
				if(runningCal == null) {
					runningCal = Calendar.getInstance();
					runningCal.setTimeInMillis(rTimestamp);
				}
			}
			loadNext = false;
			
			
			try {
				rowCal.setTimeInMillis(rTimestamp);
				tmpDate = rowCal.getTime();
				rowCal.setTime(
						groupByDateFormatter.parse(groupByDateFormatter.format(tmpDate))
				);
				
				tmpDate = runningCal.getTime();
				runningCal.setTime(
						groupByDateFormatter.parse(groupByDateFormatter.format(tmpDate))
				);
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			Calendar nextCal = Calendar.getInstance();
			nextCal.setTimeInMillis(runningCal.getTimeInMillis());
			nextCal.add(this.groupBy, 1);
			//rShouldHilight = this.noteBetween(runningCal.getTimeInMillis(), nextCal.getTimeInMillis());
			rShouldHilight = this.shouldHilight(runningCal.getTimeInMillis(), nextCal.getTimeInMillis());
			
			String labelString = labelDateFormatter.format(runningCal.getTime());
			if(rowCal.getTimeInMillis() == runningCal.getTimeInMillis()) {
				resultValues.add(
						new Label<Date>(labelString, runningCal.getTime()),
						new Value(rValue, null, rShouldHilight)
				);
				
				loadNext = true;
			} else {
				resultValues.add(
						new Label<Date>(labelString, runningCal.getTime()), 
						new Value(null, null, rShouldHilight)
				);
			}
			
			runningCal.add(this.groupBy, 1);
		}
		c.close();
		
		
		if(openForThis) {
			this.dbAdapter.close();
		}
		
		return resultValues;
	}

}
