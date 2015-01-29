package de.uhrenbastler.watchcheck;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gc.materialdesign.views.ButtonFloat;
import java.text.SimpleDateFormat;
import java.util.List;

import de.uhrenbastler.watchcheck.managers.ResultManager;
import de.uhrenbastler.watchcheck.tools.Logger;
import de.uhrenbastler.watchcheck.views.ResultListAdapter;
import watchcheck.db.Log;
import watchcheck.db.LogDao;
import watchcheck.db.Watch;
import watchcheck.db.WatchDao;

/**
 * Created by clorenz on 17.12.14.
 */
public class DisplayResultFragment extends Fragment {

    // Store instance variables
    private Watch currentWatch;
    private List<Log> log=null;
    private Log lastLog;
    private long watchId;
    private int period;
    private ArrayAdapter resultListAdapter;
    private ListView listView;
    private TextView averageDeviation;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");


    // newInstance constructor for creating fragment with arguments
    public static DisplayResultFragment newInstance(Long watchId, int page) {
        DisplayResultFragment fragmentFirst = new DisplayResultFragment();
        Bundle args = new Bundle();
        args.putLong("watchId", watchId);
        args.putInt("period", page);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    // Store instance variables based on arguments passed
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        watchId=getArguments().getLong("watchId");
        period=getArguments().getInt("period");
        log = ResultManager.getLogsForWatchAndPeriod(getActivity().getApplicationContext(),watchId, period);
        lastLog = ResultManager.getLastLogForWatch(getActivity().getApplicationContext(),watchId);
        WatchDao watchDao = ((WatchCheckApplication)getActivity().getApplicationContext()).getDaoSession().getWatchDao();
        currentWatch = watchDao.load(watchId);
    }


    @Override
    public void onResume() {
        super.onResume();
        log = ResultManager.getLogsForWatchAndPeriod(getActivity().getApplicationContext(),watchId, period);
        lastLog = ResultManager.getLastLogForWatch(getActivity().getApplicationContext(),watchId);
        if ( listView!=null ) {
            resultListAdapter.clear();
            resultListAdapter.addAll(log);
            resultListAdapter.notifyDataSetChanged();
            listView.invalidateViews();
        }
        calculateAverageDeviation();


    }

    private void calculateAverageDeviation() {
        Logger.debug("Avg.deviation");
        // avg. deviation
        if ( averageDeviation!=null) {
            String avgDeviationFormat = getString(R.string.list_average_deviation);
            if (log.size() > 1) {
                // We can calculate the avg. deviation only if we have at least one daily rate!
                long diffReferenceMillis = log.get(log.size() - 1).getReferenceTime().getTime() - log.get(0).getReferenceTime().getTime();
                long diffWatchMillis = log.get(log.size() - 1).getWatchTime().getTime() - log.get(0).getWatchTime().getTime();

                double diffReferenceInDays = (double) diffReferenceMillis / (double) 86400000d;
                double avgDeviation = ((double) diffWatchMillis / diffReferenceInDays) / 1000 - 86400d;

                Logger.debug("Avg deviation=" + avgDeviation);

                averageDeviation.setText(String.format(avgDeviationFormat, avgDeviation));
            } else {
                averageDeviation.setText(getString(R.string.list_no_average_deviation));
            }
        }
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_result, container, false);
        listView = (ListView) view.findViewById(R.id.resultListView);
        averageDeviation = (TextView) view.findViewById(R.id.result_footer);
        resultListAdapter = new ResultListAdapter(this.getActivity().getApplicationContext(), log);
        listView.setAdapter(resultListAdapter);
        registerForContextMenu(listView);

        ButtonFloat fab = (ButtonFloat) getActivity().findViewById(R.id.buttonAddLog);
        fab.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        Logger.debug("Setting button color");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent checkWatchIntent = new Intent(getActivity(),CheckWatchActivity.class);
                checkWatchIntent.putExtra(CheckWatchActivity.EXTRA_WATCH, currentWatch);
                checkWatchIntent.putExtra(CheckWatchActivity.EXTRA_LAST_LOG, lastLog);
                startActivity(checkWatchIntent);
            }
        });
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });

        /*
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log log = (Log) parent.getItemAtPosition(position);
                Logger.debug("Position="+position+", id="+id+", item="+log.getId());
                return false;
            }
        });
        */

        Logger.debug("Before avg. deviation");
        calculateAverageDeviation();

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        v.setBackgroundColor(getResources().getColor(R.color.background_material_light));
        if ( v.getId() == R.id.resultListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Log logToHandle = (Log)listView.getAdapter().getItem(info.position);
            info.id=logToHandle.getId();            // Yuck!
            menu.setHeaderTitle("Log from "+sdf.format(logToHandle.getReferenceTime()));
            String[] menuItems = getResources().getStringArray(R.array.resultlist_contextmenu);
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        int menuItemIndex = item.getItemId();

        LogDao logDao = ((WatchCheckApplication)getActivity().getApplicationContext()).getDaoSession().getLogDao();
        Log logToHandle = logDao.load(info.id);


        switch ( menuItemIndex ) {
            case 0: Logger.debug("Edit item "+logToHandle.getReferenceTime());
                Intent addLogIntent = new Intent(this.getActivity(),AddLogActivity.class);
                addLogIntent.putExtra(AddLogActivity.EXTRA_WATCH, currentWatch);
                addLogIntent.putExtra(AddLogActivity.EXTRA_EDIT_LOG, logToHandle);
                startActivity(addLogIntent);
                if ( listView!=null ) {
                    resultListAdapter.clear();
                    resultListAdapter.addAll(log);
                    resultListAdapter.notifyDataSetChanged();
                    listView.invalidateViews();
                }
                break;
            case 1: Logger.debug("Delete item "+logToHandle.getReferenceTime());
                displayDeleteDialog(info.id,sdf.format(logToHandle.getReferenceTime()) );
                break;
        }



        return true;
    }


    private void displayDeleteDialog(final long logId, final String dateString) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Logger.debug("Delete log "+logId);
                        LogDao logDao = ((WatchCheckApplication)getActivity().getApplicationContext()).getDaoSession().getLogDao();
                        logDao.deleteByKey(logId);
                        Toast.makeText(getActivity().getApplicationContext(), String.format(getString(R.string.deletedLogEntry),
                                dateString), Toast.LENGTH_SHORT).show();
                        log = ResultManager.getLogsForWatchAndPeriod(getActivity().getApplicationContext(),watchId, period);
                        if ( listView!=null ) {
                            resultListAdapter.clear();
                            resultListAdapter.addAll(log);
                            resultListAdapter.notifyDataSetChanged();
                            listView.invalidateViews();
                        }
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder deleteWatchAlertDialog = new AlertDialog.Builder(DisplayResultFragment.this.getActivity());
        deleteWatchAlertDialog.setMessage(String.format(getString(R.string.deleteLogQuestion), dateString))
                .setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener)
                .show();
    };
}
