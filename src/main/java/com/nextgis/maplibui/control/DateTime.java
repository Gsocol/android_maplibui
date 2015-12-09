/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui.control;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplibui.R;
import com.nextgis.maplibui.api.ISimpleControl;
import com.nextgis.maplibui.util.ControlHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.nextgis.maplibui.util.ConstantsUI.*;


public class DateTime
        extends AppCompatTextView
        implements ISimpleControl
{
    String           mFieldName;
    SimpleDateFormat mDateFormat;
    Long mValue = null;


    public DateTime(Context context)
    {
        super(context);
    }


    public DateTime(
            Context context,
            AttributeSet attrs)
    {
        super(context, attrs);
    }


    public DateTime(
            Context context,
            AttributeSet attrs,
            int defStyle)
    {
        super(context, attrs, defStyle);
    }


    public void setCurrentDate()
    {
        mValue = Calendar.getInstance().getTimeInMillis();
        setText(getFormattedValue(mValue));
    }


    protected View.OnClickListener getDateUpdateWatcher(final int pickerType)
    {
        return new View.OnClickListener()
        {
            protected Calendar mCalendar = Calendar.getInstance();


            protected void setValue()
            {
                mValue = mCalendar.getTimeInMillis();
                DateTime.this.setText(getFormattedValue(mValue));
            }


            @Override
            public void onClick(View view)
            {
                Context context = DateTime.this.getContext();

                if (null != mValue) {
                    mCalendar.setTimeInMillis(mValue);
                } else {
                    mCalendar.setTime(new Date());
                }

                switch (pickerType) {

                    case DATE:
                        DatePickerDialog.OnDateSetListener onDateSetListener =
                                new DatePickerDialog.OnDateSetListener()
                                {
                                    @Override
                                    public void onDateSet(
                                            DatePicker view,
                                            int year,
                                            int monthOfYear,
                                            int dayOfMonth)
                                    {
                                        mCalendar.set(Calendar.YEAR, year);
                                        mCalendar.set(Calendar.MONTH, monthOfYear);
                                        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                        setValue();
                                    }

                                };

                        DatePickerDialog datePickerDialog = new DatePickerDialog(
                                context, onDateSetListener, mCalendar.get(Calendar.YEAR),
                                mCalendar.get(Calendar.MONTH),
                                mCalendar.get(Calendar.DAY_OF_MONTH));
                        datePickerDialog.show();
                        break;

                    case TIME:
                        TimePickerDialog.OnTimeSetListener onTimeSetListener =
                                new TimePickerDialog.OnTimeSetListener()
                                {
                                    @Override
                                    public void onTimeSet(
                                            TimePicker view,
                                            int hourOfDay,
                                            int minute)
                                    {
                                        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        mCalendar.set(Calendar.MINUTE, minute);

                                        setValue();
                                    }
                                };

                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                context, onTimeSetListener, mCalendar.get(Calendar.HOUR_OF_DAY),
                                mCalendar.get(Calendar.MINUTE),
                                android.text.format.DateFormat.is24HourFormat(context));
                        timePickerDialog.show();
                        break;

                    case DATETIME:
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);

                        builder.setTitle(mDateFormat.format(mCalendar.getTime()));
                        builder.setPositiveButton(
                                R.string.ok, new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialog,
                                            int which)
                                    {
                                        setValue();
                                        dialog.dismiss();
                                    }
                                });

                        final AlertDialog alert = builder.create();

                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                                Context.LAYOUT_INFLATER_SERVICE);
                        View datetimePickerLayout =
                                inflater.inflate(R.layout.dialog_datetimepicker, null);
                        alert.setView(datetimePickerLayout);

                        DatePicker dt =
                                (DatePicker) datetimePickerLayout.findViewById(R.id.datePicker);

                        dt.init(
                                mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                                mCalendar.get(Calendar.DAY_OF_MONTH),
                                new DatePicker.OnDateChangedListener()
                                {

                                    @Override
                                    public void onDateChanged(
                                            DatePicker view,
                                            int year,
                                            int monthOfYear,
                                            int dayOfMonth)
                                    {
                                        mCalendar.set(Calendar.YEAR, year);
                                        mCalendar.set(Calendar.MONTH, monthOfYear);
                                        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                        alert.setTitle(mDateFormat.format(mCalendar.getTime()));
                                    }
                                });

                        TimePicker tp =
                                (TimePicker) datetimePickerLayout.findViewById(R.id.timePicker);
                        tp.setIs24HourView(
                                android.text.format.DateFormat.is24HourFormat(context));

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            tp.setCurrentHour(mCalendar.get(Calendar.HOUR_OF_DAY));
                            tp.setCurrentMinute(mCalendar.get(Calendar.MINUTE));
                        } else {
                            tp.setHour(mCalendar.get(Calendar.HOUR_OF_DAY));
                            tp.setMinute(mCalendar.get(Calendar.MINUTE));
                        }

                        tp.setOnTimeChangedListener(
                                new TimePicker.OnTimeChangedListener()
                                {
                                    @Override
                                    public void onTimeChanged(
                                            TimePicker view,
                                            int hourOfDay,
                                            int minute)
                                    {
                                        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                        mCalendar.set(Calendar.MINUTE, minute);

                                        alert.setTitle(mDateFormat.format(mCalendar.getTime()));
                                    }
                                });

                        alert.show();
                        break;
                }
            }
        };
    }


    public String getFieldName()
    {
        return mFieldName;
    }


    @Override
    public void addToLayout(ViewGroup layout)
    {
        layout.addView(this);
        GreyLine.addToLayout(layout);
    }


    @Override
    public Object getValue()
    {
        return mValue;
    }


    @Override
    public void init(
            Field field,
            Bundle savedState,
            Cursor featureCursor)
    {
        if (null != field) {
            mFieldName = field.getName();
        }

        String text = "";
        mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        mValue = null;

        if (ControlHelper.hasKey(savedState, text)) {
            mValue = savedState.getLong(ControlHelper.getSavedStateKey(mFieldName));
        } else if (null != featureCursor) {
            int column = featureCursor.getColumnIndex(mFieldName);
            if (column >= 0) {
                mValue = featureCursor.getLong(column);
            }
        }

        if (null != mValue) {
            text = getFormattedValue(mValue);
        }

        setText(text);
        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(DATETIME));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }


    @Override
    public void saveState(Bundle outState)
    {
        outState.putLong(ControlHelper.getSavedStateKey(mFieldName), mValue);
    }


    protected String getFormattedValue(long value)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(value);
        return mDateFormat.format(calendar.getTime());
    }


    public void setValue(Object val)
    {
        if (val instanceof Long) {
            mValue = (long) val;
        } else if (val instanceof Date) {
            mValue = ((Date) val).getTime();
        } else if (val instanceof Calendar) {
            mValue = ((Calendar) val).getTimeInMillis();
        } else {
            return;
        }

        setText(getFormattedValue(mValue));

        setSingleLine(true);
        setFocusable(false);
        setOnClickListener(getDateUpdateWatcher(DATETIME));

        String pattern = mDateFormat.toLocalizedPattern();
        setHint(pattern);
    }
}
