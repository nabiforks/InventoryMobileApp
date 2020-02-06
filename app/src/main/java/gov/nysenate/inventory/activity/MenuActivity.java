package gov.nysenate.inventory.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import gov.nysenate.inventory.activity.verification.Verification;
import gov.nysenate.inventory.adapter.CustomListViewAdapter;
import gov.nysenate.inventory.android.InvApplication;
import gov.nysenate.inventory.android.R;
import gov.nysenate.inventory.model.DBAdapter;
import gov.nysenate.inventory.model.RowItem;
import gov.nysenate.inventory.util.Toasty;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends SenateActivity implements OnItemClickListener
{
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private ListView mList;

    public String res = null;

    String URL = ""; // this will be initialized once in onCreate() and used for
    // all server calls.

    public static String[] titles;

    public static String[] descriptions = new String[] {
            "Scan an item and show information",
            "Perform Inventory Verification for a Senate Location",
            "Move Items from one location to another", "Logout of this UserID" };

    public static Integer[] images;

    public static DBAdapter db;

    ListView listView;
    List<RowItem> rowItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        registerBaseActivityReceiver();

        // Does user have access to Verification
        InvApplication app = ((InvApplication)getApplicationContext());
        int cdseclevel = app.getCdseclevel();
        if (cdseclevel == 1) {
            titles = new String[]{ "Search", "Verification", "Move Items", "Inventory Removal", "Logout" };
            images = new Integer[]{ R.drawable.ssearch, R.drawable.sverify, R.drawable.smove, R.drawable.removalrequest, R.drawable.slogout };
        } else {
            titles = new String[]{ "Search", "Move Items", "Inventory Removal", "Logout" };
            images = new Integer[]{ R.drawable.ssearch, R.drawable.smove, R.drawable.removalrequest, R.drawable.slogout };
        }

        rowItems = new ArrayList<RowItem>();
        for (int i = 0; i < titles.length; i++) {
            RowItem item = new RowItem(images[i], titles[i]);
            rowItems.add(item);
        }

        listView = (ListView) findViewById(R.id.list);
        CustomListViewAdapter adapter = new CustomListViewAdapter(this,
                R.layout.list_item, rowItems);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        // Setup Local Database
        db = new DBAdapter(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (checkServerResponse(true) != OK) {
            return;
        }

        String selection = rowItems.get(position).getTitle();
        if (selection.equalsIgnoreCase("Search")) {
            this.search(view);
        } else if (selection.equalsIgnoreCase("Verification")) {
            this.verify(view);
        } else if (selection.equalsIgnoreCase("Move Items")) {
            this.addItem(view);
        } else if (selection.equalsIgnoreCase("Logout")) {
            this.logout(view);
        } else if (selection.equalsIgnoreCase("Inventory Removal")) {
            inventoryRemoval();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        case android.R.id.home:
            // 1. Instantiate an AlertDialog.Builder with its constructor
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // 2. Chain together various setter methods to set the dialog
            // characteristics
            builder.setTitle(Html
                    .fromHtml("<font color='#000055'>Log Out</font>"));
            builder.setMessage("Do you really want to log out?");
            // Add the buttons
            builder.setPositiveButton(Html.fromHtml("<b>Yes</b>"),
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            backToParent();
                        }
                    });
            builder.setNegativeButton(Html.fromHtml("<b>No</b>"),
                    new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });

            // 3. Get the AlertDialog from create()
            AlertDialog dialog = builder.create();
            dialog.show();

            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    public void backToParent() {
        Toasty.displayCenteredMessage(this, "Logging Out", Toast.LENGTH_SHORT);
        NavUtils.navigateUpFromSameTask(this);
        finish();
        overridePendingTransition(R.anim.in_left, R.anim.out_right);
    }

    private void inventoryRemoval() {
        Intent intent = new Intent(this, InventoryRemovalMenu.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in_right, R.anim.out_left);
    }

    public void search(View view) {

        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in_right, R.anim.out_left);
        // overridePendingTransition(R.anim.slide_in_left,
        // R.anim.slide_out_left);

    }

    public void addItem(View view) {
        Intent intent = new Intent(this, Move.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in_right, R.anim.out_left);

        // overridePendingTransition(R.anim.slide_in_left,
        // R.anim.slide_out_left);
    }

    public void verify(View view) {
        Intent intent = new Intent(this, Verification.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in_right, R.anim.out_left);
        // overridePendingTransition(R.anim.slide_in_left,
        // R.anim.slide_out_left);

    }

    public void noServerResponse() {
        noServerResponse(null);
    }

    public void noServerResponse(final String barcode_num) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        StringBuilder title = new StringBuilder();
        title.append("<font color='#000055'>");
        if (barcode_num != null && barcode_num.trim().length() > 0) {
            title.append("Barcode#: ");
            title.append(barcode_num);
            title.append(" ");
        }
        title.append("NO SERVER RESPONSE");
        title.append("</font>");

        StringBuilder msg = new StringBuilder();
        msg.append("!!ERROR: There was <font color='RED'><b>NO SERVER RESPONSE</b></font>.");
        if (barcode_num != null && barcode_num.trim().length() > 0) {
            msg.append(" Senate Tag#:<b>");
            msg.append(barcode_num);
            msg.append("</b> will be <b>IGNORED</b>.");
        }
        msg.append("<br/> Please contact STS/BAC.");

        // set title
        alertDialogBuilder.setTitle(Html.fromHtml(title.toString()));

        // set dialog message
        alertDialogBuilder.setMessage(Html.fromHtml(msg.toString()))
                .setCancelable(false)
                .setPositiveButton(Html.fromHtml("<b>Ok</b>"), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        if (barcode_num != null
                                && barcode_num.trim().length() > 0) {
                            Context context = getApplicationContext();

                            CharSequence text = "Senate Tag#: " + barcode_num
                                    + " was NOT added";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text,
                                    duration);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        };
                        dialog.dismiss();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public String stripHtml(String html) {
        return Html.fromHtml(html).toString();
    }

    /*
     * 
     * Testing Code above
     */

    public void logout(View view) {
        final Intent intent = new Intent(this, LoginActivity.class);

        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog
        // characteristics
        builder.setTitle(Html.fromHtml("<font color='#000055'>Log out</font>"));
        builder.setMessage("Do you really want to log out?");

        // Add the buttons
        builder.setPositiveButton(Html.fromHtml("<b>Yes</b>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.in_left, R.anim.out_right);
                Toast toast = Toast.makeText(getApplicationContext(),
                        "logging out", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
        builder.setNegativeButton(Html.fromHtml("<b>No</b>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog
        // characteristics
        builder.setTitle(Html.fromHtml("<font color='#000055'>Log Out</font>"));
        builder.setMessage("Do you really want to log out?");
        // Add the buttons
        builder.setPositiveButton(Html.fromHtml("<b>Yes</b>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                backToParent();
            }
        });
        builder.setNegativeButton(Html.fromHtml("<b>No</b>"), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

}
