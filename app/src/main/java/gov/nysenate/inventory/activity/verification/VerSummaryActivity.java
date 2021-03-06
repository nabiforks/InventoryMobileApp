package gov.nysenate.inventory.activity.verification;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import gov.nysenate.inventory.activity.LoginActivity;
import gov.nysenate.inventory.activity.MenuActivity;
import gov.nysenate.inventory.activity.SenateActivity;
import gov.nysenate.inventory.adapter.InvListViewAdapter;
import gov.nysenate.inventory.android.AppSingleton;
import gov.nysenate.inventory.android.InvApplication;
import gov.nysenate.inventory.android.R;
import gov.nysenate.inventory.android.StringInvRequest;
import gov.nysenate.inventory.model.InvItem;
import gov.nysenate.inventory.util.HttpUtils;
import gov.nysenate.inventory.util.Toasty;

public class VerSummaryActivity extends SenateActivity {
    ArrayList<InvItem> unscannedItems = new ArrayList<InvItem>();
    ArrayList<InvItem> newItems = new ArrayList<InvItem>();
    ArrayList<InvItem> allScannedItems = new ArrayList<InvItem>();
    TextView totalItemCountView;
    TextView scannedItemCountView;
    TextView unscannedItemCountView;
    TextView newItemCountView;
    String barcodeNum = "";

    public String res = null;
    String loc_code = null;
    String cdloctype = null;

    static Button btnVerSumBack;
    static Button btnVerSumCont;
    ProgressBar progressVerSum;
    boolean positiveButtonPressed = false;
    Activity currentActivity;
    String timeoutFrom = "VERIFICATIONSUMMARY";
    public final int VERIFICATIONREPORTS_TIMEOUT = 101,
            CONTINUEBUTTON_TIMEOUT = 102;
    String URL = null;

    private InvListViewAdapter scannedItemsListAdapter;
    private InvListViewAdapter unscannedItemsListAdapter;
    private InvListViewAdapter newItemsListAdapter;
    private ListView scannedItemsListView;
    private ListView unscannedItemsListView;
    private ListView newItemsListView;

    private TabHost tabHost;

    Response.Listener submitItemsRespListener = new Response.Listener<String>() {

        @Override
        public void onResponse(String response) {

            // Display Toaster
            Context context = getApplicationContext();
            String text = response;
            int duration = Toast.LENGTH_LONG;
            if (response == null) {
                text = "!!ERROR: NO RESPONSE FROM SERVER";
            } else if (response.length() == 0) {
                text = "Database not updated";
            } else if (response.trim().startsWith("!!ERROR:")
                    || response.trim().startsWith("**WARNING:")) {
                text = response.trim();
            } else {
                duration = Toast.LENGTH_SHORT;
            }

            new Toasty(context).showMessage(text, duration);

            /*
             * If there was some kind of error, don't leave the Activity. User will
             * have to then call or cancel the verification. At least the user will
             * be aware that some problem occured instead of ignoring the message.
             */

            if (text.equals("Database not updated")
                    || text.toString().startsWith("!!ERROR:")
                    || text.toString().startsWith("**WARNING:")) {
                return;
            }

            // ===================ends
            Intent intent = new Intent(VerSummaryActivity.this, MenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            overridePendingTransition(R.anim.in_right, R.anim.out_left);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ver_summary);
        registerBaseActivityReceiver();
        AppSingleton.getInstance(this).timeoutFrom = "VERIFICATIONSUMMARY";

        currentActivity = this;

        // Summary Fields

        totalItemCountView = (TextView) findViewById(R.id.tvTotItemVSum);
        scannedItemCountView = (TextView) findViewById(R.id.tvTotScanVSum);
        unscannedItemCountView = (TextView) findViewById(R.id.tvMisItems);
        newItemCountView = (TextView) findViewById(R.id.tvNewItems);

        // Code for tab

        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabSpec spec1 = tabHost.newTabSpec("Tab 1");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Scanned");

        TabSpec spec2 = tabHost.newTabSpec("Tab 2");
        spec2.setIndicator("Unscanned");
        spec2.setContent(R.id.tab2);

        TabSpec spec3 = tabHost.newTabSpec("Tab 3");
        spec3.setIndicator("New/Found");
        spec3.setContent(R.id.tab3);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);

        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            TextView tv = (TextView) tabHost.getTabWidget().getChildAt(i)
                    .findViewById(android.R.id.title);
            tv.setTextSize(20);
        }

