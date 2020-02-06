package gov.nysenate.inventory.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.RequestFuture;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.nysenate.inventory.android.AppSingleton;
import gov.nysenate.inventory.android.ChangePasswordDialog;
import gov.nysenate.inventory.android.CommentsDialog;
import gov.nysenate.inventory.android.InvApplication;
import gov.nysenate.inventory.android.JsonInvObjectRequest;
import gov.nysenate.inventory.android.KeywordDialog;
import gov.nysenate.inventory.android.MsgAlert;
import gov.nysenate.inventory.android.NewInvDialog;
import gov.nysenate.inventory.android.R;
import gov.nysenate.inventory.android.RequestTask;
import gov.nysenate.inventory.android.SoundAlert;
import gov.nysenate.inventory.listener.ChangePasswordDialogListener;
import gov.nysenate.inventory.listener.CommodityDialogListener;
import gov.nysenate.inventory.listener.OnKeywordChangeListener;
import gov.nysenate.inventory.model.Commodity;
import gov.nysenate.inventory.model.Employee;
import gov.nysenate.inventory.model.LoginStatus;
import gov.nysenate.inventory.util.HttpUtils;
import gov.nysenate.inventory.util.Toasty;

public abstract class SenateActivity extends Activity implements
        CommodityDialogListener, OnKeywordChangeListener, ChangePasswordDialogListener {
    public String timeoutFrom = "N/A";

    public static final String FINISH_ALL_ACTIVITIES_ACTIVITY_ACTION = "gov.nysenate.inventory.android.FINISH_ALL_ACTIVITIES_ACTIVITY_ACTION";
    public final int OK = 100, SERVER_SESSION_TIMED_OUT = 1000,
            NO_SERVER_RESPONSE = 1001, EXCEPTION_IN_CODE = 1002;
    public final int CHECK_SERVER_RESPONSE = 200;
    public static final int COMMODITYLIST = 3030, NEWITEMCOMMENTS = 3031,
            ITEMCOMMENTS = 3032;

    private BaseActivityReceiver baseActivityReceiver = new BaseActivityReceiver();
    // private CheckInternet receiver;
    public static final IntentFilter INTENT_FILTER = createIntentFilter();
    public static long maxWifiWaitTime = 20 * 1000; // 20 Seconds

    public int dialogSelectedRow = -1;
    public String dialogKeywords = null;
    public String dialogComments = null;
    public String dialogTitle = null;
    public String dialogMsg = null;
    public boolean senateTagNum = false;

    public NewInvDialog newInvDialog = null;
    public CommentsDialog commentsDialog = null;
    public static Context stContext;
    public ChangePasswordDialog changePasswordDialog = null;
    android.app.FragmentManager fragmentManager = this.getFragmentManager();
    String defrmint = null;
    public static boolean changePasswordOnLogin = false;
    public LoginActivity currentLoginActivity = null;
    protected final int DBASTRING = 5000, DBANAME = 5001, DBASERVER = 5002, DBAPORT = 5003;
    static boolean enablingWifi = false;
    static boolean prevConnected = true; // Assume that connection was already found so
    // that it doesn't show Wifi Connection Found
    // for every Activity
    static boolean curConnected = true; // Assume that connection was already found so
    // that it doesn't show Wifi Connection Found
    // for every Activity
    // The Check Internet Service will check for
    // connections and disconnections.

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                Toast toast = Toast.makeText(getApplicationContext(), "Going Back",
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                NavUtils.navigateUpFromSameTask(this);

                overridePendingTransition(R.anim.in_left, R.anim.out_right);
                return true;
            case R.id.changeWifiCountsMenu:
                showWifiCountsDialog();
                return true;
            case R.id.resetWifiCountsMenu:
                resetWifiCounts();
                return true;
            case R.id.aboutApp:
                showAboutDialog();
                return true;
            case R.id.changePasswordMenu:
                if (LoginActivity.user_name.getText().toString().trim().length() == 0) {
                    new Toasty(this, "!!ERROR: Username must first be entered when changing password.", Toast.LENGTH_SHORT).showMessage();
                } else {
                    changePasswordOnLogin = false;
                    showChangePasswordDialog();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
//        if (checkServerResponse(true) == OK) {
            super.onBackPressed();
            overridePendingTransition(R.anim.in_left, R.anim.out_right);
 //   }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterBaseActivityReceiver();
        InvApplication.activityDestroyed();
        // unregisterReceiver(receiver);
        InvApplication.activityPaused();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_senate, menu);
        return true;
    }

    // Quick shortcut for playing a single sound.
    public void playSound(int sound) {
        int[] sounds = new int[1];
        sounds[0] = sound;
        playSounds(sounds);
    }

    public void playSounds(int[] sounds) {
        // void for now.. Might return error msgs, or completion result in
        // future.
        Object[] soundParams = new Object[sounds.length + 1];
        soundParams[0] = getApplicationContext();
        for (int x = 0; x < sounds.length; x++) {
            soundParams[x + 1] = new Integer(sounds[x]);
        }

        AsyncTask<Object, Integer, String> soundResults = new SoundAlert()
                .execute(soundParams);

    }

    private static IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(FINISH_ALL_ACTIVITIES_ACTIVITY_ACTION);
        return filter;
    }

    protected void registerBaseActivityReceiver() {
        registerReceiver(baseActivityReceiver, INTENT_FILTER);
    }

    protected void unRegisterBaseActivityReceiver() {
        unregisterReceiver(baseActivityReceiver);
    }

    public class BaseActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()
                    .equals(FINISH_ALL_ACTIVITIES_ACTIVITY_ACTION)) {
                finish();
            }
        }
    }

    protected void closeAllActivities() {
        sendBroadcast(new Intent(FINISH_ALL_ACTIVITIES_ACTIVITY_ACTION));
        InvApplication.activityDestroyed();
    }

    public void startTimeout(int timeoutType) {
        Intent intentTimeout = new Intent(this, LoginActivity.class);
        intentTimeout.putExtra("TIMEOUTFROM", timeoutFrom);
        startActivityForResult(intentTimeout, timeoutType);
    }

    @Override
    public void commoditySelected(int rowSelected, Commodity commoditySelected, String comments) {
        // TODO Auto-generated method stub

    }

    public void getDialogDataFromServer() {

    }

    public void showAboutDialog() {
        final Activity currentActivity = this;
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        // getInstance the app version Name for display
        String version = pInfo.versionName;
        // getInstance the app version Code for checking
        int versionCode = pInfo.versionCode;
        Resources resources;
        resources = this.getResources();
        AssetManager assetManager = resources.getAssets();
        String URL = "<N/A>";
        String server = "<N/A>";
        String port = "<N/A>";

        String dba = parseServerDatabaseString(DBANAME);

        boolean httpServer = false;

        try {
            InputStream inputStream = assetManager.open("invApp.properties");
            Properties properties = new Properties();
            properties.load(inputStream); // we load the properties here and we
            // use same object elsewhere in
            // project
            URL = properties.get("WEBAPP_BASE_URL").toString().trim();
            if (!URL.endsWith("/")) {
                URL += "/";
            }
            this.defrmint = properties.get("DEFRMINT").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (URL == null || URL.length() == 0) {

        } else {
            if (URL.toUpperCase().startsWith("HTTP://")) {
                String IPADDRESS_PATTERN =
                        "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

                Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
                URL = URL.replaceAll("(?i)HTTP://", "");
                Matcher matcher = pattern.matcher(URL);
                boolean ipAddressFound = matcher.find();
                if (ipAddressFound) {
                    server = matcher.group();
                    URL = URL.replaceAll(server, "");
                    int colon = URL.indexOf(":");
                    port = URL.substring(colon + 1);
                    port = port.replaceAll("\\D+", "");
                    httpServer = true;
                } else {
                    int period = URL.indexOf(".");
                    int colon = URL.indexOf(":");
                    if (period > -1) {
                        server = URL.substring(0, period);
                    }
                    if (colon > -1) {
                        port = URL.substring(colon + 1);
                    } else {
                        port = "<N/A>";
                    }
                    port = port.replaceAll("\\D+", "");
                    httpServer = true;
                }
            } else if (URL.toUpperCase().startsWith("HTTPS://")) {
                String IPADDRESS_PATTERN =
                        "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

                Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
                URL = URL.replaceAll("(?i)HTTPS://", "");
                Matcher matcher = pattern.matcher(URL);
                boolean ipAddressFound = matcher.find();
                if (ipAddressFound) {
                    server = matcher.group();
                    URL = URL.replaceAll(server, "");
                    int colon = URL.indexOf(":");
                    port = URL.substring(colon + 1);
                    port = port.replaceAll("\\D+", "");
                    httpServer = true;
                } else {
                    int period = URL.indexOf(".");
                    int colon = URL.indexOf(":");
                    if (period > -1) {
                        server = URL.substring(0, period);
                    }
                    if (colon > -1) {
                        port = URL.substring(colon + 1);
                    } else {
                        port = "<N/A>";
                    }
                    port = port.replaceAll("\\D+", "");
                    server = server.replaceAll("(?i)HTTPS://", "");
                    httpServer = true;
                }
            } else {
                server = URL;
                httpServer = false;
            }
        }

        String message;

        if (httpServer) {
            message = "<b>Version:</b> " + version + " ("
                    + versionCode + ") <br/><br/><b>Server:</b> " + server + " <br/><br/><b>Port:</b> " + port + "<br/>";
            if (dba != null && dba.trim().length() > 0) {
                message = message + "<br/><b>Database:</b> " + dba + "<br/>";
            }
        } else {
            message = "<b>Version:</b> " + version + " ("
                    + versionCode + ") <br/><br/><b>Server:</b> " + URL + "<br/>";
            if (dba != null && dba.trim().length() > 0) {
                message = message + "<br/><b>Database:</b> " + dba + "<br/>";
            }
        }

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog
        // characteristics
        builder.setTitle(Html
                .fromHtml("<font color='#000055'>About Inventory Mobile App</font>"));
        builder.setMessage(Html.fromHtml(message));
        // Add the buttons
        builder.setPositiveButton(Html.fromHtml("<b>OK</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);

    }

    public void showWifiCountsDialog() {
        StringBuffer message = new StringBuffer();
        if (LoginActivity.wifiCountStart != null) {
            message.append("<b>Wifi Count started at:</b> ");
            message.append(new SimpleDateFormat("MM/dd/yy hh:mm:ssa", Locale.US).format(LoginActivity.wifiCountStart));
            message.append("<br/><br/>");
        }
        message.append("<b>Check Internet Wifi Found Count:</b> ");
        message.append(LoginActivity.chkintWifiFoundCount);
        message.append("<br/><br/><b>Check Internet Wifi Lost Count:</b> ");
        message.append(LoginActivity.chkintWifiLostCount);
        message.append("<br/><br/><b>Senate Activity Wifi Found Count:</b> ");
        message.append(LoginActivity.senateWifiFoundCount);
        message.append("<br/><br/><b>Senate Activity Wifi Lost Count:</b> ");
        message.append(LoginActivity.senateWifiLostCount);

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog
        // characteristics
        builder.setTitle(Html
                .fromHtml("<font color='#000055'>Wifi Found/Lost Counts</font>"));
        builder.setMessage(Html.fromHtml(message.toString()));
        // Add the buttons
        builder.setPositiveButton(Html.fromHtml("<b>OK</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
        TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);

    }

    public void resetWifiCounts() {
        LoginActivity.senateWifiFoundCount = 0;
        LoginActivity.senateWifiLostCount = 0;
        LoginActivity.chkintWifiFoundCount = 0;
        LoginActivity.chkintWifiLostCount = 0;
        LoginActivity.wifiCountStart = new Date();
        new Toasty(stContext).showMessage("Wifi counts were reset!!!");
    }

    public ChangePasswordDialog showChangePasswordDialog() {
        return showChangePasswordDialog(null, null, null, null);
    }

    public ChangePasswordDialog showChangePasswordDialog(int initialFocus) {
        return showChangePasswordDialog(null, null, null, null, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(boolean oldPasswordRequired, int initialFocus) {
        return showChangePasswordDialog(null, null, null, null, oldPasswordRequired, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(boolean oldPasswordRequired) {
        return showChangePasswordDialog(null, null, null, null, oldPasswordRequired);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, int initialFocus) {
        return showChangePasswordDialog(loginStatus, null, null, null, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus) {
        return showChangePasswordDialog(loginStatus, null, null, null, ChangePasswordDialog.DEFAULTFOCUS);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, boolean oldPasswordRequired, int initialFocus) {
        return showChangePasswordDialog(loginStatus, null, null, null, oldPasswordRequired, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, boolean oldPasswordRequired) {
        return showChangePasswordDialog(loginStatus, null, null, null, oldPasswordRequired, ChangePasswordDialog.DEFAULTFOCUS);
    }

    public ChangePasswordDialog showChangePasswordDialog(String oldPassword, String newPassword, String confirmPassword, int initialFocus) {
        return showChangePasswordDialog(null, oldPassword, newPassword, confirmPassword, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(String oldPassword, String newPassword, String confirmPassword) {
        return showChangePasswordDialog(null, oldPassword, newPassword, confirmPassword, ChangePasswordDialog.DEFAULTFOCUS);
    }

    public ChangePasswordDialog showChangePasswordDialog(String oldPassword, String newPassword, String confirmPassword, boolean oldPasswordRequired, int initialFocus) {
        return showChangePasswordDialog(null, oldPassword, newPassword, confirmPassword, oldPasswordRequired, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(String oldPassword, String newPassword, String confirmPassword, boolean oldPasswordRequired) {
        return showChangePasswordDialog(null, oldPassword, newPassword, confirmPassword, oldPasswordRequired, ChangePasswordDialog.DEFAULTFOCUS);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, String oldPassword, String newPassword, String confirmPassword) {
        return showChangePasswordDialog(loginStatus, oldPassword, newPassword, confirmPassword, true, ChangePasswordDialog.DEFAULTFOCUS);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, String oldPassword, String newPassword, String confirmPassword, int initialFocus) {
        return showChangePasswordDialog(loginStatus, oldPassword, newPassword, confirmPassword, true, initialFocus);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, String oldPassword, String newPassword, String confirmPassword, boolean oldPasswordRequired) {
        return showChangePasswordDialog(loginStatus, oldPassword, newPassword, confirmPassword, oldPasswordRequired, ChangePasswordDialog.DEFAULTFOCUS);
    }

    public ChangePasswordDialog showChangePasswordDialog(LoginStatus loginStatus, String oldPassword, String newPassword, String confirmPassword, boolean oldPasswordRequired, int initialFocus) {

        playSound(R.raw.warning);
        String title = "Change Password";
        String message = "";

        if (changePasswordOnLogin) {
            title = "**WARNING: Your password has expired. Please enter a New Password";
        }

        changePasswordDialog = new ChangePasswordDialog(this, title, message, oldPasswordRequired, oldPassword, newPassword, confirmPassword, initialFocus);
        changePasswordDialog.addListener(this);
        changePasswordDialog.setRetainInstance(true);
        changePasswordDialog.show(fragmentManager, "change_password_dialog");

        return changePasswordDialog;

    }

    public void reOpenNewInvDialog() {
        if (newInvDialog != null) {
            newInvDialog.dismiss();
        }
        android.app.FragmentManager fm = this.getFragmentManager();
        newInvDialog = new NewInvDialog(this, dialogTitle, dialogMsg);
        newInvDialog.addListener(this);
        newInvDialog.setRetainInstance(true);
        newInvDialog.show(fm, "fragment_name");
        // newInvDialog.getDialog().setCanceledOnTouchOutside(false);
    }

    public void onVolleyInvError () {
        new Toasty(this).showMessage("On Volley Error GENERIC SENATE Activity");
    }

    public void afterVolleyInvError () {

    }

    public void reOpenCommentsDialog() {
        if (newInvDialog != null) {
            newInvDialog.dismiss();
        }
        android.app.FragmentManager fm = this.getFragmentManager();
        commentsDialog = new CommentsDialog(this, dialogTitle, dialogMsg);
        commentsDialog.setRetainInstance(true);
        commentsDialog.show(fm, "fragment_name");
        // commentsDialog.getDialog().setCanceledOnTouchOutside(false);
    }

    public void startKeywordSpeech(View view) {
        if (view.getId() == R.id.btnKeywordSpeech) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    "Commodity Keyword Search");
            startActivityForResult(intent, COMMODITYLIST);
        }
    }

    public void startNewItemSpeech(View view) {
        if (view.getId() == R.id.btnNewItemCommentSpeech) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "New Item Comments");
            startActivityForResult(intent, NEWITEMCOMMENTS);
        }
    }

    public void startItemCommentSpeech(View view) {
        if (view.getId() == R.id.btnItemCommentSpeech) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Item Comments");
            startActivityForResult(intent, ITEMCOMMENTS);
        }
    }

    public String parseServerDatabaseString(int returnValue) {
        String serverResponse = null;
        AsyncTask<String, String, String> requestServerResponse = null;
        // check network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data

            try {
                // Get the URL from the properties
                String URL = LoginActivity.properties.get("WEBAPP_BASE_URL")
                        .toString();
                if (!URL.endsWith("/")) {
                    URL += "/";
                }
                requestServerResponse = new RequestTask().execute(URL
                        + "GetDatabaseName");

                try {
                    serverResponse = null;
                    String resp = requestServerResponse.get();
                    serverResponse = requestServerResponse.get().trim()
                            .toString();
                    if (serverResponse == null) {
                        //noServerResponseMsg();
                        return "";
                    } else if (serverResponse.indexOf("Session timed out") > -1) {
                        // TODO Handle Session Timeouts, for now, simply return nothing.
                        return "";
                    } else if (serverResponse.trim().length() == 0) {
                        //noServerResponseMsg();
                        return "";
                    } else {
                        try {
                            String[] splitDBAString = serverResponse.split(":");
                            if (splitDBAString.length < 1) {
                                switch (returnValue) {
                                    case DBASTRING:
                                        return serverResponse;
                                    default:
                                        return "";
                                }
                            } else {
                                // dbaString = jdbc:{driver}:{driver type}:@{dba server}:{dba port}:{dba name}

                                switch (returnValue) {
                                    case DBASTRING:
                                        return serverResponse;
                                    case DBANAME:
                                        if (splitDBAString.length > 5) {
                                            return splitDBAString[5];
                                        } else {
                                            return "";
                                        }
                                    case DBASERVER:
                                        if (splitDBAString.length > 3) {
                                            return splitDBAString[3];
                                        } else {
                                            return "";
                                        }
                                    case DBAPORT:
                                        if (splitDBAString.length > 4) {
                                            return splitDBAString[4];
                                        } else {
                                            return "";
                                        }
                                }
                            }

                        } catch (NullPointerException e1) {
                            System.out.println("NULLPOINTER ERROR:" + e1.getMessage());
                            e1.printStackTrace();
                            return "";
                        } catch (Exception e1) {
                            System.out.println("ERROR:" + e1.getMessage());
                            e1.printStackTrace();
                            return "";
                        }

                    }

                } catch (NullPointerException e) {
                    noServerResponse("Senate Activity:1:"+e.getMessage());
                    return "";
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "";
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return "";
            }
        } else {
            // TODO Handle Errors, for now, simply return nothing.
            return "";
        }
        return "";

    }

    protected void onResume(Bundle savedInstanceState) {
        super.onResume();
        InvApplication.activityResumed();
        checkInternetConnection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        InvApplication.activityResumed();
        checkInternetConnection();
        timer.cancel();
        if (!((Object) this).getClass().getSimpleName().equalsIgnoreCase("LoginActivity"))
            timer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        InvApplication.activityPaused();

        timer.cancel();
    }

/*    public int checkServerResponse() {
        return checkServerResponse(true);
    }

    public int checkServerResponse(boolean handleServerResponse) {
        Log.i("internet check", "checkServerResponse");
        String serverResponse = null;
        AsyncTask<String, String, String> requestServerResponse = null;
        // check network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        Log.i("internet check", "checkServerResponse 5");

        HttpUtils httpUtils = new HttpUtils();

        Log.i("internet check", "checkServerResponse 8");
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            Log.i("internet check", "checkServerResponse CONNECTED");

            try {

                if (LoginActivity.properties==null) {
                    LoginActivity.loadProperties(this.getAssets());
                }

                // Get the URL from the properties
                String URL = LoginActivity.properties.get("WEBAPP_BASE_URL")
                        .toString();
                if (!URL.endsWith("/")) {
                    URL += "/";
                }
                Log.i("internet check", "checkServerResponse RequestTask");

                RequestFuture<String> future = RequestFuture.newFuture();

                String string = new String();

                StringInvRequest req = new StringInvRequest(Request.Method.GET, URL , null, future, future);

                try {

                    Log.i(this.getClass().getName(), "!!Before Response:" + URL );
                    AppSingleton.getInstance(InvApplication.getAppContext()).addToRequestQueue(req);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }


// add it to the RequestQueue



                Log.i("internet check", "checkServerResponse :"+URL
                );

                try {
                    serverResponse = null;
                    Log.i("internet check", "checkServerResponse serverResponse before getInstance");
                    serverResponse = future.get();

                    Log.i("internet check", "checkServerResponse serverResponse:"+serverResponse);

                    if (serverResponse == null) {
                        if (handleServerResponse) {
                            Log.i("internet check", "checkServerResponse serverResponse handle server response");
                            noServerResponse();
                        }
                        try {
                            Log.i("internet check", "checkServerResponse serverResponse don't handle server response");
                            new Toasty(SenateActivity.stContext).showMessage("!!ERROR: App is Offline.");
                        }
                        catch (Exception e) {
                            Log.e(this.getClass().getName(), "!!ERROR: Could not show toasty message that App is Offline!!");
                        }

                        Log.i("internet check", "checkServerResponse before noconnect sound");
                        httpUtils.playSound(R.raw.noconnect);
                        Log.i("internet check", "checkServerResponse after noconnect sound");

                        return NO_SERVER_RESPONSE;
                    } else if (serverResponse.toString().indexOf("Session timed out") > -1) {  // it is wrong to do a toString... only done for testing..
                        if (handleServerResponse) {
                            startTimeout(this.CHECK_SERVER_RESPONSE);
                        }
                        return SERVER_SESSION_TIMED_OUT;
                    }

                } catch (NullPointerException e) {
                    if (handleServerResponse) {
                        Log.i("internet check", "checkServerResponse noServerResponse with NULL Pointer Exception");
                        noServerResponse();
                        Log.i("internet check", "checkServerResponse noServerResponse with NULL Pointer Exception DONE");
                    }

                    Log.i("internet check", "checkServerResponse noconnect play sound");
                    httpUtils.playSound(R.raw.noconnect);
                    Log.i("internet check", "checkServerResponse noconnect after play sound");
                    return EXCEPTION_IN_CODE;
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            Log.i("internet check", "checkServerResponse ELSE");
            try {
                new Toasty(SenateActivity.stContext).showMessage("!!ERROR: App is Offline.");
            }
            catch (Exception e) {
                Log.e(this.getClass().getName(), "!!ERROR: Could not show toasty message that App is Offline!!");
            }
            Log.i("internet check", "checkServerResponse ELSE 2");

            httpUtils.playSound(R.raw.noconnect);
            Log.i("internet check", "checkServerResponse ELSE 3");
            return NO_SERVER_RESPONSE;
        }
        Log.i("internet check", "checkServerResponse OK (DONE)");
        return OK;
    }*/

    WifiManager mainWifi;
    // WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    ScanResult currentWifiResult;
    int currentSignalStrength = 0;
    int prevSignalStrength = 0;
    Context context;

    /*
     * The app needs an internet connection, so check it on Activity Startup
     * (fires when Any Activity is opened) If the internet connection is turned
     * off, then attempt to turn it on.
     */

    public void checkInternetConnection() {
        /*
         * new Thread() { public void run() {
         */
        int duration = Toast.LENGTH_SHORT;
        Toast toast;
        if (context == null) {
            context = getApplicationContext();
        }
        duration = Toast.LENGTH_SHORT;
        /*
         * toast = Toast.makeText(context,
         * "Inventory App checking Internet Connection. Please be patient..",
         * duration); toast.setGravity(Gravity.CENTER, 0, 0); toast.show();
         */

        SenateActivity.this.context = context;
        ConnectivityManager cm = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE));

        try {
            if (cm == null) {
                return;
            }

            prevConnected = curConnected;
            /*
             * If the internet connection is on, then check to see if it
             * previously wasn't, if it was off then show the Wifi Connection
             * Found toast.
             *
             * If there is no internet connection, but Wifi on the Tablet is
             * turned on then return the toast that the Wifi Connection is lost.
             *
             * If there is no internet connection and the Wifi is off on the
             * tablet, then attempt to turn on the Wifi Connection.
             */
            if (cm.getActiveNetworkInfo() != null
                    && cm.getActiveNetworkInfo().isConnected()) {
                curConnected = cm.getActiveNetworkInfo().isConnected();
                if (!prevConnected) {
                    Log.i("SenateActivity", "****************WFI CONNECTION FOUND");
                    LoginActivity.senateWifiFoundCount++;

                    toast = Toast.makeText(context, "Wifi Connection found.",
                            duration);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            } else {

                mainWifi = (WifiManager) context
                        .getSystemService(Context.WIFI_SERVICE);
                curConnected = false;
                if (mainWifi.isWifiEnabled()) {
                    if (prevConnected) {
                        Log.i("SenateActivity", "****************WFI CONNECTION LOST");
                        LoginActivity.senateWifiLostCount++;
                        toast = Toast.makeText(context,
                                "Wifi Connection lost.", duration);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        prevConnected = false;
                    }
                } else {
                    // check if connected! (will not work in a service)
                    // wifiAlert("WIFI IS CURRENTLY TURNED OFF",
                    // "**WARNING: Wifi is currently turned <font color='RED'>OFF</font>. This app cannot work without the Wifi connection. Do you want to turn it on?");

                    new Toasty(this.getApplicationContext()).showMessage("turnWifiOn is commented out in Senate Activity for testing");

                    // so using turnwifi on directly
                    //turnWifiOn();
                }
            }
            ;

        } catch (Exception e0) {
            toast = Toast.makeText(context,
                    "(EXCEPTION0) Check Internet:" + e0.getMessage(), duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();

        }
        // 1. Instantiate an AlertDialog.Builder with its constructor
        /*
         * AlertDialog.Builder builder = new AlertDialog.Builder(context);
         *
         * // 2. Chain together various setter methods to set the dialog
         * characteristics builder.setMessage(
         * "Internet Connection is Lost. Please fix before continuing.")
         * .setTitle("Internet Connection Lost");
         *
         * // 3. Get the AlertDialog from create() AlertDialog dialog =
         * builder.create(); dialog.show();
         */
        /*
         * } }.start();
         */

    }

    /*
     * Wifi Alert does not work with the Check Internet connection Service..
     * Original plans were to ask the user before connecting, but those plans
     * have changed.. Now while the app is opened and in the foreground, we will
     * try keep the wifi on (the app is useless without an internet connection).
     * We will not try keeping the internet connection on if the App is closed
     * or in the background. Currently, WifiAlert is not being used for this
     * reason.
     */

    public void wifiAlert(String title, String msg) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                context);

        // set title
        alertDialogBuilder.setTitle(Html.fromHtml("<font color='#000055'>"
                + title + "</font>"));

        // set dialog message
        alertDialogBuilder.setMessage(Html.fromHtml(msg)).setCancelable(false)
                .setPositiveButton(Html.fromHtml("<b>Yes</b>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing

                        turnWifiOn();

                        dialog.dismiss();
                    }

                })
                .setNegativeButton(Html.fromHtml("<b>No</b>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing

                        CharSequence text = "Wifi will remain off.";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                        dialog.dismiss();
                    }

                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    /*
     * Turn Wifi Connection back on if it is not Enabled
     */

    public void turnWifiOn() {
        turnWifiOn(maxWifiWaitTime);
    }

    public void turnWifiOn(long maxWifiWaitTime) {
        int duration = Toast.LENGTH_SHORT;

        Toast toast = null;
        enablingWifi = false;
        long startTime = System.currentTimeMillis();

        try {
            // Wait until Wifi is Enabled or the Maximum Wifi Wait Time has
            // occurred
            while (!mainWifi.isWifiEnabled()
                    && (System.currentTimeMillis() - startTime <= maxWifiWaitTime)) {
                // While waiting, attempt to turn on the Wifi Connection if it
                // already hasn't been attempted
                if (!enablingWifi) {
                    mainWifi.setWifiEnabled(true);
                    enablingWifi = true;
                }
                // Wait to connect
                Thread.sleep(1000);
            }
            // Check to see if the Wifi Connection turned on, give appropriate
            // message depending on if it was or not.
            if (mainWifi.isWifiEnabled()) {
                toast = Toast.makeText(context,
                        "(Inventory App) Wifi has been turned back on.",
                        duration);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                new MsgAlert(
                        context,
                        "WIFI could not be turned back on",
                        "**WARNING: (Inventory App) Unable to turn Wifi on. Please turn it on manually.");
            }

        } catch (Exception e) {
            toast = Toast.makeText(
                    context,
                    "(Inventory App) (EXCEPTION1) Check Internet:"
                            + e.getMessage(), duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    @Override
    public void OnKeywordChange(KeywordDialog keywordDialog,
                                ListView lvKeywords, String keywords) {
        NewInvDialog.tvKeywordsToBlock.setText(keywords);
        getDialogDataFromServer();

    }

    public static CountDownTimer timer = new CountDownTimer(15 * 60 * 1000, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            // TODO Auto-generated method stub
            //System.out.println(millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {
            // TODO Auto-generated method stub
            inactivityTimeout();
        }

    };

    public static void inactivityTimeout() {
        Intent myIntent = new Intent(stContext, LoginActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.putExtra("TIMEOUTFROM", "softkey");
        stContext.startActivity(myIntent);
    }

    @Override
    public void onUserInteraction() {
        timer.cancel();
        if (!((Object) this).getClass().getSimpleName().equalsIgnoreCase("LoginActivity"))
            timer.start();
        super.onUserInteraction();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        InvApplication.timeoutType = -1;
        InvApplication.currentSenateActivity = this;

        if (stContext == null)
            stContext = getApplicationContext();
        if (!((Object) this).getClass().getSimpleName().equalsIgnoreCase("LoginActivity"))
            timer.start();
        setCurrentActivity(((Object) this).getClass().getSimpleName());
    }

    public LoginStatus verifyLogin(String user_name, String password) {
        LoginStatus loginStatus = new LoginStatus();
        try {
            // check network connection
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            JSONObject res = null;

            if (networkInfo != null && networkInfo.isConnected()) {
                // fetch data
                try {
                    // Get the URL from the properties
                    // if (loginStatusParam==null)
                    String URL = LoginActivity.properties
                            .get("WEBAPP_BASE_URL").toString();
                    if (!URL.endsWith("/")) {
                        URL += "/";
                    }

                    RequestFuture<JSONObject> future = RequestFuture.newFuture();
                    JsonInvObjectRequest req = new JsonInvObjectRequest(URL  + "Login?user=" + user_name + "&pwd="
                            + password + "&defrmint=" + defrmint, new JSONObject(),future, future);
                    /* Add your Requests to the RequestQueue to execute */
                    AppSingleton.getInstance(InvApplication.getAppContext()).addToRequestQueue(req);


                    try {
                        res = future.get();

                        loginStatus.parseJSON(res);

                        if (res == null) {

                            noServerResponse("Senate Activity:2:null");
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        // TODO Auto-generated catch block
                        noServerResponse("Senate Activity:3:"+e.getMessage());
                    }
                } catch (Exception e) {

                }
                LoginActivity.nauser = user_name;
            }
        } catch (Exception e) {
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this,
                    "Problem connecting to Mobile App Server. Please contact STSBAC.("
                            + e.getMessage() + ")", duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        return loginStatus;
    }

    private static String activity;

    public void setCurrentActivity(String activity) {
        SenateActivity.activity = activity;
    }

    public static String getCurrentActivity() {
        return activity;
    }

    public int findEmployee(String employeeName, List<Employee> empList) {
        for (int x = 0; x < empList.size(); x++) {
            if (employeeName.equals(empList.get(x).getFullName())) {
                return x;
            }
        }
        return -1;
    }

    protected boolean selectedEmployeeValid(String name, List<Employee> empList) {
        int index = -1;
        if (name.length() > 0) {
            index = findEmployee(name, empList);
        }
        return index > -1 ? true : false;
    }

    protected void displayInvalidEmployeeMessage(String name) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        if (name.length() > 0) {
            Toast toast = Toast.makeText(context,
                    "!!ERROR: No xref# found for employee", duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            Toast toast = Toast
                    .makeText(
                            context,
                            "!!ERROR: You must first pick an employee name for the signature.",
                            Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    protected void displayNoSignatureMessage() {
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context,
                "!!ERROR: Employee must also sign within the Red box.",
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onChangePasswordOKButtonClicked(boolean oldPasswordRequired, String oldPassword, String newPassword,
                                                String confirmPassword) {
        String username = LoginActivity.user_name.getText().toString();
        final String oldPasswordF = oldPassword;
        final String newPasswordF = newPassword;
        final String confirmPasswordF = confirmPassword;
        final boolean oldPasswordRequiredF = oldPasswordRequired;

        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == dialog.BUTTON_POSITIVE) {
                    showChangePasswordDialog(oldPasswordF, newPasswordF, confirmPasswordF, oldPasswordRequiredF);
                }

            }

        };

        if (username == null || username.trim().length() == 0) {
            new Toasty(this, "!!ERROR: Username must first be entered.", Toast.LENGTH_SHORT).showMessage();
            return;
        } else if (oldPasswordRequired && (oldPassword == null || oldPassword.trim().length() == 0)) {
            new MsgAlert(this).showMessage("!!ERROR: Old Password must be entered", "Old Password must be entered.", onClickListener);

            return;
        }

        if (oldPasswordRequired) {
            LoginStatus loginStatus = new LoginStatus();
            loginStatus = verifyLogin(username, oldPassword);
            if (!loginStatus.isUsernamePasswordValid()) {
                if (loginStatus.getNustatus() == loginStatus.INVALID_USERNAME_OR_PASSWORD) {
                    onClickListener = new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == dialog.BUTTON_POSITIVE) {
                                showChangePasswordDialog("", newPasswordF, confirmPasswordF, oldPasswordRequiredF, ChangePasswordDialog.OLDPASSWORDFOCUS);
                            }

                        }

                    };
                    new MsgAlert(this).showMessage("!!ERROR: Invalid Old Password", "Invalid Old Password.", onClickListener);
                } else {
                    new MsgAlert(this).showMessage("!!ERROR: " + loginStatus.getDestatus(), loginStatus.getDestatus(), onClickListener);
                }
                return;
            }
        }

        if (newPassword.isEmpty()) {

            new MsgAlert(this).showMessage("!!ERROR: New Password must be entered", "New Password must be entered.", onClickListener);
        } else if (newPassword.length() < 8) {
            onClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        showChangePasswordDialog(oldPasswordF, newPasswordF, confirmPasswordF, oldPasswordRequiredF, ChangePasswordDialog.NEWPASSWORDFOCUS);
                    }

                }
            };
            new MsgAlert(this).showMessage("!!ERROR: New Password too short", "New password must be at least 8 characters in length.", onClickListener);
        } else if ((oldPasswordRequired && (newPassword.equalsIgnoreCase(oldPassword))) ||
                (!oldPasswordRequired && (newPassword.trim().equalsIgnoreCase(LoginActivity.password.getText().toString().trim())))) {
            onClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        showChangePasswordDialog(oldPasswordF, null, null, oldPasswordRequiredF, ChangePasswordDialog.NEWPASSWORDFOCUS);
                    }
                }

            };
            new MsgAlert(this).showMessage("!!ERROR: Password cannot be reused", "New Password cannot be the same as the Old Password.", onClickListener);

        } else if (confirmPassword.isEmpty()) {
            new MsgAlert(this).showMessage("!!ERROR: Confirm Password must be entered", "Confirm Password must be entered.", onClickListener);
        } else if (confirmPassword.equals(newPassword)) {
            changePassword(LoginActivity.user_name.getText().toString(), newPassword, oldPasswordRequired, oldPassword);
        } else {
            onClickListener = new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        showChangePasswordDialog(oldPasswordF, newPasswordF, confirmPasswordF, oldPasswordRequiredF, ChangePasswordDialog.CONFIRMPASSWORDFOCUS);
                    }

                }

            };
            new MsgAlert(this).showMessage("!!ERROR: Passwords do not match", "New Password and Confirm Password do not match.", onClickListener);
        }
    }

    @Override
    public void onChangePasswordCancelButtonClicked() {
    }

    private void changePassword(String userName, String newPassword) {
        changePassword(userName, newPassword, false, null);
    }

    private void changePassword(String userName, String newPassword, boolean oldPasswordRequired, String oldPassword) {
        Hashtable<String, String> postParams = new Hashtable<>();
        postParams.put("user", userName);
        postParams.put("newPassword", newPassword);
        AsyncTask<String, String, String> resr1;
        resr1 = new RequestTask(postParams).execute("ChangePassword");
        String res = null;
        try {
            res = resr1.get().trim().toString();
            final String oldPasswordF = oldPassword;
            final boolean oldPasswordRequiredF = oldPasswordRequired;
            MsgAlert msgAlert = new MsgAlert(this);

            if (res.trim().equalsIgnoreCase("OK") || res.trim().toUpperCase().startsWith("**WARNING:") || res.trim().toUpperCase().startsWith("***WARNING:")) {
                if (res.trim().equalsIgnoreCase("OK")) {
                    new Toasty(this, "Password has been changed.", Toast.LENGTH_SHORT).showMessage();
                    if (changePasswordOnLogin && currentLoginActivity != null) {
                        currentLoginActivity.login(userName, newPassword);
                    }
                } else {
                    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new Toasty(getApplicationContext(), "Password has been partially changed.", Toast.LENGTH_SHORT).showMessage();
                        }
                    };
                    /** **WARNING message from the Server will remain a warning because the mobile app will still work
                     *  but !!ERROR will display to the user to indicate a problem the user should call about
                     */
                    if (res.trim().toUpperCase().contains("**WARNING:(CHANGESSOPASSWORD)") || res.trim().toUpperCase().contains("**WARNING:(UPDATESSOUSERRESOURCE)")) {
                        msgAlert.showMessage("!!ERROR: Part of Password Change failed. Please contact STSBAC.", "ERROR DETAILS:<br/>" + res.trim().replaceFirst("\\*\\*\\*WARNING:", "").replaceFirst("\\*\\*WARNING:", ""), onClickListener);
                    } else {
                        msgAlert.showMessage("!!ERROR: Problem during Password Change. Please contact STSBAC.", "ERROR DETAILS:<br/>" + res.trim().replaceFirst("\\*\\*\\*WARNING:", "").replaceFirst("\\*\\*WARNING:", ""), onClickListener);
                    }
                }
            } else if (res.contains("ORA-20003") || res.contains("ORA-20002") || res.contains("ORA-00988") || res.contains("ORA-28003")) {

                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            showChangePasswordDialog(oldPasswordF, null, null, oldPasswordRequiredF, ChangePasswordDialog.NEWPASSWORDFOCUS);
                        }
                    }
                };

                msgAlert.showMessage("!!ERROR: SFMS Password Policy Issue", res.trim(), onClickListener);
            } else {

                DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == Dialog.BUTTON_POSITIVE) {
                            showChangePasswordDialog(oldPasswordF, null, null, oldPasswordRequiredF, ChangePasswordDialog.NEWPASSWORDFOCUS);
                        }
                    }
                };

                if (res.contains("ORA-28007")) {
                    msgAlert.showMessage("!!ERROR: Password cannot be reused", res.trim(), onClickListener);
                } else if (res.contains("ORA-28002")) {
                    msgAlert.showMessage("!!ERROR: Your password will expire soon", res.trim(), onClickListener);
                } else {
                    msgAlert.showMessage("!!ERROR: Change Password", res.trim(), onClickListener);
                }

            }
        } catch (InterruptedException e) {
            MsgAlert msgAlert = new MsgAlert(this);
            msgAlert.showMessage("!!ERROR: Interruption Exception. Please contact STSBAC.", "!!ERROR: " + e.getMessage() + ". Please Contact STSBAC.");
            e.printStackTrace();
        } catch (ExecutionException e) {
            MsgAlert msgAlert = new MsgAlert(this);
            msgAlert.showMessage("!!ERROR: Execution Exception. Please contact STSBAC.", "!!ERROR: " + e.getMessage() + ". Please Contact STSBAC.");
            e.printStackTrace();
        } catch (Exception e) {
            MsgAlert msgAlert = new MsgAlert(this);
            msgAlert.showMessage("!!ERROR: Generic Exception. Please contact STSBAC.", "!!ERROR: " + e.getMessage() + ". Please Contact STSBAC.");
            e.printStackTrace();
        }


    }

    public void noServerResponse(String testLocation) {
        noServerResponse(testLocation, InvApplication.getInstance());
    }

    public void noServerResponse(String testLocation, Context context) {
        Log.i(this.getClass().getName(), "NOSERVERRESPONSE (SENATEACTIVITY)");
        Log.i(this.getClass().getName(), "noServerResponse:"+testLocation);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set title
        alertDialogBuilder.setTitle(Html
                .fromHtml("<b><font color='#000055'>NO SERVER RESPONSE</font></b>"));

        // set dialog message
        alertDialogBuilder
                .setMessage(
                        Html.fromHtml("!!ERROR: There was <font color='RED'><b>NO SERVER RESPONSE</b></font>. <br/> Please contact STS/BAC."))
                .setCancelable(false)
                .setPositiveButton(Html.fromHtml("<b>Ok</b>"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        Context context = getApplicationContext();

                        CharSequence text = "No action taken due to NO SERVER RESPONSE";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });

        try {
            new Toasty(SenateActivity.stContext).showMessage("!!ERROR: App is Offline.");
        }
        catch (Exception e) {
            Log.e(this.getClass().getName(), "!!ERROR: Could not show toasty message that App is Offline!!");
        }

        new HttpUtils().playSound(R.raw.noconnect);
        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

}
