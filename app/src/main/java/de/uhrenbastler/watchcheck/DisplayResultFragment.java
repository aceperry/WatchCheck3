package de.uhrenbastler.watchcheck;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.shamanland.fab.FloatingActionButton;
import com.shamanland.fab.ShowHideOnScroll;

import java.util.List;

import de.uhrenbastler.watchcheck.managers.ResultManager;
import de.uhrenbastler.watchcheck.tools.Logger;
import de.uhrenbastler.watchcheck.views.ResultListAdapter;
import watchcheck.db.Log;

/**
 * Created by clorenz on 17.12.14.
 */
public class DisplayResultFragment extends Fragment {

    // Store instance variables
    private List<Log> log;

    // newInstance constructor for creating fragment with arguments
    public static DisplayResultFragment newInstance(Long watchId, int page) {
        Logger.debug("Starting new instance of ResultFragment for watchId="+watchId+" and page="+page);
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
        long watchId=getArguments().getLong("watchId");
        int period=getArguments().getInt("period");
        log = ResultManager.getLogsForWatchAndPeriod(getActivity().getApplicationContext(),watchId, period);


        Logger.debug("watchId="+watchId+", period="+period+"="+log);
    }

    // Inflate the view for the fragment based on layout XML
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_display_result, container, false);
        ListView listView = (ListView) view.findViewById(R.id.resultListView);
        ListAdapter resultListAdapter = new ResultListAdapter(this.getActivity().getApplicationContext(), log);
        listView.setAdapter(resultListAdapter);

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.buttonAddLog);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent addLogIntent = new Intent(getActivity(),AddLogActivity.class);
                startActivity(addLogIntent);
            }
        });
        listView.setOnTouchListener(new ShowHideOnScroll(fab));


        return view;
    }

}
