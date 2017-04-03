package com.udacity.stockhawk.widget;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.data.StockProvider;
import com.udacity.stockhawk.ui.StockDetailActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Created by chart on 31/3/2017.
 */

public class StockWidgetService extends RemoteViewsService {





    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockWidgetViewFactory(getApplicationContext());
    }

        private class StockWidgetViewFactory implements RemoteViewsFactory {
            private final Context mApplicationContext;
            private final DecimalFormat dollarFormat,dollarFormatWithPlus,percentageFormat;



         public   StockWidgetViewFactory(Context applicationContext){
             mApplicationContext = applicationContext;
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");
                percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");

    }



            private List<ContentValues> mCvList = new ArrayList<>();



            @Override
        public void onCreate() {

                getData();

            }

            private void getData() {
                mCvList.clear();

                long identity = Binder.clearCallingIdentity();
                try {
                    ContentResolver contentResolver = mApplicationContext.getContentResolver();
                    Cursor cursor = contentResolver.query(
                            Contract.Quote.URI
                            , null
                            , null
                            , null
                            , null);
                    while (cursor.moveToNext()) {

                        String symbol = cursor.getString(cursor.getColumnIndex(Contract.Quote.COLUMN_SYMBOL));
                        float price = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PRICE));
                        float absChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_ABSOLUTE_CHANGE));
                        float percentChange = cursor.getFloat(cursor.getColumnIndex(Contract.Quote.COLUMN_PERCENTAGE_CHANGE));


                        ContentValues cv = new ContentValues();

                        cv.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                        cv.put(Contract.Quote.COLUMN_PRICE, price);
                        cv.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, absChange);
                        cv.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                        mCvList.add(cv);

                    }
                    cursor.close();
                }finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }

            @Override
        public void onDataSetChanged() {
            getData();
        }

        @Override
        public void onDestroy() {

        }

        @Override
        public int getCount() {
            return mCvList.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {

            ContentValues cv = mCvList.get(position);
            RemoteViews remoteViews = new RemoteViews(
                    mApplicationContext.getPackageName()
                    ,R.layout.list_item_quote);

            String symbol = cv.getAsString(Contract.Quote.COLUMN_SYMBOL);

            remoteViews.setTextViewText(R.id.symbol,cv.getAsString(Contract.Quote.COLUMN_SYMBOL));
            remoteViews.setTextViewText(R.id.price,dollarFormat.format(cv.getAsFloat(Contract.Quote.COLUMN_PRICE)));

            float abdChange = cv.getAsFloat(Contract.Quote.COLUMN_ABSOLUTE_CHANGE);
            float perChange = cv.getAsFloat(Contract.Quote.COLUMN_PERCENTAGE_CHANGE);

            if(abdChange>0){

                remoteViews.setInt(R.id.change,"setBackgroundResource",R.drawable.percent_change_pill_green);
            }else {
                remoteViews.setInt(R.id.change,"setBackgroundResource",R.drawable.percent_change_pill_red);
            }
            remoteViews.setTextViewText(R.id.change, percentageFormat.format(perChange/100));

            Intent fillIntent = new Intent(mApplicationContext, StockDetailActivity.class);
            fillIntent.putExtra(StockWidgetProvider.EXTRA_SYMBOL,symbol);
            remoteViews.setOnClickFillInIntent(R.id.list_item,fillIntent);

            return remoteViews;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
