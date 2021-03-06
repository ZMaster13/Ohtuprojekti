package fi.ohtu.mobilityprofile.ui.fragments;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import fi.ohtu.mobilityprofile.LicensesActivity;
import fi.ohtu.mobilityprofile.MainActivity;
import fi.ohtu.mobilityprofile.data.GpsPointDao;
import fi.ohtu.mobilityprofile.data.PlaceDao;
import fi.ohtu.mobilityprofile.data.VisitDao;
import fi.ohtu.mobilityprofile.domain.Coordinate;
import fi.ohtu.mobilityprofile.domain.GpsPoint;
import fi.ohtu.mobilityprofile.domain.Place;
import fi.ohtu.mobilityprofile.domain.RouteSearch;
import fi.ohtu.mobilityprofile.domain.Visit;
import fi.ohtu.mobilityprofile.suggestions.locationHistory.PlaceRecorder;
import fi.ohtu.mobilityprofile.R;
import fi.ohtu.mobilityprofile.data.RouteSearchDao;
import fi.ohtu.mobilityprofile.ui.Transport;
import fi.ohtu.mobilityprofile.util.PermissionManager;
import fi.ohtu.mobilityprofile.util.ProfileBackup;

/**
 * SettingsFragment handles everything concerning the SETTINGS tab in the UI.
 */
public class SettingsFragment extends Fragment {

    private static final String title = "SETTINGS";
    private static final int page = 2;

    private static final int ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST = 1;
    private static final int READ_CALENDAR_PERMISSIONS_REQUEST = 2;

    private CheckBox gpsCheckBox;
    private CheckBox calendarCheckBox;

    private Button resetAllButton;
    private Button backUpButton;
    private Button licenses;
    private Context context;

    private ResultReceiver resultReceiver;

    /**
     * Creates a new instance of SettingsFragment.
     * @return SettingsFragment
     */
    public static SettingsFragment newInstance() {
        SettingsFragment settingsFragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putInt("page", page);
        args.putString("title", title);
        settingsFragment.setArguments(args);
        return settingsFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, final Bundle savedInstanceState) {

        Transport transport = new Transport();
        transport.initializeTransportModes(view);

        findViewElements(view);
        setChecked();
        setListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        setupServiceReceiver();

        if (isLocationServiceRunning()) {
            Intent intent = new Intent(context, PlaceRecorder.class);
            intent.putExtra("Receiver", resultReceiver);
            intent.putExtra("UPDATE", true);
            context.startService(intent);
        }
    }

