package gov.nysenate.inventory.android;

import gov.nysenate.inventory.model.Location;
import gov.nysenate.inventory.model.Pickup;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class Pickup3 extends SenateActivity
{
    ArrayList<InvItem> scannedBarcodeNumbers = new ArrayList<InvItem>();
    public String res = null;
    private SignatureView sign;
    private byte[] imageInByte = {};
    public ArrayList<Employee> employeeHiddenList = new ArrayList<Employee>();
    public ArrayList<String> employeeNameList = new ArrayList<String>();
    ClearableAutoCompleteTextView employeeNamesView;
    int nuxrefem = -1;
    String pickupRequestTaskType = "";
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    ClearableEditText commentsEditText;
    private String DECOMMENTS = null;
    public String status = null;
    String URL;
    static Button continueBtn;
    static Button cancelBtn;
    static Button btnPickup3ClrSig;

    public TextView pickupCountTV;
    public TextView tvOriginPickup3;
    public TextView tvDestinationPickup3;

    public static ProgressBar progBarPickup3;
    boolean positiveButtonPressed = false;
    public final int CONTINUEBUTTON_TIMEOUT = 101,
            POSITIVEDIALOG_TIMEOUT = 102, KEEPALIVE_TIMEOUT = 103,
            EMPLOYEELIST_TIMEOUT = 104;
    public String timeoutFrom = "pickup3";
    private Pickup pickup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup3);
        registerBaseActivityReceiver();

        sign = (SignatureView) findViewById(R.id.blsignImageView);
        sign.setMinDimensions(200, 100);
        commentsEditText = (ClearableEditText) findViewById(R.id.pickupCommentsEditText);
        commentsEditText
                .setClearMsg("Do you want to clear the Pickup Comments?");
        commentsEditText.showClearMsg(true);

        ListView ListViewTab1 = (ListView) findViewById(R.id.listView1);
        pickup = (Pickup) getIntent().getSerializableExtra("pickup");
        scannedBarcodeNumbers = getInvItemArrayList(pickup.getPickupItems());
        tvOriginPickup3 = (TextView) findViewById(R.id.tv_origin_pickup3);
        tvDestinationPickup3 = (TextView) findViewById(R.id.tv_destination_pickup3);
        tvOriginPickup3.setText(pickup.getOrigin().getAddressLine1());
        tvDestinationPickup3.setText(pickup.getDestination().getAddressLine1());
        pickupCountTV = (TextView) findViewById(R.id.tv_count_pickup3);
        pickupCountTV.setText(Integer.toString(pickup.getPickupItems().size()));

        Adapter listAdapter1 = new InvListViewAdapter(this,
                R.layout.invlist_item, scannedBarcodeNumbers);

        // Set the ArrayAdapter as the ListView's adapter.
        ListViewTab1.setAdapter((ListAdapter) listAdapter1);

        // Brian code starts
        Pickup2Activity.continueBtn.getBackground().setAlpha(255);

        continueBtn = (Button) findViewById(R.id.btnPickup3Cont);
        continueBtn.getBackground().setAlpha(255);
        cancelBtn = (Button) findViewById(R.id.btnPickup3Back);
        cancelBtn.getBackground().setAlpha(255);
        btnPickup3ClrSig = (Button) findViewById(R.id.btnPickup3ClrSig);
        btnPickup3ClrSig.getBackground().setAlpha(255);
        employeeNamesView = (ClearableAutoCompleteTextView) findViewById(R.id.naemployee);
        employeeNamesView
                .setClearMsg("Do you want to clear the name of the signer?");
        employeeNamesView.showClearMsg(true);
        employeeNamesView
                .setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(
                                employeeNamesView.getWindowToken(), 0);
                    }
                });

        // Setup ProgressBar
        progBarPickup3 = (ProgressBar) findViewById(R.id.progBarPickup3);

        getEmployeeList();

        // code for textwatcher
        // for origin location code
        // loc_code = (EditText) findViewById(R.id.editText1);
        // loc_code.addTextChangedListener(filterTextWatcher);

        // Commented out by Brian Heitner, found by Kevin Caseiras
        // I believe this code is left over code that doesn't ever fire..
        // Leaving commented code in just in case I am wrong.
        // /

        /*
         * naemployeeView.setOnItemSelectedListener(new OnItemSelectedListener()
         * {
         * 
         * @Override public void onItemSelected(AdapterView<?> arg0, View arg1,
         * int arg2, long arg3) { String employeeSelected =
         * naemployeeView.getText().toString(); int employeeFoundAt =
         * findEmployee(employeeSelected);
         * System.out.println("EMPLOYEE SELECTED:" + employeeSelected +
         * " FOUND AT:" + employeeFoundAt); if (employeeSelected == null ||
         * employeeSelected.length() == 0) { nuxrefem = -1; Context context =
         * getApplicationContext(); int duration = Toast.LENGTH_SHORT;
         * 
         * Toast toast = Toast.makeText(context, "No Employee entered.", 3000);
         * toast.setGravity(Gravity.CENTER, 0, 0); toast.show(); } else if
         * (employeeFoundAt == -1) { nuxrefem = -1; Context context =
         * getApplicationContext(); int duration = Toast.LENGTH_SHORT;
         * 
         * Toast toast = Toast.makeText(context, "Employee not found.", 3000);
         * toast.setGravity(Gravity.CENTER, 0, 0); toast.show(); } else {
         * nuxrefem = employeeHiddenList.get(employeeFoundAt)
         * .getEmployeeXref(); Context context = getApplicationContext(); int
         * duration = Toast.LENGTH_SHORT; Toast toast = Toast.makeText(context,
         * "Employee xref#:" + nuxrefem + " Name:" +
         * employeeHiddenList.get(employeeFoundAt) .getEmployeeName(), 3000);
         * toast.setGravity(Gravity.CENTER, 0, 0); toast.show(); } }
         * 
         * @Override public void onNothingSelected(AdapterView<?> arg0) {
         * nuxrefem = -1; } });
         */
    }

    @Override
    protected void onResume() {
        super.onResume();
        positiveButtonPressed = false;
        continueBtn = (Button) findViewById(R.id.btnPickup3Cont);
        continueBtn.getBackground().setAlpha(255);
        cancelBtn = (Button) findViewById(R.id.btnPickup3Back);
        cancelBtn.getBackground().setAlpha(255);
        btnPickup3ClrSig = (Button) findViewById(R.id.btnPickup3ClrSig);
        btnPickup3ClrSig.getBackground().setAlpha(255);
        if (progBarPickup3 ==null) {
            progBarPickup3 = (ProgressBar) this.findViewById(R.id.progBarPickup3);
        }            
        progBarPickup3.getBackground().setAlpha(255);         
    }

    public int findEmployee(String employeeName) {
        for (int x = 0; x < employeeHiddenList.size(); x++) {
            if (employeeName
                    .equals(employeeHiddenList.get(x).getEmployeeName())) {
                return x;
            }
        }
        return -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_pickup3, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
            Toast toast = Toast.makeText(getApplicationContext(), "Going Back",
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            backButton(this.getCurrentFocus());
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    public void startCommentsSpeech(View view) {
        if (view.getId() == R.id.pickupCommentsSpeechButton) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                    "Pickup Comments Speech");
            startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
        }
    }

    /**
     * Handle the results from the recognition activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onActivityResult", "requestCode:" + requestCode + " resultCode:"
                + resultCode);

        switch (requestCode) {
        case EMPLOYEELIST_TIMEOUT:
            if (resultCode == RESULT_OK) {
                getEmployeeList();
            }
            break;
        case POSITIVEDIALOG_TIMEOUT:
            new Timer().schedule(new TimerTask()
            {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(
                            employeeNamesView.getWindowToken(), 0);
                }
            }, 50);
            break;
        case KEEPALIVE_TIMEOUT:
            // Log.i("onActivityResult", "KEEPALIVE_TIMEOUT");
            new Timer().schedule(new TimerTask()
            {
                @Override
                public void run() {
                    // Log.i("onActivityResult",
                    // "KEEPALIVE_TIMEOUT Hide Keyboard");
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(
                            employeeNamesView.getWindowToken(), 0);
                }
            }, 50);
            break;

        case VOICE_RECOGNITION_REQUEST_CODE:
            if (resultCode == RESULT_OK) {
                // Fill the list view with the strings the recognizer thought it
                // could have heard
                ArrayList<String> matches = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                commentsEditText.setText(matches.get(0));
            }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void continueButton(View view) {
        if (checkServerResponse(true) == OK) {

            String employeePicked = employeeNamesView.getEditableText().toString();
            if (employeePicked.trim().length() > 0) {
                int foundEmployee = this.findEmployee(employeePicked);

                if (foundEmployee < 0) {
                    nuxrefem = -1;
                } else {
                    nuxrefem = this.employeeHiddenList.get(foundEmployee)
                            .getEmployeeXref();
                }
            } else {
                nuxrefem = -1;
            }

            if (nuxrefem < 0) {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                if (employeeNamesView.getEditableText().toString().trim().length() > 0) {
                    Toast toast = Toast.makeText(context,
                            "!!ERROR: No xref# found for employee", duration);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    Toast toast = Toast
                            .makeText(
                                    context,
                                    "!!ERROR: You must first pick an employee name for the signature.",
                                    3000);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

                return;

            }

            if (!sign.isSigned()) {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context,
                        "!!ERROR: Employee must also sign within the Red box.",
                        3000);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }

            Log.i("continueButton",
                    "Check for Session by using KeepSessionAlive");

            if (!keepAlive()) {
                return;
            }

            AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
            confirmDialog.setTitle(Html.fromHtml("<font color='#000055'>Pickup Confirmation</font>"));
            confirmDialog.setMessage("Are you sure you want to pickup these "
                    + scannedBarcodeNumbers.size() + " items?");
            confirmDialog.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            /*
                             * Prevent Multiple clicks on button, which will
                             * cause issues witn the database inserting multiple
                             * nuxrpds for the same pickup.
                             */

                            if (positiveButtonPressed) {
                                /*
                                 * Context context = getApplicationContext();
                                 * int duration = Toast.LENGTH_SHORT;
                                 * 
                                 * Toast toast = Toast.makeText(context,
                                 * "Button was already been pressed.",
                                 * Toast.LENGTH_SHORT);
                                 * toast.setGravity(Gravity.CENTER, 0, 0);
                                 * toast.show();
                                 */

                            } else {
                                positiveButtonPressed = true;
                                positiveDialog();
                            }
                        }
                    });

            confirmDialog.setNegativeButton("No",
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue in same activity.
                        }
                    });

            AlertDialog dialog = confirmDialog.create();
            dialog.show();

        }
    }

    public void noServerResponse() {
        progBarPickup3.setVisibility(ProgressBar.INVISIBLE);
        continueBtn.getBackground().setAlpha(255);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(Html.fromHtml("<font color='#000055'>NO SERVER RESPONSE</font>"));

        // set dialog message
        alertDialogBuilder
                .setMessage(
                        Html.fromHtml("!!ERROR: There was <font color='RED'><b>NO SERVER RESPONSE</b></font>. <br/> Please contact STS/BAC."))
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener()
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

                        dialog.dismiss();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void backButton(View view) {
        if (checkServerResponse(true) == OK) {
            super.onBackPressed();
        }
        /*
         * float alpha = 0.45f; AlphaAnimation alphaUp = new
         * AlphaAnimation(alpha, alpha); alphaUp.setFillAfter(true);
         * btnPickup3Back.startAnimation(alphaUp); Intent intent = new
         * Intent(this, Pickup2Activity.class); startActivity(intent);
         * overridePendingTransition(R.anim.in_left, R.anim.out_right);
         */
    }

    public void clearSignatureButton(View view) {
        btnPickup3ClrSig.getBackground().setAlpha(45);
        Bitmap clearedSignature = BitmapFactory.decodeResource(getResources(),
                R.drawable.simplethinborder);
        if (clearedSignature == null) {
            Log.i("ClearSig", "Signature drawable was NULL");
        } else {
            Log.i("ClearSig", "Signature size:" + clearedSignature.getWidth()
                    + " x " + clearedSignature.getHeight());
        }
        sign.clearSignature();
        btnPickup3ClrSig.getBackground().setAlpha(255);
    }

    public ArrayList<String> getJSONArrayList(ArrayList<InvItem> invList) {
        ArrayList<String> returnArray = new ArrayList<String>();
        if (invList != null) {
            for (int x = 0; x < invList.size(); x++) {
                returnArray.add(invList.get(x).toJSON());
            }
        }

        return returnArray;
    }

    public ArrayList<InvItem> getInvItemArrayList(ArrayList<String> invList) {
        ArrayList<InvItem> returnArray = new ArrayList<InvItem>();
        if (invList != null) {
            for (int x = 0; x < invList.size(); x++) {
                String curInvJson = invList.get(x);
                InvItem currentInvItem = new InvItem();
                currentInvItem.parseJSON(curInvJson);
                returnArray.add(currentInvItem);
            }
        }

        return returnArray;
    }

   public Bitmap setBackgroundColor(Bitmap image, int backgroundColor) {
        Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(backgroundColor);
        canvas.drawBitmap(image, 0, 0, null);
        return newBitmap;
   }
    
    class pickupRequestTask extends AsyncTask<String, String, String>
    {

        @Override
        protected String doInBackground(String... uri) {
            // First Upload the Signature and get the nuxsign from the Server
            if (pickupRequestTaskType.equalsIgnoreCase("Pickup")) {

                // Scale the Image

                String NUXRRELSIGN = "";

                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                Bitmap bitmap = sign.getImage();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200,
                        40, true);

                for (int x = 0; x < scaledBitmap.getWidth(); x++) {
                    for (int y = 0; y < scaledBitmap.getHeight(); y++) {
                        String strColor = String.format("#%06X",
                                0xFFFFFF & scaledBitmap.getPixel(x, y));
                        if (strColor.equals("#000000")
                                || scaledBitmap.getPixel(x, y) == Color.TRANSPARENT) {
                            // System.out.println("********"+x+" x "+y+" SETTING COLOR TO WHITE");
                            scaledBitmap.setPixel(x, y, Color.WHITE);
                        }
                    }
                }
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bs);

                scaledBitmap = setBackgroundColor(scaledBitmap, Color.WHITE);
                imageInByte = bs.toByteArray();
                String responseString = "";
                try {
                    // Post the Image to the Web Server

                    StringBuilder urls = new StringBuilder();
                    urls.append(uri[0].trim());
                    if (uri[0].indexOf("?") > -1) {
                        if (!uri[0].trim().endsWith("?")) {
                            urls.append("&");
                        }
                    } else {
                        urls.append("?");
                    }
                    urls.append("userFallback=");
                    urls.append(LoginActivity.nauser);

                    URL url = new URL(urls.toString());
                    HttpClient httpClient = LoginActivity.httpClient;

                    if (httpClient == null) {
                        Log.i(pickupRequestTask.class.getName(),
                                "MainActivity.httpClient was null so it is being reset");
                        LoginActivity.httpClient = new DefaultHttpClient();
                        httpClient = LoginActivity.httpClient;
                    }

                    HttpContext localContext = new BasicHttpContext();
                    MultipartEntity entity = new MultipartEntity(
                            HttpMultipartMode.BROWSER_COMPATIBLE);

                    HttpPost httpPost = new HttpPost(urls.toString());
                    entity.addPart("Signature", new ByteArrayBody(imageInByte,
                            "temp.jpg"));
                    httpPost.setEntity(entity);

                    /*
                     * HttpURLConnection conn = (HttpURLConnection) url
                     * .openConnection(); // Set connection parameters.
                     * conn.setDoInput(true); conn.setDoOutput(true);
                     * conn.setUseCaches(false);
                     * 
                     * // Set content type to PNG
                     * conn.setRequestProperty("Content-Type", "image/jpg");
                     * OutputStream outputStream = conn.getOutputStream();
                     * OutputStream out = outputStream; // Write out the bytes
                     * of the content string to the stream.
                     * out.write(imageInByte); out.flush(); out.close(); // Read
                     * response from the input stream. BufferedReader in = new
                     * BufferedReader( new
                     * InputStreamReader(conn.getInputStream())); String temp;
                     * while ((temp = in.readLine()) != null) { responseString
                     * += temp + "\n"; } temp = null; in.close();
                     */

                    // Get Server Response to the posted Image

                    HttpResponse response = httpClient.execute(httpPost,
                            localContext);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getEntity()
                                    .getContent(), "UTF-8"));
                    responseString = reader.readLine();
                    System.out.println("***Image Server response:\n'"
                            + responseString + "'");
                    int nuxrsignLoc = responseString.indexOf("NUXRSIGN:");
                    if (nuxrsignLoc > -1) {
                        NUXRRELSIGN = responseString.substring(nuxrsignLoc + 9)
                                .replaceAll("\r", "").replaceAll("\n", "");
                    } else {
                        NUXRRELSIGN = responseString.replaceAll("\r", "")
                                .replaceAll("\n", "");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Then post the rest of the information along with the NUXRSIGN

                HttpClient httpclient = LoginActivity.httpClient;
                HttpResponse response;
                responseString = null;
                try {

                    String pickupURL = uri[1] + "&NUXRRELSIGN=" + NUXRRELSIGN
                            + "&DECOMMENTS=" + DECOMMENTS + "&userFallback="
                            + LoginActivity.nauser;
                    System.out.println("pickupURL:" + pickupURL);
                    response = httpclient.execute(new HttpGet(pickupURL));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        out.close();
                        responseString = out.toString();
                    } else {
                        // Closes the connection.
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (ClientProtocolException e) {
                    // TODO Handle problems..
                } catch (ConnectTimeoutException e) {
                    return "***WARNING: Server Connection timeout";
                    //Toast.makeText(getApplicationContext(), "Server Connection timeout", Toast.LENGTH_LONG).show();
                    //Log.e("CONN TIMEOUT", e.toString());
                } catch (SocketTimeoutException e) {
                    return "***WARNING: Server Socket timeout";
                    //Toast.makeText(getApplicationContext(), "Server timeout", Toast.LENGTH_LONG).show();
                    //Log.e("SOCK TIMEOUT", e.toString());
                } catch (IOException e) {
                    // TODO Handle problems..
                }
                res = responseString;
                return responseString;
            } else if (pickupRequestTaskType.equalsIgnoreCase("EmployeeList")) {
                HttpClient httpclient = LoginActivity.httpClient;
                HttpResponse response;
                String responseString = null;
                try {
                    StringBuilder urls = new StringBuilder();
                    urls.append(uri[0].trim());
                    if (uri[0].indexOf("?") > -1) {
                        if (!uri[0].trim().endsWith("?")) {
                            urls.append("&");
                        }
                    } else {
                        urls.append("?");
                    }
                    urls.append("userFallback=");
                    urls.append(LoginActivity.nauser);
                    response = httpclient.execute(new HttpGet(urls.toString()));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        out.close();
                        responseString = out.toString();
                    } else {
                        // Closes the connection.
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (ClientProtocolException e) {
                    // TODO Handle problems..
            } catch (ConnectTimeoutException e) {
                return "***WARNING: Server Connection timeout";
            //Toast.makeText(getApplicationContext(), "Server Connection timeout", Toast.LENGTH_LONG).show();
            //Log.e("CONN TIMEOUT", e.toString());
            } catch (SocketTimeoutException e) {
                return "***WARNING: Server Socket timeout";
            //Toast.makeText(getApplicationContext(), "Server timeout", Toast.LENGTH_LONG).show();
            //Log.e("SOCK TIMEOUT", e.toString());
            }   catch (IOException e) {
                    // TODO Handle problems..
                }
                res = responseString;
                return responseString;
            } else if (pickupRequestTaskType.equalsIgnoreCase("KeepAlive")) {
                HttpClient httpclient = LoginActivity.httpClient;
                HttpResponse response;
                String responseString = null;
                try {
                    response = httpclient.execute(new HttpGet(uri[0]));
                    StatusLine statusLine = response.getStatusLine();
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        response.getEntity().writeTo(out);
                        out.close();
                        responseString = out.toString();
                    } else {
                        // Closes the connection.
                        response.getEntity().getContent().close();
                        throw new IOException(statusLine.getReasonPhrase());
                    }
                } catch (ConnectTimeoutException e) {
                    return "***WARNING: Server Connection timeout";
                //Toast.makeText(getApplicationContext(), "Server Connection timeout", Toast.LENGTH_LONG).show();
                //Log.e("CONN TIMEOUT", e.toString());
                } catch (SocketTimeoutException e) {
                    return "***WARNING: Server Socket timeout";
                //Toast.makeText(getApplicationContext(), "Server timeout", Toast.LENGTH_LONG).show();
                //Log.e("SOCK TIMEOUT", e.toString());
                }   catch (ClientProtocolException e) {
                    // TODO Handle problems..
                } catch (IOException e) {
                    // TODO Handle problems..
                }
                res = responseString;
                return responseString;

            } else {
                System.out.println("!!ERROR: Invalid requestTypeTask:"
                        + pickupRequestTaskType);
                return "!!ERROR: Invalid requestTypeTask:"
                        + pickupRequestTaskType;
            }
        }
    }

    private void positiveDialog() {
        progBarPickup3.setVisibility(ProgressBar.VISIBLE);
        continueBtn.getBackground().setAlpha(45);
        // new VersummaryActivity().sendJsonString(scannedBarcodeNumbers);
        // String jsonString = null;
        String status = null;
        // JSONArray jsArray = new JSONArray(scannedBarcodeNumbers);

        // String barcodeNum = "";

        // for (int i = 0; i < scannedBarcodeNumbers.size(); i++) {
        // barcodeNum += scannedBarcodeNumbers.get(i).getNusenate() + ",";
        // }

        // Create a JSON string from the arraylist
        /*
         * WORK ON IT LATER (SENDING THE STRING AS JSON) JSONObject jo=new
         * JSONObject();// =jsArray.toJSONObject("number"); try {
         * 
         * //jo.putOpt("barcodes",scannedBarcodeNumbers.toString());
         * jsonString=jsArray.toString(); } catch (Exception e) { // TODO
         * Auto-generated catch block e.printStackTrace(); }
         */

        // call the servlet image upload and return the nuxrsign

        String NAPICKUPBY = LoginActivity.nauser;
        String NUXREFEM = Integer.toString(nuxrefem);
        String NUXRRELSIGN = "1111";
        /*
         * changes VP String
         * RELEASEBY=this.naemployeeView.getText().toString().replace(",", " ");
         * 
         * String NARELEASEBY= URLEncoder.encode(RELEASEBY);
         */
        String NARELEASEBY = null;
        DECOMMENTS = null;
        /*
         * String NADELIVERBY= ""; String NAACCEPTBY = ""; String
         * NUXRACCPTSIGN="";
         */

        try {
            NARELEASEBY = URLEncoder.encode(this.employeeNamesView.getText()
                    .toString(), "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            DECOMMENTS = URLEncoder.encode(this.commentsEditText.getText()
                    .toString(), "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Send it to the server

        // check network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            status = "yes";
            pickupRequestTaskType = "Pickup";
            AsyncTask<String, String, String> resr1;
            try {
                // Get the URL from the properties

                String URL = LoginActivity.properties.get("WEBAPP_BASE_URL")
                        .toString();
                // System.out.println("("+MainActivity.nauser+")");

                // resr1 = new
                // pickupRequestTask().execute(URL+"/ImgUpload?nauser="+MainActivity.nauser+"&nuxrefem="+nuxrefem,
                // URL+"/Pickup?originLocation="+originLocationCode+"&destinationLocation="+destinationLocationCode+"&barcodes="+barcodeNum+"&NAPICKUPBY="+NAPICKUPBY+"&NARELEASEBY="+NARELEASEBY);

                resr1 = new pickupRequestTask().execute(
                        URL + "/ImgUpload?nauser=" + LoginActivity.nauser
                                + "&nuxrefem=" + nuxrefem,
                        URL
                                + "/Pickup?originLocation="
                                + pickup.getOrigin().getCdLoc()
                                + "&destinationLocation="
                                + pickup.getDestination().getCdLoc()
                                + Formatter.generateGetArray("barcode[]",
                                        scannedBarcodeNumbers) + "&NAPICKUPBY="
                                + NAPICKUPBY + "&NARELEASEBY=" + NARELEASEBY
                                + "&cdloctypeto=" + pickup.getDestination().getCdLocType()
                                + "&cdloctypefrm=" + pickup.getOrigin().getCdLocType());

                try {
                    res = null;
                    res = resr1.get().trim().toString();
                    if (res == null) {
                        noServerResponse();
                        return;
                    } else if (res.indexOf("Session timed out") > -1) {
                        startTimeout(POSITIVEDIALOG_TIMEOUT);
                        return;
                    } else if (res.startsWith("***WARNING:")||res.startsWith("!!ERROR:")) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                        // set title
                        alertDialogBuilder.setTitle(Html.fromHtml("<font color='#000055'>"+res.trim()+"</font>"));

                        // set dialog message
                        alertDialogBuilder
                                .setMessage(
                                        Html.fromHtml(res.trim()+"<br/> Continue (Y/N)?"))
                                .setCancelable(false)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // if this button is clicked, just close
                                        // the dialog box and do nothing
                                        returnToMoveMenu();
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("No", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        // if this button is clicked, just close
                                        // the dialog box and do nothing
                                        dialog.dismiss();
                                    }
                                });

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();

                        // show it
                        alertDialog.show();
                        return;
                    }
                    
                } catch (NullPointerException e) {
                    noServerResponse();
                    return;
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            status = "yes1";
        } else {
            // display error
            status = "no";
        }

        // Display Toster
        Context context = getApplicationContext();
        CharSequence text = res;
        if (res.length() == 0) {
            noServerResponse();
            return;
        }

        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        // ===================ends
        // Intent intent = new Intent(this, MenuActivity.class);
        returnToMoveMenu();
    }
    
    public void returnToMoveMenu() {
        // ===================ends
        // Intent intent = new Intent(this, MenuActivity.class);
        Intent intent = new Intent(this, Move.class);
        startActivity(intent);
    }

    @Override
    public void startTimeout(int timeoutType) {
        Intent intentTimeout = new Intent(this, LoginActivity.class);
        intentTimeout.putExtra("TIMEOUTFROM", timeoutFrom);
        startActivityForResult(intentTimeout, timeoutType);
    }

    public boolean keepAlive() {
        // check network connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            status = "yes";

            AsyncTask<String, String, String> resr1;
            try {
                // Get the URL from the properties
                String URL = LoginActivity.properties.get("WEBAPP_BASE_URL")
                        .toString();
                this.pickupRequestTaskType = "KeepAlive";
                resr1 = new pickupRequestTask().execute(URL
                        + "/KeepSessionAlive");

                try {
                    res = null;
                    res = resr1.get().trim().toString();
                    if (res == null) {
                        noServerResponse();
                        return false;
                    } else if (res.indexOf("Session timed out") > -1) {
                        startTimeout(this.KEEPALIVE_TIMEOUT);
                        return false;
                    }

                } catch (NullPointerException e) {
                    noServerResponse();
                    return false;
                }

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            status = "yes1";
        } else {
            // display error
            status = "no";
        }
        return true;

    }

    public void getEmployeeList() {
        // Get the Employee Name List from the Web Service and populate the
        // Employee Name Autocomplete Field with it

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // fetch data
            status = "yes";

            // Get the URL from the properties
            URL = LoginActivity.properties.get("WEBAPP_BASE_URL").toString();
            pickupRequestTaskType = "EmployeeList";
            AsyncTask<String, String, String> resr1 = new pickupRequestTask()
                    .execute(URL + "/EmployeeList");
            try {
                try {
                    res = null;
                    res = resr1.get().trim().toString();
                    if (res == null) {
                        noServerResponse();
                        return;
                    } else if (res.indexOf("Session timed out") > -1) {
                        startTimeout(EMPLOYEELIST_TIMEOUT);
                        return;
                    }
                } catch (NullPointerException e) {
                    noServerResponse();
                    return;
                }
                // code for JSON

                String jsonString = resr1.get().trim().toString();
                JSONArray jsonArray = new JSONArray(jsonString);
                for (int x = 0; x < jsonArray.length(); x++) {
                    JSONObject jo = new JSONObject();
                    jo = jsonArray.getJSONObject(x);
                    Employee currentEmployee = new Employee();
                    currentEmployee.setEmployeeData(jo.getInt("nuxrefem"),
                            jo.getString("naemployee"));
                    employeeHiddenList.add(currentEmployee);
                    employeeNameList.add(jo.getString("naemployee"));
                }

                Collections.sort(employeeNameList);

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        employeeNameList);

                // for origin dest code
                employeeNamesView.setThreshold(1);
                employeeNamesView.setAdapter(adapter);
                // for destination code

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            status = "yes1";
        } else {
            // display error
            status = "no";
        }
        Pickup2Activity.progBarPickup2.setVisibility(View.INVISIBLE);
    }

}
