/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoLineString;
import com.nextgis.maplib.datasource.GeoMultiLineString;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoMultiPolygon;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.datasource.GeoPolygon;
import com.nextgis.maplib.map.MapDrawable;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.Constants;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.VectorCacheItem;
import com.nextgis.maplibui.api.Overlay;

import java.util.ArrayList;
import java.util.List;


/**
 * The class for edit vector features
 */
public class EditLayerOverlay
        extends Overlay
{
    public final static int MODE_HIGHLIGHT = 1;
    public final static int MODE_EDIT      = 2;

    protected final static int VERTEX_RADIUS = 20;
    protected final static int EDGE_RADIUS = 12;
    protected final static int LINE_WIDTH = 4;

    protected VectorLayer mLayer;
    protected VectorCacheItem mItem;
    protected int mMode;
    protected Paint mPaint;
    protected int mFillColor;
    protected int mOutlineColor;
    protected int mSelectColor;
    protected DrawItems mDrawItems;


    public EditLayerOverlay(
            Context context,
            MapViewOverlays mapViewOverlays)
    {
        super(context, mapViewOverlays);
        mLayer = null;
        mItem = null;
        mMode = MODE_HIGHLIGHT;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mFillColor = mContext.getResources().getColor(R.color.accent);
        mOutlineColor = Color.BLACK;
        mSelectColor = Color.RED;

        //mPaint.setAlpha(190);

        mDrawItems = new DrawItems();
    }


    public void setMode(int mode)
    {
        mMode = mode;
    }


    public void setFeature(
            VectorLayer layer,
            VectorCacheItem item)
    {
        mLayer = layer;
        mItem = item;

        mMapViewOverlays.postInvalidate();
    }


    @Override
    public void draw(
            Canvas canvas,
            MapDrawable mapDrawable)
    {
        if (null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if (null == geom)
            return;

        fillDrawItems(geom, mapDrawable);

        switch (geom.getType()){
            case GeoConstants.GTPoint:
            case GeoConstants.GTMultiPoint:
                mDrawItems.drawPoints(canvas);
                break;
            case GeoConstants.GTLineString:
            case GeoConstants.GTMultiLineString:
            case GeoConstants.GTPolygon:
            case GeoConstants.GTMultiPolygon:
                mDrawItems.drawLines(canvas);
                break;
            default:
                break;
        }
    }

    protected void fillDrawItems(GeoGeometry geom, MapDrawable mapDrawable){
        mDrawItems.clear();

        GeoPoint[] geoPoints = null;
        float[] points = null;
        GeoLineString lineString = null;
        switch (geom.getType()) {
            case GeoConstants.GTPoint:
                geoPoints = new GeoPoint[1];
                geoPoints[0] = (GeoPoint) geom;
                points = mapDrawable.mapToScreen(geoPoints);
                mDrawItems.addItems(0, points, DrawItems.TYPE_VERTEX);
                break;
            case GeoConstants.GTMultiPoint:
                GeoMultiPoint geoMultiPoint = (GeoMultiPoint) geom;
                geoPoints = new GeoPoint[geoMultiPoint.size()];
                for (int i = 0; i < geoMultiPoint.size(); i++) {
                    geoPoints[i] = geoMultiPoint.get(i);
                }
                points = mapDrawable.mapToScreen(geoPoints);
                mDrawItems.addItems(0, points, DrawItems.TYPE_VERTEX);
                break;
            case GeoConstants.GTLineString:
                lineString = (GeoLineString)geom;
                fillDrawLine(0, lineString, mapDrawable);
                break;
            case GeoConstants.GTMultiLineString:
                GeoMultiLineString multiLineString = (GeoMultiLineString)geom;
                for(int i = 0; i < multiLineString.size(); i++){
                    fillDrawLine(i, multiLineString.get(i), mapDrawable);
                }
                break;
            case GeoConstants.GTPolygon:
                GeoPolygon polygon = (GeoPolygon)geom;
                fillDrawPolygon(polygon, mapDrawable);
                break;
            case GeoConstants.GTMultiPolygon:
                GeoMultiPolygon multiPolygon = (GeoMultiPolygon)geom;
                for(int i = 0; i < multiPolygon.size(); i++){
                    fillDrawPolygon(multiPolygon.get(i), mapDrawable);
                }
                break;
            case GeoConstants.GTGeometryCollection:
            default:
                break;
        }
    }

    protected void fillDrawPolygon(GeoPolygon polygon, MapDrawable mapDrawable){
        fillDrawLine(0, polygon.getOuterRing(), mapDrawable);
        for(int i = 0; i < polygon.getInnerRingCount(); i++){
            fillDrawLine(i + 1, polygon.getInnerRing(i), mapDrawable);
        }
    }

    protected void fillDrawLine(int ring, GeoLineString lineString, MapDrawable mapDrawable){
        GeoPoint[] geoPoints = lineString.getPoints().toArray(new GeoPoint[lineString.getPoints().size()]);
        float[] points = mapDrawable.mapToScreen(geoPoints);
        mDrawItems.addItems(ring, points, DrawItems.TYPE_VERTEX);
        float[] edgePoints = new float[points.length - 1];
        for(int i = 0; i < points.length - 1; i++){
            edgePoints[i] = (points[i] + points[i + 1]) * .5f;
        }
        mDrawItems.addItems(ring, edgePoints, DrawItems.TYPE_EDGE);
    }

    @Override
    public void drawOnPanning(
            Canvas canvas,
            PointF mCurrentMouseOffset)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;
    }


    @Override
    public void drawOnZooming(
            Canvas canvas,
            PointF mCurrentFocusLocation,
            float scale)
    {
        if(null == mItem)
            return;
        GeoGeometry geom = mItem.getGeoGeometry();
        if(null == geom)
            return;
    }

    protected class DrawItems{
        List<float[]> mDrawItemsVertex;
        List<float[]> mDrawItemsEdge;

        public static final int TYPE_VERTEX = 1;
        public static final int TYPE_EDGE   = 2;

        protected int mSelectedRing, mSelectedPoint;


        public DrawItems()
        {
            mDrawItemsVertex = new ArrayList<>();
            mDrawItemsEdge = new ArrayList<>();

            mSelectedRing = Constants.NOT_FOUND;
            mSelectedPoint = Constants.NOT_FOUND;
        }


        public void setSelectedRing(int selectedRing)
        {
            mSelectedRing = selectedRing;
        }


        public void setSelectedPoint(int selectedPoint)
        {
            mSelectedPoint = selectedPoint;
        }


        public void addItems(
                int ring,
                float[] points,
                int type)
        {
            if(type == TYPE_VERTEX)
                mDrawItemsVertex.add(ring, points);
            else if(type == TYPE_EDGE)
                mDrawItemsEdge.add(ring, points);
        }


        public void clear()
        {
            mDrawItemsVertex.clear();
            mDrawItemsEdge.clear();
        }


        public void drawPoints(Canvas canvas)
        {
            for (float[] items : mDrawItemsVertex) {

                mPaint.setColor(mOutlineColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
                canvas.drawPoints(items, mPaint);

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(VERTEX_RADIUS);
                canvas.drawPoints(items, mPaint);
            }

            //draw selected point
            if(mSelectedRing != Constants.NOT_FOUND && mSelectedPoint != Constants.NOT_FOUND) {

                float[] items = mDrawItemsVertex.get(mSelectedRing);
                if(null != items) {
                    mPaint.setColor(mSelectColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);

                    canvas.drawPoint(items[mSelectedPoint], items[mSelectedPoint + 1], mPaint);
                }
            }
        }

        public void drawLines(Canvas canvas)
        {
            for (float[] items : mDrawItemsVertex) {

                mPaint.setColor(mFillColor);
                mPaint.setStrokeWidth(LINE_WIDTH);
                canvas.drawLines(items, mPaint);

                if(mMode == MODE_EDIT) {
                    mPaint.setColor(mOutlineColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS + 2);
                    canvas.drawPoints(items, mPaint);

                    mPaint.setColor(mFillColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);
                    canvas.drawPoints(items, mPaint);
                }
            }

            if(mMode == MODE_EDIT) {
                for (float[] items : mDrawItemsEdge) {

                    mPaint.setColor(mOutlineColor);
                    mPaint.setStrokeWidth(EDGE_RADIUS + 2);
                    canvas.drawPoints(items, mPaint);

                    mPaint.setColor(mFillColor);
                    mPaint.setStrokeWidth(EDGE_RADIUS);
                    canvas.drawPoints(items, mPaint);
                }
            }

            //draw selected point
            if(mSelectedRing != Constants.NOT_FOUND && mSelectedPoint != Constants.NOT_FOUND) {

                float[] items = mDrawItemsVertex.get(mSelectedRing);
                if(null != items) {
                    mPaint.setColor(mSelectColor);
                    mPaint.setStrokeWidth(VERTEX_RADIUS);

                    canvas.drawPoint(items[mSelectedPoint], items[mSelectedPoint + 1], mPaint);
                }
            }
        }
    }
}