/*
 * Copyright (C) 2012 TopCoder Inc., All Rights Reserved.
 */
package com.lib;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Reordable list view.
 *
 * @author TCSASSEMBLER
 * @version 1.0
 */
public class ReordableListView extends ListView implements View.OnTouchListener {
    /** Indicates if user is dragging view item. */
    private boolean isDragging;
    /** Start position of the item. */
    private int startPosition;
    /** Start offset to item's top. */
    private int startOffset;
    /** Image view id, is used to drag items. */
    private int imageViewId;

    /** The view that is dragged. */
    private View dragView;
    /** The layout params of the view. */
    private RelativeLayout.LayoutParams layoutParams;
    private View startItem;

    /**
     * Constructor for ReordableListView type.
     *
     * @param context the context
     */
    public ReordableListView(Context context) {
        this(context, null);
    }

    /**
     * Constructor for ReordableListView type.
     *
     * @param context the context
     * @param attrs the xml attributes
     */
    public ReordableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
    }

    /**
     * Sets drag image view id.
     *
     * @param id the drag image id
     */
    public void setImageViewId(int id) {
        imageViewId = id;
    }

    /**
     * Checks if cursor is inside the drag image.
     *
     * @param x the x cursor position
     * @return true if cursor is inside
     */
    protected boolean isInsideImageView(int x) {
        View imageView = findViewById(imageViewId);
        if (imageView == null) {
            return true;
        }
        return imageView.getLeft() < x && x < imageView.getRight();
    }

    /**
     * Override of onTouch event.
     *
     * @param v the touched view
     * @param ev the motion event
     * @return true, if event was handled
     */
    @Override
    public boolean onTouch(View v, MotionEvent ev) {
        final int action = ev.getAction();
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (action == MotionEvent.ACTION_DOWN && isInsideImageView(x)) {
            isDragging = true;
        }

        Log.i("reorder", "Dragging: " + isDragging + ", action: " + action);

        if (!isDragging) {
            return super.onTouchEvent(ev);
        }

        int firstVisiblePosition = getFirstVisiblePosition();
        int lastVisiblePosition = getLastVisiblePosition();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            startPosition = pointToPosition(x, y);
            if (startPosition != INVALID_POSITION) {
                int itemIndex = startPosition - firstVisiblePosition;
                startItem = getChildAt(itemIndex);
                startOffset = y - startItem.getTop();
                onStartDrag(itemIndex, y - startOffset);
            } else {
                isDragging = false;
            }
            break;
        case MotionEvent.ACTION_MOVE:
            onDrag(y - startOffset);
            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:
        default:
            isDragging = false;
            int endPosition = pointToPosition(x, y);
            if (startPosition != INVALID_POSITION) {
                if (endPosition == INVALID_POSITION) {
                    // try to paste at start to end
                    if (getChildAt(firstVisiblePosition).getTop() > y) {
                        endPosition = firstVisiblePosition;
                    } else if (getChildAt(lastVisiblePosition).getBottom() < y) {
                        endPosition = lastVisiblePosition;
                    }
                }

                if (endPosition != INVALID_POSITION) {
                    ((ReordableAdapter<?>) getAdapter()).onDrop(startPosition, endPosition);
                }
            }

            // finalize dragging
            onStopDrag();
            invalidateViews();
            break;
        }
        return true;
    }

    /**
     * Executed while dragging to update position of dragged item.
     *
     * @param y the y coordinate of item
     */
    private void onDrag(int y) {
        if (dragView != null) {
            layoutParams.topMargin = y;
            dragView.setLayoutParams(layoutParams);
        }
    }

    /**
     * Executed dragging is started create dragged item.
     *
     * @param itemIndex the index of
     * @param y the y coordinate of item
     */
    private void onStartDrag(int itemIndex, int y) {
        startItem.setVisibility(View.INVISIBLE);

        dragView = getAdapter().getView(itemIndex, null, this);
        layoutParams = new RelativeLayout.LayoutParams(startItem.getWidth(), startItem.getHeight());
        layoutParams.topMargin = y;
        ((ViewGroup) getParent()).addView(dragView, layoutParams);
    }

    /**
     * Executed when dragging is finished to update views.
     *
     * @param itemView the clicked item
     */
    public void onStopDrag() {
        if (startItem != null) {
            startItem.setVisibility(View.VISIBLE);
            startItem = null;
        }
        if (dragView != null) {
            ((ViewGroup) getParent()).removeView(dragView);
            dragView = null;
        }
    }

    /**
     * List adapter that support reordering.
     *
     * @author TCSASSEMBLER
     * @version 1.0
     */
    public static class ReordableAdapter<T> extends BaseAdapter {

        /** The id of text item inside the layout. */
        private final int textViewId;
        /** The item layout id. */
        private final int itemLayoutId;
        /** Data content. */
        private final List<T> content;

        /**
         * Constructor for ReordableAdapter type.
         *
         * @param itemLayoutId the list item layout
         * @param textViewId the text view id inside layout
         * @param content the data content
         */
        public ReordableAdapter(int itemLayoutId, int textViewId, List<T> content) {
            this.textViewId = textViewId;
            this.itemLayoutId = itemLayoutId;
            this.content = content;
        }

        /**
         * The number of items in the list.
         *
         * @see android.widget.ListAdapter#getCount()
         */
        @Override
        public int getCount() {
            return content.size();
        }

        /**
         * Since the data comes from an array, just returning the index is sufficient to get at the data. If we were using a more complex
         * data structure, we would return whatever object represents one row in the list.
         *
         * @see android.widget.ListAdapter#getItem(int)
         */
        @Override
        public T getItem(int position) {
            return content.get(position);
        }

        /**
         * Use the array index as a unique id.
         *
         * @see android.widget.ListAdapter#getItemId(int)
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a dragView to hold each row.
         *
         * @see android.widget.ListAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(itemLayoutId, null);
            }

            TextView textView = (TextView) convertView.findViewById(textViewId);
            textView.setText(content.get(position).toString());
            return convertView;
        }

        /**
         * Executed when adapter is requested to drop item from one position to another.
         *
         * @param from the start position of the item
         * @param to the drop position of the item
         */
        public void onDrop(int from, int to) {
            T temp = content.get(from);
            content.remove(from);
            content.add(to, temp);
        }
    }
}