        // Find the ListView resource.
        scannedItemsListView = (ListView) findViewById(R.id.listView1);
        unscannedItemsListView = (ListView) findViewById(R.id.listView2);
        newItemsListView = (ListView) findViewById(R.id.listView3);

        // Setup Buttons and Progress Bar
        this.progressVerSum = (ProgressBar) findViewById(R.id.progressVerSum);
        VerSummaryActivity.btnVerSumBack = (Button) findViewById(R.id.btnVerSumBack);
        VerSummaryActivity.btnVerSumBack.getBackground().setAlpha(255);
        VerSummaryActivity.btnVerSumCont = (Button) findViewById(R.id.btnVerSumCont);
        VerSummaryActivity.btnVerSumCont.getBackground().setAlpha(255);

        unscannedItems = VerScanActivity.unscannedItems;
        newItems = VerScanActivity.newItems;
        allScannedItems = VerScanActivity.allScannedItems;

        loc_code = getIntent().getStringExtra("loc_code");
        cdloctype = getIntent().getStringExtra("cdloctype");
        totalItemCountView.setText(String.valueOf(getIntent().getIntExtra("totalItemCount", -1)));

        initializeItemCountViews();

        TextView locCodeView = (TextView) findViewById(R.id.textView2);
        locCodeView.setText(Verification.autoCompleteTextView1.getText());

