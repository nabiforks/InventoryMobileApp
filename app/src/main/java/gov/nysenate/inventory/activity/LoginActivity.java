package gov.nysenate.inventory.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import gov.nysenate.inventory.android.*;
import gov.nysenate.inventory.listener.ChangePasswordDialogListener;
import gov.nysenate.inventory.model.DBAdapter;
import gov.nysenate.inventory.model.LoginStatus;
import gov.nysenate.inventory.util.Toasty;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

//   WIFI Code Added Below

public class LoginActivity extends SenateActivity implements
ChangePasswordDialogListener
{
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    // WIFI Code Added Below
    WifiManager mainWifi;
    // WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    ScanResult currentWifiResult;
    int senateWifiFound = -1;
    int senateVisitorWifiFound = -1;
    boolean updateChecked = false;
    int connectedTo = -1;
    String senateSSID = "";
    String senateSSIDpwd = "";
    String senateVisitorSSID = "";
    String wifiMessage = "" /* "Horrible News!!! Currently no Wifi Networks found!!! You need a Wifi network (Preferrably a NY Senate one) in order to use this app." */;
    String currentSSID = "";
    public static String defrmint = null;
    public static String nauser = null;
    Resources resources = null;
    static ClearableEditText user_name;
    static ClearableEditText password;
    String URL = "";
    int lastNumericErrorLength = 0;
    public static Properties properties; // Since we want to refer to this in
                                         // other activities
    public static int senateWifiFoundCount = 0;
    public static int senateWifiLostCount = 0;
    public static int chkintWifiFoundCount = 0;
    public static int chkintWifiLostCount = 0;
    public static Date wifiCountStart;
    
    static AssetManager assetManager;
    Button buttonLogin;
    ProgressBar progressBarLogin;

    private static final String LOG_TAG = "AppUpgrade";
    // private MyWebReceiver receiver;
    private int versionCode = 0;
    private String versionName = null;
    static String appURI = "";
    static String latestVersionName;
    static int latestVersion;

    AudioManager audio;
    Activity currentActivity;
    String status = "no";
    boolean timeoutActivity = false;
    String timeoutFrom = null;
    public static TextView tvWarnLabel;
    boolean updateApp = false;

    public long lastTimeCheck = 0;
    public int lastLengthCheck = 0;

    public static DefaultHttpClient httpClient;
    AlertDialog alertDialog = null;
    public static LoginStatus loginStatus = new LoginStatus();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        super.currentLoginActivity = this;
        setContentView(R.layout.activity_login);
        registerBaseActivityReceiver();
        if (wifiCountStart==null) {
            wifiCountStart = new Date();
        }
        // See if there is a Parent Activity, if there was one, then it must
        // have timed out.
        try {
            Intent fromIntent = getIntent();
            timeoutFrom = fromIntent.getStringExtra("TIMEOUTFROM");
        } catch (Exception e) {
            timeoutFrom = null;
        }

        try {
            Intent fromIntent = getIntent();
            updateChecked = Boolean.valueOf(fromIntent
                    .getStringExtra("UPDATECHECKED"));
        } catch (Exception e2) {
            updateChecked = false;
            Log.i("UPDATECHECKED", "EXCEPTION SO ASSUME FALSE");
        }

        // Red Text Message
        tvWarnLabel = (TextView) findViewById(R.id.tvWarnLabel);

        if (timeoutFrom != null) {
            timeoutActivity = true;
        }

        currentActivity = this;
        resources = this.getResources();
        user_name = (ClearableEditText) findViewById(R.id.user_name);
        password = (ClearableEditText) findViewById(R.id.password);
        password.addTextChangedListener(senateTagPWDWatcher);
        //testSQLlite();
        if (timeoutActivity) {
            user_name.setKeyListener(null);
            user_name.setText(nauser);
            user_name.removeClearButton();
            user_name.setBackgroundResource(R.drawable.customshape);
            user_name.setOnTouchListener(new OnTouchListener()
            {

                @Override
                public boolean onTouch(View arg0, MotionEvent arg1) {
                    password.requestFocus();
                    return true;
                }

            });
            password.requestFocus();
            tvWarnLabel.setText("TIME OUT\r\nPlease enter your password");
            if (timeoutActivity) {
                this.playSound(R.raw.timeout_julie);
            }
        } else {
            tvWarnLabel.setText("");
        }
        user_name.addTextChangedListener(senateTagUSRWatcher);

        // Read from the /assets directory for properties of the project
        // we can modify this file and the URL will be changed
        // Resources resources = this.getResources();
        progressBarLogin = (ProgressBar) findViewById(R.id.progressBarLogin);
        buttonLogin = (Button) findViewById(R.id.buttonLogin);

        AssetManager assetManager = resources.getAssets();
        try {
            InputStream inputStream = assetManager.open("invApp.properties");
            properties = new Properties();
            properties.load(inputStream); // we load the properties here and we
                                          // use same object elsewhere in
                                          // project
            URL = properties.get("WEBAPP_BASE_URL").toString();
            if (! URL.endsWith("/")) {
                URL += "/";
            }              
            this.defrmint = properties.get("DEFRMINT").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!this.timeoutActivity) {
            checkInitialWifiConnection();
            checkInitialAudioLevel();
        }
    }

    private TextWatcher senateTagPWDWatcher = new TextWatcher()
    {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            lastTimeCheck = System.currentTimeMillis();
            lastLengthCheck = s.toString().length();
        }

        @Override
        public void afterTextChanged(Editable s) {

            if (lastTimeCheck == 0) {
                if (s.toString().length() > lastLengthCheck + 4) {
                    if (timeoutActivity) {
                        playSound(R.raw.timeout_julie);
                    } else {
                        playSound(R.raw.login_julie);
                    }
                    s.clear();
                    lastNumericErrorLength = s.toString().length();
                    lastTimeCheck = System.currentTimeMillis();
                    lastLengthCheck = s.toString().length();
                } else {
                    lastNumericErrorLength = s.toString().length();
                    lastTimeCheck = System.currentTimeMillis();
                    lastLengthCheck = s.toString().length();
                }
            } else if (s.toString().length() > lastLengthCheck + 4) {
                if (System.currentTimeMillis() - lastTimeCheck < 300) {
                    if (timeoutActivity) {
                        playSound(R.raw.timeout_julie);
                    } else {
                        playSound(R.raw.login_julie);
                    }
                    s.clear();
                }

            } else if (s.length() == 0) {
                lastNumericErrorLength = s.toString().length();
                lastTimeCheck = System.currentTimeMillis();
                lastLengthCheck = s.toString().length();
            }
            
        }
    };

    private TextWatcher senateTagUSRWatcher = new TextWatcher()
    {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                int after) {
            lastTimeCheck = System.currentTimeMillis();
            lastLengthCheck = s.toString().length();
        }

        @Override
        public void afterTextChanged(Editable s) {

            if (lastTimeCheck == 0) {
                if (s.toString().length() > lastLengthCheck + 4) {
                    playSound(R.raw.login_julie);
                    if (timeoutActivity) {
                        s.clear();
                    } else {
                        user_name.setText(nauser);
                    }
                    lastNumericErrorLength = s.toString().length();
                    lastTimeCheck = System.currentTimeMillis();
                    lastLengthCheck = s.toString().length();
                } else {
                    lastNumericErrorLength = s.toString().length();
                    lastTimeCheck = System.currentTimeMillis();
                    lastLengthCheck = s.toString().length();
                }
            } else if (s.toString().length() > lastLengthCheck + 4) {
                if (System.currentTimeMillis() - lastTimeCheck < 300) {
                    playSound(R.raw.login_julie);
                    if (timeoutActivity) {
                        s.clear();
                    } else {
                        user_name.setText(nauser);
                    }
                    lastNumericErrorLength = s.toString().length();
                    lastTimeCheck = System.currentTimeMillis();
                    lastLengthCheck = s.toString().length();
                }
            } else if (s.length() == 0) {
                lastNumericErrorLength = s.toString().length();
                lastTimeCheck = System.currentTimeMillis();
                lastLengthCheck = s.toString().length();
            }
        }

    };

    public void checkInitialWifiConnection() {
        try {
            // WIFI Code Added Below
            senateWifiFound = -1;
            senateVisitorWifiFound = -1;
            connectedTo = -1;
            boolean enablingWifi = false;
            long startTime = System.currentTimeMillis();
            mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            while (!mainWifi.isWifiEnabled()
                    || System.currentTimeMillis() - startTime > 3000) {
                if (!enablingWifi) {
                    mainWifi.setWifiEnabled(true);
                    enablingWifi = true;
                    Thread.sleep(1000);
                }
            }
            if (mainWifi.isWifiEnabled()) {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                if (enablingWifi) {
                    Toast toast = Toast.makeText(context,
                            "Wifi has been enabled.", duration);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE); // done
                                                                                     // Again
                                                                                     // BH
                }
            } else {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast
                        .makeText(
                                context,
                                "Unable to Enable Wifi Connection necessary to login to this app. Please fix before continuing or contact STS/BAC.",
                                duration);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }

            mainWifi.startScan();
            // get list of the results in object format ( like an array )
            wifiList = mainWifi.getScanResults();
            WifiInfo connectionInfo = mainWifi.getConnectionInfo();

            if (connectionInfo != null) {
                currentSSID = connectionInfo.getSSID().trim()
                        .replaceAll("\"", "");
            } else {
                currentSSID = "";
            }
            for (int x = 0; x < wifiList.size(); x++) {
                currentWifiResult = wifiList.get(x);
                if (currentSSID.length() > 0) {
                    if (currentSSID.equals(currentWifiResult.SSID)) {
                        connectedTo = x;
                    }
                }

                if (currentWifiResult.SSID.equalsIgnoreCase(senateSSID)) {
                    senateWifiFound = x;
                }
                if (currentWifiResult.SSID.equalsIgnoreCase(senateVisitorSSID)) {
                    senateVisitorWifiFound = x;
                }
            }

            if (connectedTo == -1) {
                // wifiMessage = wifiMessage +
                // " <font color='RED'>AAAA Currently not connected to a network ("+senateWifiFound+")</font>";
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context,
                        "NO WIFI NETWORK CONNECTION!!!", duration);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                if (senateWifiFound > -1) { // A MUCH better option would be to
                                            // connect to Senate Net Instead
                    // 1. Instantiate an AlertDialog.Builder with its
                    // constructor
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    // 2. Chain together various setter methods to set the
                    // dialog characteristics
                    builder.setMessage(
                            "Would you like to connect NY Senate Network instead? (You are connected to NY Senate Visitor Network)")
                            .setTitle(
                                    Html.fromHtml("<font color='#000055'>Connect to NY Senate Network</font>"))

                    // Add the buttons
                    .setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    WifiConfiguration conf = new WifiConfiguration();
                                    conf.SSID = "\"" + senateSSID + "\""; // Please
                                                                          // note
                                                                          // the
                                                                          // quotes.
                                                                          // String
                                                                          // should
                                                                          // contain
                                                                          // ssid
                                                                          // in
                                                                          // quotes
                                    conf.preSharedKey = "\"" + senateSSIDpwd
                                            + "\"";
                                    mainWifi.addNetwork(conf);
                                    List<WifiConfiguration> list = mainWifi
                                            .getConfiguredNetworks();
                                    for (WifiConfiguration i : list) {
                                        if (i.SSID != null
                                                && i.SSID.equals("\""
                                                        + senateSSID + "\"")) {
                                            mainWifi.disconnect();
                                            mainWifi.enableNetwork(i.networkId,
                                                    true);
                                            mainWifi.reconnect();

                                            break;
                                        }
                                    }

                                }
                            });
                    builder.setNegativeButton(Html.fromHtml("<b>No</b>"),
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int id) {
                                    // User cancelled the dialog
                                }
                            });

                    // 3. Get the AlertDialog from create()
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            } else {
                if (connectedTo == senateWifiFound) { // GREAT!!! We are
                                                      // connected to Senate
                                                      // Net
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context,
                            "Connected to Senate Network.", duration);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else if (connectedTo == senateVisitorWifiFound) { // NOT BAD,
                                                                    // We are
                                                                    // connected
                                                                    // to Senate
                                                                    // Visitor
                                                                    // Net
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context,
                            "Connected to Senate Visitor Network.", duration);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    if (senateWifiFound > -1) { // A MUCH better option would be
                                                // to connect to Senate Net
                                                // Instead
                        // 1. Instantiate an AlertDialog.Builder with its
                        // constructor
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                this);

                        // 2. Chain together various setter methods to set the
                        // dialog characteristics
                        builder.setMessage(
                                "Would you like to connect NY Senate Network instead? (You are connected to NY Senate Visitor Network)")
                                .setTitle(
                                        Html.fromHtml("<font color='#000055'>Connect to NY Senate Network</font>"));
                        // Add the buttons
                        builder.setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        WifiConfiguration conf = new WifiConfiguration();
                                        conf.SSID = "\"" + senateSSID + "\""; // Please
                                                                              // note
                                                                              // the
                                                                              // quotes.
                                                                              // String
                                                                              // should
                                                                              // contain
                                                                              // ssid
                                                                              // in
                                                                              // quotes
                                        conf.preSharedKey = "\""
                                                + senateSSIDpwd + "\"";
                                        mainWifi.addNetwork(conf);
                                        List<WifiConfiguration> list = mainWifi
                                                .getConfiguredNetworks();
                                        for (WifiConfiguration i : list) {
                                            if (i.SSID != null
                                                    && i.SSID
                                                            .equals("\""
                                                                    + senateSSID
                                                                    + "\"")) {
                                                mainWifi.disconnect();
                                                mainWifi.enableNetwork(
                                                        i.networkId, true);
                                                mainWifi.reconnect();

                                                break;
                                            }
                                        }

                                    }
                                });
                        builder.setNegativeButton(Html.fromHtml("<b>No</b>"),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        // User cancelled the dialog
                                    }
                                });

                        // 3. Get the AlertDialog from create()
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                } else {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, "Connected to "
                            + currentSSID + ".", duration);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }

            // EditText text = (EditText) findViewById(R.id.wifiMessage);
            TextView t = (TextView) findViewById(R.id.textView1);
            t.setTextColor(Color.BLACK);
            if (connectionInfo != null
                    && connectionInfo.getSSID().trim().length() > 0) {
                t.setText(Html.fromHtml("<h1>" + wifiMessage + "</h1>"));

            } else {
                t.setText(Html.fromHtml("<h1> (No Current Connection Info) "
                        + wifiMessage + "</h1>"));
            }

        } catch (Exception e) {
            TextView t = (TextView) findViewById(R.id.textView1);
            t.setText(e.getMessage());
            StackTraceElement[] trace = e.getStackTrace();
            for (int x = 0; x < trace.length; x++) {
                t.setText(t.getText() + trace[x].toString());
            }
            t.setText(Html.fromHtml("<h1>" + t.getText() + "</h1>"));
            t.setTextColor(Color.RED);

        }
        // Overall information about the contents of a package
        // This corresponds to all of the information collected from
        // AndroidManifest.xml.
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        // get the app version Code for checking
        this.versionCode = pInfo.versionCode;
        this.versionName = pInfo.versionName;
        //Log.i("onCreate VERSION CODE", "versionCode:" + versionCode);
        // display the current version in a TextView

        // Broadcast receiver for our Web Request
        // IntentFilter filter = new
        // IntentFilter(MyWebReceiver.PROCESS_RESPONSE);
        // filter.addCategory(Intent.CATEGORY_DEFAULT);
        // receiver = new MyWebReceiver();
        // registerReceiver(receiver, filter);

        // Broadcast receiver for the download manager
        // filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        // registerReceiver(downloadReceiver, filter);

        // check of internet is available before making a web service request
        updateApp = false;

        if (isNetworkAvailable(this)) {
            Intent msgIntent = new Intent(this, InvWebService.class);

            try {
                // Get the URL from the properties
                String URL = LoginActivity.properties.get("WEBAPP_BASE_URL")
                        .toString();
                if (! URL.endsWith("/")) {
                    URL += "/";
                }                   
                AsyncTask<String, String, String> resr1 = new RequestTask()
                        .execute(URL
                                + "CheckAppVersion?appName=InventoryMobileApp.apk");
                try {
                    String res = resr1.get().trim().toString();
                    if (res == null) {
                        noServerResponse();
                    } else if (res.trim().length() == 0) {
                        noServerResponse();
                    } else {

                        JSONObject responseObj;
                        try {
                            Log.d(this.getClass().getName(), "CheckAppVersion Res:"+res);
                            responseObj = new JSONObject(res);
                            boolean success = responseObj.getBoolean("success");
                            //Log.i("LoginActivity", "CheckAppVersion returned success:"+ success);
                            // if the reponse was successful check further
                            if (success) {
                                // get the latest version from the JSON string
                                latestVersion = responseObj
                                        .getInt("latestVersion");

                                // get the lastest application URI from the JSON
                                // string
                                appURI = responseObj.getString("appURI");
                                latestVersionName = responseObj
                                        .getString("latestVersionName");
                                appURI = responseObj.getString("appURI");
                                // check if we need to upgrade?
/*                                Log.i("LoginActivity","CheckAppVersion: Is latestVersion:"
                                                + latestVersion
                                                + " > versionCode:"
                                                + versionCode);*/

                                if (latestVersion > versionCode) {
                                    updateApp = true;
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    // TODO Auto-generated catch block
                    noServerResponse();
                }
            } catch (Exception e) {

            }
            // msgIntent.putExtra(InvWebService.REQUEST_STRING, URL
            // + "/CheckAppVersion?appName=InventoryMobileApp.apk");
            // startService(msgIntent);
        }
        if (!timeoutActivity && !updateChecked) {
            this.startUpdate(null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    private void checkInitialAudioLevel() {
        // Check Tablet Audio Volume. If it is too low or turned off, ask user
        // if they would like to turn it up or on in a dialog

        try {
            audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio
                    .getStreamVolume(AudioManager.STREAM_MUSIC);
            final int maxVolume = audio
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            String msg = null;
            boolean showAudioMsg = false;

            if (currentVolume == 0) {
                msg = "**WARNING: The volume is currently <font color='RED'><b>OFF</b></font>. You will not hear any <b>WARNING</b> or <b>ERROR</b> sounds. Would you like to turn up the volume so you can hear these sounds?";
                showAudioMsg = true;
            } else if (currentVolume < .4 * maxVolume) {
                msg = "**WARNING: The volume is currently is <font color='RED'><b>LOW</b></font>. You might not hear any <b>WARNING</b> or <b>ERROR</b> sounds. Would you like to turn up the volume so you can hear these sounds?";
                showAudioMsg = true;
            }

            final int duration = Toast.LENGTH_LONG;

            if (showAudioMsg) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        this);

                // set title
                if (currentVolume == 0) {
                    alertDialogBuilder
                            .setTitle(Html
                                    .fromHtml("<b><font color='#000055'>**WARNING: NO SOUND</font></b>"));
                } else if (currentVolume < .4 * maxVolume) {
                    alertDialogBuilder
                            .setTitle(Html
                                    .fromHtml("<b><font color='#000055'>**WARNING: LOW SOUND</font></b>"));
                }

                // set dialog message
                alertDialogBuilder
                        .setMessage(Html.fromHtml(msg))
                        .setCancelable(false)
                        .setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        audio.setStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                maxVolume, 0);
                                        Toast toast = Toast
                                                .makeText(
                                                        getApplicationContext(),
                                                        "Sound has been turned on to Maximum Volume.",
                                                        duration);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();

                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(Html.fromHtml("<b>No</b>"),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        // if this button is clicked, just close
                                        // the dialog box and do nothing
                                        Toast toast = Toast
                                                .makeText(
                                                        getApplicationContext(),
                                                        "Sound Volume was NOT changed.",
                                                        duration);
                                        toast.setGravity(Gravity.CENTER, 0, 0);
                                        toast.show();
                                        dialog.dismiss();
                                    }
                                });

                // create alert dialog
                alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void login(String user_name, String password) {
        login(user_name, password, null);
    }

    public void login(String user_name, String password, LoginStatus loginStatusParam) {
        loginStatus = new LoginStatus();
        MsgAlert msgAlert = new MsgAlert(this);
        try {
            // check network connection
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            String res = null;

            if (networkInfo != null && networkInfo.isConnected()) {
                // fetch data
                status = "yes";
                if (loginStatusParam == null) {
                try {
                    // Get the URL from the properties
                    //if (loginStatusParam==null) 
                    String URL = LoginActivity.properties
                            .get("WEBAPP_BASE_URL").toString();
                    if (! URL.endsWith("/")) {
                        URL += "/";
                    }                       
                    AsyncTask<String, String, String> resr1 = new RequestTask()
                            .execute(URL + "Login?user=" + user_name + "&pwd="
                                    + password+"&defrmint="+defrmint);
                    try {
                        /*System.out.println("login url:"+ "/Login?user=" + user_name + "&pwd="
                                + password+"&defrmint="+defrmint);*/
                        res = resr1.get().trim().toString();
                        //System.out.println("login Result:"+res);

                        /*
                        * Serializer was used on the Server to convert LoginStatus to a JSON Object,
                        * but currently deserializing on the client is not working (object returned
                        * but values were set to null). Had to change the format mask for dtpasswdexpire
                        * to match the new format mask set on the server within the LoginStatus.parseHSON method
                        * */

                        loginStatus.parseJSON(res);

                        if (res == null) {
                            noServerResponse();
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        // TODO Auto-generated catch block
                        noServerResponse();
                    }
                } catch (Exception e) {

                }
                status = "yes1";
                LoginActivity.nauser = user_name;
            } else {
                // display error
                status = "no";
                buttonLogin.getBackground().setAlpha(255);
                progressBarLogin.setVisibility(View.INVISIBLE);
                LoginActivity.nauser = null;
            }

            // Create the text view
            TextView textView = new TextView(this);
            textView.setTextSize(40);            
            
            // calling the menu activity after validation
            if (loginStatus.getNustatus() == loginStatus.VALID ) {
                // If LoginActivity was called because the App Timed Out..,
                // Go back to the activity of the timeout.
                // If it is not an Application Timed Out, go to the App main
                // menu
                //
                proceedPastLoginScreen(loginStatus);
            }
            else if (loginStatus.getNustatus() == loginStatus.ACCOUNT_LOCKED ) {
                msgAlert = new MsgAlert(
                        getApplicationContext(),
                        "Account Locked",
                        "!!ERROR: Your account is locked. Please contact STS/BAC.");
                buttonLogin.getBackground().setAlpha(255);
                progressBarLogin.setVisibility(View.INVISIBLE);
                LoginActivity.password.setText("");
            }
            else if (loginStatus.getNustatus() == loginStatus.PASSWORD_EXPIRED) {
                allowUserToChangePassword(loginStatus);
            }
            else if (loginStatus.getNustatus() == loginStatus.PASSWORD_EXPIRES_SOON) {
                final LoginStatus loginStatusFinal = loginStatus;
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        LoginActivity.this);
                builder.setMessage(
                        Html.fromHtml(loginStatusFinal.getDestatus()))
                        .setTitle(
                                Html.fromHtml("<b><font color='#000055'>Your Password will Expire Soon</font></b>"))
                        .setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        allowUserToChangePassword(loginStatusFinal);
                                        buttonLogin.getBackground().setAlpha(255);
                                        // User cancelled the dialog
                                        // finish();
                                        
                                    }
                                })
                        .setNegativeButton(Html.fromHtml("<b>No</b>"),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                            int id) {
                                        buttonLogin.getBackground().setAlpha(255);
                                        proceedPastLoginScreen(loginStatusFinal);
                                        
                                        dialog.dismiss();
                                        // User cancelled the dialog
                                        // finish();
                                        
                                    }
                                });
                
                // show the alert message
                Dialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();      
            }
            else if (loginStatus.getNustatus() == loginStatus.NO_ACCESS) {
                
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            LoginActivity.this);
                    builder.setMessage(
                            Html.fromHtml(loginStatus.getDestatus()))
                            .setTitle(
                                    Html.fromHtml("<b><font color='#000055'>NO SECURITY CLEARANCE FOR THIS APP</font></b>"))
                            .setPositiveButton(Html.fromHtml("<b>Close App</b>"),
                                    new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog,
                                                int id) {
                                            // User cancelled the dialog
                                            // finish();
                                            closeAllActivities();
                                        }
                                    });
                    // show the alert message
                    Dialog dialog = builder.create();
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();         
            }
            else if (loginStatus.getNustatus() == loginStatus.INVALID_USERNAME_OR_PASSWORD) {       
                new Toasty(this).showMessage( "!!ERROR: Invalid Username and/or Password.", Toast.LENGTH_LONG);
                buttonLogin.getBackground().setAlpha(255);
                progressBarLogin.setVisibility(View.INVISIBLE);
                LoginActivity.password.setText("");

            }
            else {
                //new Toasty(this).showMessage( loginStatus.getDestatus(), Toast.LENGTH_LONG);
                msgAlert.showMessage("!!ERROR: Problem with logging in. Please contact STSBAC.", loginStatus.getDestatus());
                buttonLogin.getBackground().setAlpha(255);
                progressBarLogin.setVisibility(View.INVISIBLE);
                LoginActivity.password.setText("");
            }
          }
        } catch (Exception e) {

            msgAlert.showMessage("!!Error: Problem connecting to Mobile App Server.", e.getMessage()+". Please contact STSBAC.");
/*            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(this,
                    "Problem connecting to Mobile App Server. Please contact STSBAC.("
                            + e.getMessage() + ")", duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();*/
        }
    }
    
    private void proceedPastLoginScreen(LoginStatus loginStatus) {
        String level = loginStatus.getCdseclevel();
        InvApplication app = ((InvApplication)getApplicationContext());
        app.setCdseclevel(Integer.valueOf(level));

        if (timeoutActivity) {
            Intent i = getIntent();
            setResult(RESULT_OK, i);
            finish();
        } else {
            Intent intent2 = new Intent(this, MenuActivity.class);
            startActivity(intent2);
            overridePendingTransition(R.anim.slide_in_left,
                    R.anim.slide_out_left);
        }
    }

    public ChangePasswordDialog allowUserToChangePassword(LoginStatus loginStatus) {
        return allowUserToChangePassword(loginStatus, null, null, null);
    }
    
    public ChangePasswordDialog allowUserToChangePassword(LoginStatus loginStatus, String oldPassword, String newPassword, String confirmPassword) {
        
            playSound(R.raw.warning);
            String title = "**WARNING: Your password has expired. Please enter a New Password";
            String message = "";
            changePasswordOnLogin = true;
                        
            changePasswordDialog = new ChangePasswordDialog(this, title, message, false, oldPassword, newPassword, confirmPassword);
            changePasswordDialog.addListener(this);
            changePasswordDialog.setRetainInstance(true);
            changePasswordDialog.show(fragmentManager, "change_password_dialog");
            
            return changePasswordDialog;
            
     }

    public void noServerResponse() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(Html
                .fromHtml("<b><font color='#000055'>NO SERVER RESPONSE</font></b>"));

        // set dialog message
        alertDialogBuilder
                .setMessage(
                        Html.fromHtml("!!ERROR: There was <font color='RED'><b>NO SERVER RESPONSE</b></font>. <br/> Please contact STS/BAC."))
                .setCancelable(false)
                .setPositiveButton( Html.fromHtml("<b>Ok</b>"), new DialogInterface.OnClickListener()
                {
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
                        progressBarLogin.setVisibility(View.INVISIBLE);
                        buttonLogin.getBackground().setAlpha(255);
                        dialog.dismiss();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    @Override
    protected void onResume(Bundle savedInstanceState) {
        super.onResume(savedInstanceState);
        httpClient = new DefaultHttpClient();
        buttonLogin.getBackground().setAlpha(255);
        progressBarLogin.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        // unregister your receivers
        if (!this.timeoutActivity) {
            try {
                // this.unregisterReceiver(receiver);
            } catch (Exception e) {
                Log.w("LoginActivity",
                        "**WARNING: unable to unregister Internet Connection Receiver.");
            }
            try {
                // this.unregisterReceiver(downloadReceiver);
            } catch (Exception e) {
                Log.w("LoginActivity",
                        "**WARNING: unable to unregister Download Receiver.");
            }
        }

        super.onDestroy();
    }

    // check for internet connection
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    Log.v(LOG_TAG, String.valueOf(i));
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        Log.v(LOG_TAG, "connected!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Properties getProperties() {
        if (properties == null) {
            InputStream inputStream = null;
            try {
                inputStream = assetManager.open("invApp.properties");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                MsgAlert msgAlert = new MsgAlert(
                        getApplicationContext(),
                        "Could not Open Properties File",
                        "!!ERROR: The Inventory Mobile App could not open the Properties File. Please contact STS/BAC.");

                e1.printStackTrace();
            }
            properties = new Properties();
            try {
                properties.load(inputStream);
            } catch (IOException e) {
                MsgAlert msgAlert = new MsgAlert(
                        getApplicationContext(),
                        "Could not Load Properties File",
                        "!!ERROR: The Inventory Mobile App could not open the Properties File. Please contact STS/BAC.");

            } // we load the properties here and we
              // use same object elsewhere in
              // project

        }
        return properties;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void validate(View view) {
        if (view.getId() == R.id.buttonLogin) {
            if (buttonLogin.getText().toString().equalsIgnoreCase("Close")) {
                finish();
            } else {
                buttonLogin.getBackground().setAlpha(70);
                progressBarLogin.setVisibility(View.VISIBLE);
                String u_name = user_name.getText().toString();
                String pwd = password.getText().toString();
                
                if (u_name.trim().length()==0 & pwd.trim().length()==0) {
                    new Toasty(getApplicationContext(), "!!ERRROR: Username and password must be entered.").showMessage();
                    buttonLogin.getBackground().setAlpha(255);
                    progressBarLogin.setVisibility(View.INVISIBLE);
                    return;
                }
                else if (u_name.trim().length()==0) {
                    new Toasty(getApplicationContext(), "!!ERRROR: Username must be entered.").showMessage();
                    buttonLogin.getBackground().setAlpha(255);
                    progressBarLogin.setVisibility(View.INVISIBLE);
                    return;
               }
               else if (pwd.trim().length()==0) {
                   new Toasty(getApplicationContext(), "!!ERRROR: Password must be entered.").showMessage();
                   buttonLogin.getBackground().setAlpha(255);
                   progressBarLogin.setVisibility(View.INVISIBLE);
                   return;
               }
                
                
               this.login(u_name, pwd);
               progressBarLogin.setVisibility(View.INVISIBLE);
                /*
                 * Intent intent = new Intent(this,
                 * DisplayMessageActivity.class); // Intent intent = new
                 * Intent(this, MenuActivity.class); String u_name =
                 * user_name.getText().toString(); String pwd =
                 * password.getText().toString(); intent.putExtra(u_name_intent,
                 * u_name); intent.putExtra(pwd_intent, pwd);
                 * startActivity(intent);
                 * overridePendingTransition(R.anim.in_right, R.anim.out_left);
                 */
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (this.timeoutActivity) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context,
                    "!!ERROR: You must first enter you password.", duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }

    }

    public void testSQLlite() {
        DBAdapter db = new DBAdapter(this);
        try {
             db.resetDB();
             db.truncateTable("ad12verinv");
            
             long rowsInserted = db.insert("ad12verinv",
             "nuserial|nusenate|decommodityf|dttxnorigin|natxnorguser|dttxnupdate|natxnupduser"
             ,
             "111111|NEWSDFJS|THIS IS THE FIRST TEST|NOW|HEITNER|NOW|HEITNER"
             );
             
             Log.i(LoginActivity.class.getName(),
             "ROWS INSERTED:"+rowsInserted);
             

            Cursor mCursor = db.rawQuery("SELECT * FROM ad12serial", null);

            if (mCursor != null) {
                if (mCursor.moveToFirst()) {
                    do {
                        String nusenate = mCursor.getString(mCursor
                                .getColumnIndex("nusenate"));
                        String decommodityf = mCursor.getString(mCursor
                                .getColumnIndex("decommodityf"));
                        Log.i(LoginActivity.class.getName(), nusenate + ": "
                                + decommodityf);
                    } while (mCursor.moveToNext());
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void startUpdate(View View) {
        if (updateApp) {
            updateChecked = true;
            Intent intent = new Intent(this, UpgradeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.in_down, R.anim.out_down);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        /*case R.id.menu_sqllite:
            testSQLlite();
            return true;*/
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public static DefaultHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
        }
        return httpClient;
    }

    @Override
    public void onChangePasswordCancelButtonClicked() {
        this.password.setText("");
        this.password.requestFocus();
        if (changePasswordOnLogin && loginStatus!=null) {
            int nustatus = loginStatus.getNustatus();
            if (nustatus == loginStatus.PASSWORD_EXPIRES_SOON) {
                this.proceedPastLoginScreen(loginStatus);
            }
            else {
                this.buttonLogin.getBackground().setAlpha(255);

            }
        }
        
        }
}
