/*
 * Copyright (C) 2012 TopCoder Inc., All Rights Reserved.
 */
package gov.nasa.pds.android;

import gov.nasa.pds.lessons.Lesson;
import gov.nasa.pds.lessons.LessonRepository;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.AbstractAction;

public class LessonsActivity extends Activity {
    private BaseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lessons);

        // set action bar
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle("Lessons");

        // add new lesson action
        actionBar.addAction(new AbstractAction(R.drawable.compare_add, "Add") {
            @Override
            public void performAction(View view) {
                startActivity(new Intent(LessonsActivity.this, LessonActivity.class));
            }
        });

        // browse action
        actionBar.addAction(new AbstractAction(R.drawable.remove, "Browse") {
            @Override
            public void performAction(View view) {
                Toast.makeText(getApplicationContext(), "Browse", Toast.LENGTH_SHORT).show();
            }
        });

        // set empty adapter
        adapter = new BaseAdapter() {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = View.inflate(LessonsActivity.this, android.R.layout.simple_list_item_1, null);
                }

                // update lesson view
                final Lesson lesson = LessonRepository.getLessons().get(position);
                TextView captionView = (TextView) convertView.findViewById(android.R.id.text1);
                captionView.setText(lesson.getName());
                captionView.setTag(lesson.getId());
                convertView.setTag(lesson.getId());
                convertView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(LessonsActivity.this, LessonActivity.class);
                        intent.putExtra(LessonActivity.EXTRA_LESSON_ID, (Integer) v.getTag());
                        startActivity(intent);
                    }
                });
                return convertView;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public String getItem(int position) {
                Lesson lesson = LessonRepository.getLessons().get(position);
                return lesson == null ? "" : lesson.getName();
            }

            @Override
            public int getCount() {
                return LessonRepository.size();
            }
        };
        ListView listView = (ListView) findViewById(R.id.lessonsList);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // refresh adapter
        adapter.notifyDataSetChanged();
    }
}