    public void setupServiceReceiver() {
        resultReceiver = new ResultReceiver(new Handler()) {
            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultCode == 100) {
                    gpsCheckBox.setChecked(false);
                }
            }
        };
    }

    private void findViewElements(View view) {
        gpsCheckBox = (CheckBox) view.findViewById(R.id.settings_gps_checkbox);
        calendarCheckBox = (CheckBox) view.findViewById(R.id.settings_calendar_checkbox);

        resetAllButton = (Button) view.findViewById(R.id.settings_reset);
        backUpButton = (Button) view.findViewById(R.id.settings_backup_button);
        licenses = (Button) view.findViewById(R.id.settings_other_info_licenses_button);
    }

    /**
     * Sets the checkboxes checked or unchecked based on the states of the permissions.
     */
    private void setChecked() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());
        String gps = sharedPref.getString("gps", "Not Available");
        String cal = sharedPref.getString("cal", "Not Available");

        if (!PermissionManager.permissionToFineLocation(context) || gps.equals("false")) {
            gpsCheckBox.setChecked(false);
        } else if (PermissionManager.permissionToFineLocation(context) && gps.equals("true")) {
            gpsCheckBox.setChecked(true);
        } else {
            gpsCheckBox.setChecked(false);
        }

        if (!PermissionManager.permissionToReadCalendar(context) || cal.equals("false")) {
            calendarCheckBox.setChecked(false);
        } else if (PermissionManager.permissionToReadCalendar(context) && cal.equals("true")) {
            calendarCheckBox.setChecked(true);
        } else {
            calendarCheckBox.setChecked(false);
        }
    }

    private void setListeners() {
        setListenerForGPSCheckBox();
        setListenerForCheckBoxCalendar();

        setListenerForResetAllButton();
        setListenerForBackupButton();
        setListenerForLicensesButton();
    }

    /**
     * Sets a listener for gpsCheckbox.
     */
    private void setListenerForGPSCheckBox() {
        gpsCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

                if (isChecked) {
                    if (!PermissionManager.permissionToFineLocation(context)) {
                        getPermissionToAccessFineLocation();
                    } else {

                        Intent intent = new Intent(context, PlaceRecorder.class);
                        intent.putExtra("Receiver", resultReceiver);
                        context.startService(intent);

                        Toast.makeText(context, R.string.location_tracking_start, Toast.LENGTH_SHORT).show();

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("gps", "true");
                        editor.commit();
                    }
                } else {
                    context.stopService(new Intent(context, PlaceRecorder.class));
                    Toast.makeText(context, R.string.location_tracking_stop, Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("gps", "false");
                    editor.commit();
                }
            }
        });
    }

    /**
     * Sets a listener for calendarCheckBox.
     */
    private void setListenerForCheckBoxCalendar() {
        calendarCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !PermissionManager.permissionToReadCalendar(context)) {
                    getPermissionToReadCalendar();
                } else if (isChecked) {
                    Toast.makeText(context, R.string.settings_toast_calendar_used_again, Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("cal", "true");
                    editor.commit();
                } else {
                    Toast.makeText(context, R.string.settings_toast_calendar_not_used, Toast.LENGTH_SHORT).show();

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("cal", "false");
                    editor.commit();
                }
            }
        });
    }

    /**
     * Creates alert dialog to confirm resetting of the app when reset button is clicked.
     */
    private void setListenerForResetAllButton() {
        resetAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getActivity());
                builder
                        .setTitle(R.string.dialog_settings_reset_title).setMessage(R.string.dialog_settings_reset_info)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                deleteAllDataFromDatabase();
                                updateView();
                            }})
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void setListenerForBackupButton() {
        backUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileBackup profileBackup = new ProfileBackup(context);
                profileBackup.handleBackup("back up");
            }
        });
    }

    /**
     * Creates an activity to show licenses of the app.
     */
    private void setListenerForLicensesButton() {
        licenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentLicenses = new Intent(MainActivity.getContext(), LicensesActivity.class);
                startActivity(intentLicenses);
            }
        });
    }


    /**
     * Checks if we have permission to access location, and then if not, requests it.
     */
    private void getPermissionToAccessFineLocation() {
        if (!PermissionManager.permissionToFineLocation(context)) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST);
        }

        if (ContextCompat.checkSelfPermission(MainActivity.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST);
        }
    }

    /**
     * Checks if we have permission to read calendar, and then if not, requests it.
     */
    private void getPermissionToReadCalendar() {
        if (!PermissionManager.permissionToReadCalendar(context)) {
            requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},
                    READ_CALENDAR_PERMISSIONS_REQUEST);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_PERMISSIONS_REQUEST:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, R.string.settings_toast_gps_granted, Toast.LENGTH_SHORT).show();

                    gpsCheckBox.setChecked(true);
                    Intent intent = new Intent(context, PlaceRecorder.class);
                    intent.putExtra("Receiver", resultReceiver);
                    context.startService(intent);
                    Toast.makeText(context, R.string.settings_toast_gps_tracking_on, Toast.LENGTH_SHORT).show();
                } else {
                    gpsCheckBox.setChecked(false);
                    Toast.makeText(context, R.string.settings_toast_gps_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            case READ_CALENDAR_PERMISSIONS_REQUEST:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, R.string.settings_toast_calendar_granted, Toast.LENGTH_SHORT).show();
                } else {
                    calendarCheckBox.setChecked(false);
                }
                break;
        }
    }

    /**
     * Checks if PlaceRecorder is running.
     * @see PlaceRecorder
     * @return true/false
     */
    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) MainActivity.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PlaceRecorder.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes all data from the database.
     */
    private void deleteAllDataFromDatabase() {
        GpsPointDao.deleteAllData();
        PlaceDao.deleteAllData();
        RouteSearchDao.deleteAllData();
        Coordinate.deleteAll(Coordinate.class);
        VisitDao.deleteAllData();

        if (GpsPoint.count(GpsPoint.class) == 0
                && Place.count(Place.class)  == 0
                && RouteSearch.count(RouteSearch.class) == 0
                && Coordinate.count(Coordinate.class) == 0
                && Visit.count(Visit.class) == 0) {
            Toast.makeText(context, R.string.settings_toast_delete_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.settings_toast_delete_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateView() {
        FragmentTransaction tr = getFragmentManager().beginTransaction();
        Fragment yourPlaces = getFragmentManager().getFragments().get(1);
        tr.detach(yourPlaces);
        tr.attach(yourPlaces);
        tr.commit();
    }

}