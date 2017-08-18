package com.example.weilun.birthdayreminder;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
//import com.example.weilun.birthdayreminder.NotifyIntentService.CursorWrapper;
import com.example.weilun.birthdayreminder.db.PersonContract;
import com.example.weilun.birthdayreminder.db.PersonDBHelper;
import com.example.weilun.birthdayreminder.db.PersonDBQueries;

import org.w3c.dom.Text;

import java.util.Calendar;

public class TodayBirthdayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_birthday);

//        Intent intent = getIntent();
//        CursorWrapper cursorWrapper = (CursorWrapper) intent.getSerializableExtra(NotifyIntentService.CURSOR);
//        Cursor cursor = cursorWrapper.getCursor();

        Calendar calender = Calendar.getInstance();
        calender.add(Calendar.DAY_OF_MONTH, -1);
        PersonDBQueries dbQuery = new PersonDBQueries(new PersonDBHelper(this));
        String[] columns = PersonContract.columns;
        String[] selectionArgs = {calender.getTimeInMillis() + "", "" + calender.getTimeInMillis()};
        //to convert millisecond to Unix timestamp, divide by 1000
        Cursor cursor = dbQuery.query(columns, "strftime('%m-%d'," + PersonContract.PersonEntry.COLUMN_NAME_DOB + "/1000, 'unixepoch')"
                        + " BETWEEN strftime('%m-%d',?/1000, 'unixepoch') AND strftime('%m-%d',?/1000, 'unixepoch')"
                        + "AND " + PersonContract.PersonEntry.COLUMN_NAME_NOFITY + " = '1'",
                selectionArgs, null, null, null);

        ListView listView = (ListView) findViewById(R.id.listview);
        ProgressBar loadingBar = (ProgressBar)findViewById(R.id.loading_bar);

        PersonCursorAdapter adapter = new PersonCursorAdapter(this, cursor, 0);
        listView.setAdapter(adapter);
        loadingBar.setVisibility(View.GONE);
    }
}
