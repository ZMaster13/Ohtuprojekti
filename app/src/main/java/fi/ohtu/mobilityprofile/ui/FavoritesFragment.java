package fi.ohtu.mobilityprofile.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orm.query.Select;

import java.util.ArrayList;
import java.util.List;

import fi.ohtu.mobilityprofile.R;
import fi.ohtu.mobilityprofile.data.RouteSearch;

/**
 * The class creates a component called FavoritesFragment.
 *
 * FavoritesFragment handles everything concerning the FAVORITES tab in the UI.
 */
public class FavoritesFragment extends Fragment {

    /**
     * The title of the fragment.
     */
    private static final String title = "FAVORITES";

    /**
     * The position of the fragment in the "queue" of all fragments.
     */
    private static final int page = 3;
    private Context context;

    /**
     * Creates a new instance of ProfileFragment.
     * @return
     */
    public static FavoritesFragment newInstance() {
        FavoritesFragment favoritesFragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putInt("page", page);
        args.putString("title", title);
        favoritesFragment.setArguments(args);
        return favoritesFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onViewCreated(View view, final Bundle savedInstanceState) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.favorites_fragment, container, false);
    }
}