        // Create ArrayAdapter using the planet list.
        scannedItemsListAdapter = new InvListViewAdapter(this, R.layout.invlist_item, allScannedItems);
        unscannedItemsListAdapter = new InvListViewAdapter(this, R.layout.invlist_item, unscannedItems);
        newItemsListAdapter = new InvListViewAdapter(this, R.layout.invlist_item, newItems);
        // Set the ArrayAdapter as the ListView's adapter.
        scannedItemsListView.setAdapter(scannedItemsListAdapter);
        unscannedItemsListView.setAdapter(unscannedItemsListAdapter);
        newItemsListView.setAdapter(newItemsListAdapter);

    }

    private void initializeItemCountViews() {
        scannedItemCountView.setText(String.valueOf(allScannedItems.size()));
        unscannedItemCountView.setText(String.valueOf(unscannedItems.size()));
        newItemCountView.setText(String.valueOf(newItems.size()));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case VERIFICATIONREPORTS_TIMEOUT:
                if (resultCode == RESULT_OK) {
                    submitVerification();
                    break;
                }
            case CONTINUEBUTTON_TIMEOUT:
                break;
        }
    }

    public ArrayList<InvItem> getInvArrayListFromJSON(ArrayList<String> ar) {
        ArrayList<InvItem> returnList = new ArrayList<InvItem>();
        InvItem curInvItem;
        if (ar != null) {
            for (int x = 0; x < ar.size(); x++) {
                JSONObject jsonObject;
                try {
                    jsonObject = new JSONObject(ar.get(x));
                    curInvItem = new InvItem();
                    try {
                        curInvItem
                                .setNusenate(jsonObject.getString("nusenate"));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    try {
                        curInvItem.setCdcategory(jsonObject
                                .getString("cdcategory"));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    try {
                        curInvItem.setType(jsonObject.getString("type"));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                    try {
                        curInvItem.setDecommodityf(jsonObject
                                .getString("decommodityf"));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }

                    try {
                        curInvItem.setDecomments(jsonObject
                                .getString("decomments"));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }

                    try {
                        curInvItem.setCdcommodity(jsonObject
                                .getString("cdcommodity"));
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }

                    returnList.add(curInvItem);

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    Log.i("System.err",
                            "ERROR CONVERTING FROM ARRAYLIST OF JSON" + x + ":"
                                    + ar.get(x));
                    e.printStackTrace();
                }
            }
        }
        return returnList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_ver_summary, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        positiveButtonPressed = false;
        // Setup Buttons and Progress Bar
        this.progressVerSum = (ProgressBar) findViewById(R.id.progressVerSum);
        VerSummaryActivity.btnVerSumBack = (Button) findViewById(R.id.btnVerSumBack);
        VerSummaryActivity.btnVerSumBack.getBackground().setAlpha(255);
        VerSummaryActivity.btnVerSumCont = (Button) findViewById(R.id.btnVerSumCont);
        VerSummaryActivity.btnVerSumCont.getBackground().setAlpha(255);
        if (progressVerSum == null) {
            progressVerSum = (ProgressBar) this
                    .findViewById(R.id.progressVerSum);
        }
        progressVerSum.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        scannedItemsListAdapter.notifyDataSetChanged();
        unscannedItemsListAdapter.notifyDataSetChanged();
        newItemsListAdapter.notifyDataSetChanged();
        initializeItemCountViews();
        tabHost.setCurrentTab(0);
    }

    public void backButton(View view) {
        //if (checkServerResponse(true) == OK) {
        btnVerSumBack.getBackground().setAlpha(45);
        this.onBackPressed();
        // }
        // }
    }

    public void editButtonOnClick(View view) {
        //checkServerResponse();

        Intent editIntent = new Intent(this, EditVerification.class);
        startActivity(editIntent);
        overridePendingTransition(R.anim.in_right, R.anim.out_left);
    }

    public void noServerResponse() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(Html
                .fromHtml("<font color='#000055'>NO SERVER RESPONSE</font>"));

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

                        dialog.dismiss();
                    }
                });

        new HttpUtils().playSound(R.raw.noconnect);

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void continueButton(View view) {
        /*
         * "Check for Session by using KeepSessionAlive");
         */
        URL = LoginActivity.properties.get("WEBAPP_BASE_URL").toString();
        if (!URL.endsWith("/")) {
            URL += "/";
        }

        // Since AlertDialogs are asynchronous, need logic to display one at
        // a
        // time.
        if (foundItemsScanned()) {
            displayFoundItemsDialog();
        } else if (newItemsScanned()) {
            displayNewItemsDialog();
        } else {
            displayVerificationDialog();
        }
    }

    public void continueButtonTimeout() {
        Intent intentTimeout = new Intent(VerSummaryActivity.this,
                LoginActivity.class);
        intentTimeout.putExtra("TIMEOUTFROM", timeoutFrom);
        startActivityForResult(intentTimeout, CONTINUEBUTTON_TIMEOUT);
    }

    private void displayFoundItemsDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(Html
                .fromHtml("<font color='#000055'>Warning</font>"));
        dialogBuilder
                .setMessage(Html
                        .fromHtml("<font color='RED'><b>**WARNING:</font> The "
                                + numFoundItems()
                                + " Item/s found in OTHER</b> locations will be moved to the current location: <b>"
                                + loc_code
                                + "</b>. <br><br>"
                                + "Continue with Verification Submission (Y/N)?"));
        dialogBuilder.setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (newItemsScanned()) {
                            displayNewItemsDialog();
                        } else {
                            displayVerificationDialog();
                        }
                    }
                });

        dialogBuilder.setNegativeButton(Html.fromHtml("<b>No</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void displayNewItemsDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder
                .setTitle(Html
                        .fromHtml("<font color='#000055'>**WARNING: New items will not be tagged to location</font>"));
        dialogBuilder
                .setMessage(Html
                        .fromHtml("<font color='RED'><b>**WARNING:</font> The "
                                + " NEW Items scanned will "
                                + "NOT be tagged to location: "
                                + loc_code
                                + ".</b><br><br>"
                                + "Issue information for these items must be completed via the Inventory Issue Record E/U.<br><br>"
                                + "Continue with Verification Submission (Y/N)?"));
        dialogBuilder.setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        displayVerificationDialog();
                    }
                });

        dialogBuilder.setNegativeButton(Html.fromHtml("<b>No</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void displayVerificationDialog() {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog
                .setTitle(Html
                        .fromHtml("<font color='#000055'>Verification Confirmation</font>"));
        confirmDialog
                .setMessage("Are you sure you want to submit this verification?");
        confirmDialog.setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        if (positiveButtonPressed) {
                            /*
                             * Context context = getApplicationContext(); int
                             * duration = Toast.LENGTH_SHORT;
                             *
                             * Toast toast = Toast.makeText(context,
                             * "Button was already been pressed.",
                             * Toast.LENGTH_SHORT);
                             * toast.setGravity(Gravity.CENTER, 0, 0);
                             * toast.show();
                             */
                        } else {
                            positiveButtonPressed = true;
                            submitVerification();
                        }
                    }
                });

        confirmDialog.setNegativeButton(Html.fromHtml("<b>No</b>"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = confirmDialog.create();
        dialog.show();
    }

    private boolean newItemsScanned() {
        boolean exist = false;
        for (InvItem item : newItems) {
            if (item.getType().equalsIgnoreCase("NEW")) {
                exist = true;
                break;
            }
        }
        return exist;
    }

    private boolean foundItemsScanned() {
        boolean exist = false;
        for (InvItem item : newItems) {
            if (!item.getType().equalsIgnoreCase("NEW")
                    && !item.getType().equalsIgnoreCase("INACTIVE")) {
                exist = true;
                break;
            }
        }
        return exist;
    }

    private int numNewItems() {
        int numNewItems = 0;
        for (InvItem item : newItems) {
            if (item.getType().equalsIgnoreCase("NEW")
                    || item.getType().equalsIgnoreCase("INACTIVE")) {
                numNewItems++;
            }
        }
        return numNewItems;
    }

    private int numFoundItems() {
        return newItems.size() - numNewItems();
    }

    private void submitVerification() {
        VerSummaryActivity.btnVerSumCont.getBackground().setAlpha(45);
        progressVerSum.setVisibility(View.VISIBLE);
        barcodeNum = "";
        for (int i = 0; i < allScannedItems.size(); i++) {
            barcodeNum += allScannedItems.get(i).getNusenate() + ",";
        }

        // Create a JSON string from the arraylist

        // Send it to the server

        // check network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            final JSONArray jsonArray = new JSONArray();

            Log.i(this.getClass().getName(), "All Scanned Items: " + allScannedItems.size());
            for (int i = 0; i < allScannedItems.size(); i++) {
                jsonArray.put(allScannedItems.get(i).getJSONObject());
            }

            Map<String, String> params = new HashMap<String, String>();
            params.put("cdlocat", loc_code);
            params.put("cdloctype", cdloctype);
            params.put("scannedItems", jsonArray
                    .toString());

            Log.i(this.getClass().getName(), "All Scanned Items JSON: " + jsonArray
                    .toString());

            if (!URL.endsWith("/")) {
                URL = URL + "/";
            }

            StringInvRequest stringInvRequest = new StringInvRequest(Request.Method.POST,
                    URL + "VerificationReports", params, submitItemsRespListener);

            /* Add your Requests to the RequestQueue to execute */
            AppSingleton.getInstance(InvApplication.getAppContext()).addToRequestQueue(stringInvRequest);

        } else {
        }

    }

    @Override
    public void startTimeout(int timeoutType) {
        this.progressVerSum.setVisibility(View.INVISIBLE);
        VerSummaryActivity.btnVerSumCont.getBackground().setAlpha(255);
        Intent intentTimeout = new Intent(this, LoginActivity.class);
        intentTimeout.putExtra("TIMEOUTFROM", timeoutFrom);
        startActivityForResult(intentTimeout, timeoutType);
    }

}
