package com.yudiandreanp.aware_d;
import java.io.IOException;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBReader
{
    protected static final String TAG = "DataAdapter";

    private final Context mContext;
    private SQLiteDatabase mDb;
    private DataBaseHelper mDbHelper;

    public DBReader(Context context)
    {
        this.mContext = context;
        mDbHelper = new DataBaseHelper(mContext);
    }

    public DBReader createDatabase() throws SQLException
    {
        try
        {
            mDbHelper.createDataBase();
        }
        catch (IOException mIOException)
        {
            Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
        return this;
    }

    public DBReader open() throws SQLException
    {
        try
        {
            mDbHelper.openDataBase();
            mDbHelper.close();
            mDb = mDbHelper.getReadableDatabase();
        }
        catch (SQLException mSQLException)
        {
            Log.e(TAG, "open >>"+ mSQLException.toString());
            throw mSQLException;
        }
        return this;
    }

    public void close()
    {
        mDbHelper.close();
    }

    public Cursor getQuestion(int index)
    {
        try
        {
            String sql ="SELECT question_text FROM Questions_Answers WHERE _id = " + index;

            Cursor mCur = mDb.rawQuery(sql, null);
            Log.e(TAG, "got the questions!");
            if (mCur!=null)
            {
                mCur.moveToNext();
            }
            return mCur;
        }

        catch (SQLException mSQLException)
        {
            Log.e(TAG, "getTestData >>"+ mSQLException.toString());
            throw mSQLException;
        }
    }


    public Cursor getAnswer(int index)
    {
        try
        {
            String sql ="SELECT answer_text FROM Questions_Answers WHERE _id = " + index;

            Cursor mCur = mDb.rawQuery(sql, null);
            Log.e(TAG, "got the answer!");
            if (mCur!=null)
            {
                mCur.moveToNext();
            }
            return mCur;
        }

        catch (SQLException mSQLException)
        {
            Log.e(TAG, "getTestData >>"+ mSQLException.toString());
            throw mSQLException;
        }
    }
}