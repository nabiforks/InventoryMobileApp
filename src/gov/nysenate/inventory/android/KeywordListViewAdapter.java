package gov.nysenate.inventory.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class KeywordListViewAdapter extends ArrayAdapter<String> implements OnItemDoubleTapListener
{

    Context context;
    List<String> items;
    List<String> originalKeywordList = null;
    ClearableEditText etKeywordCurrent = null;
    public List<ClearableEditText> etKeywordFields = new ArrayList<ClearableEditText>();
    NewInvDialog newInvDialog = null;
    int rowSelected = -1;

    
    public KeywordListViewAdapter(Context context, NewInvDialog newInvDialog, int resourceId,
            List<String> items) {
        super(context, resourceId, items);
        this.context = context;
        this.items = items;
        originalKeywordList = new ArrayList<String>();
        for (int x=0;x<items.size();x++) {
            this.originalKeywordList.add(items.get(x));
        }
        this.newInvDialog = newInvDialog;
        //System.out.println("COMMODITY LIST ITEMS SIZE:" + items.size());
    }
    
    
    /* private view holder class */
    private class ViewHolder
    {
        LinearLayout rlkeywordlistrow;
        ClearableEditText etKeyword;
        TextView tvKeywordCnt;
        Button btnDeleteKeyword;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        final String rowItem = items.get(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.row_keyword, null);
            holder = new ViewHolder();
            holder.rlkeywordlistrow = (LinearLayout) convertView
                    .findViewById(R.id.rlkeywordlistrow);
            holder.etKeyword = (ClearableEditText) convertView
                    .findViewById(R.id.etKeyword);
            holder.tvKeywordCnt = (TextView) convertView.findViewById(R.id.tvKeywordCnt);
            holder.btnDeleteKeyword = (Button)convertView.findViewById(R.id.btnDeleteKeyword);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        holder.etKeyword.setText(rowItem);
        if (this.originalKeyword(rowItem)>-1) {
            int wordRowCount = newInvDialog.adapter.wordRowCount(rowItem);
            if (wordRowCount==0) {
                holder.tvKeywordCnt.setText(Html.fromHtml("<font color='red'><b>("+wordRowCount+")</b></font>"));
            }
            else {
                holder.tvKeywordCnt.setText(Html.fromHtml("<font color='#005500'><b>("+wordRowCount+")</b></font>"));
            }
            
        }
        else {
            holder.tvKeywordCnt.setText("(***)");
        }
        
        
        final EditText currentEtKeyword = holder.etKeyword;
        final TextView currentTvKeywordCnt = holder.tvKeywordCnt;
        final int currentPosition = position;
        
        OnClickListener l = new OnClickListener()
        {
            @Override
            public void onClick(View v) {
                 //Log.i("DELETEKEYWORD", rowItem);
                 //removeKeyword(rowItem);
                removeRow(currentPosition);
            }
        };
        
        TextWatcher filterTextWatcher = new TextWatcher()
        {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentTvKeywordCnt.setText("(***)");
                items.set(currentPosition, currentEtKeyword.getText().toString());
                rowSelected = currentPosition;
                //notifyDataSetChanged();
            }
        };
        
        
        if (currentEtKeyword!=null) {
            currentEtKeyword.addTextChangedListener(filterTextWatcher);
        }
        
        if (holder.btnDeleteKeyword!=null) {
            holder.btnDeleteKeyword.setOnClickListener(l);
        }

        
        etKeywordCurrent = holder.etKeyword;
        
        if (etKeywordFields.size()-1<position) {
            etKeywordFields.add(holder.etKeyword);
        }
        else {
            etKeywordFields.set(position, holder.etKeyword);
        }
       
        return convertView;
    }
    
    public String getKeywordAt(int y) {
        return items.get(y);
    }

    public boolean removeRow(int row) {
        boolean rowRemoved = false;
        
        if (items!=null && items.size()>1 && row<items.size()) {
            this.items.remove(row);
            this.setNotifyOnChange(true);
            this.notifyDataSetChanged();
            rowRemoved = true;
        }
        else if (items!=null && items.size()==1) {
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, "At least one Keyword row must exist.", duration);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
        
        return rowRemoved;
    }
    
    
    public int removeKeyword(String keyword) {
        int itemsRemoved = 0;
        this.setNotifyOnChange(true);
        if (this.items != null) {
            for (int x = this.items.size() - 1; x > -1; x--) {
                if (this.items.get(x).equals(keyword)) {
                    this.items.remove(x);
                    itemsRemoved++;
                }
            }
        }
        if (itemsRemoved>0) {
            this.notifyDataSetChanged();
        }
        return itemsRemoved;
    }
    
    public int originalKeyword(String keyword) {
        for (int x=0;x<originalKeywordList.size();x++) {
            if (originalKeywordList.get(x).equalsIgnoreCase(keyword)) {
                return x;
            }
        }
        return -1;
    }    
    
   public int findBlankKeyword(){
        for (int x=0;x<this.items.size();x++) {
            if (this.items.get(x).trim().length()==0) {
                return x;
            }
        }
        return -1;
   }
   
   public int getCurPosition(EditText etKeyword) {
       for (int x=0;x<etKeywordFields.size();x++) {
           if (etKeyword==etKeywordFields.get(x)) {
               return x;
           }
       }
       
       return -1;
   }
    
   public int addRow() {
       int blankRow = findBlankKeyword();
       if (blankRow==-1) {
           this.items.add(""); // Add a Row with a blank value
           this.notifyDataSetChanged();
           return this.items.size()-1;
       }
       else {
           
           int duration = Toast.LENGTH_SHORT;

           Toast toast = Toast.makeText(context, "A blank keyword row was already added.", duration);
           toast.setGravity(Gravity.CENTER, 0, 0);
           toast.show();
           return blankRow;
       }
   }
    
   public String toString() {
       StringBuffer sb = new StringBuffer();
       boolean keywordsAdded = false;
       for (int x=0;x<items.size();x++) {
           if (items.get(x).trim().length()>0) {
               if (keywordsAdded) {
                   sb.append(",");
               }
               sb.append(items.get(x).trim());
               keywordsAdded = true;
           }
      }
       return sb.toString();
   }
   
   public void fromString(String keywords) {
       String[] keywordsList = keywords.split(",");
       items = new ArrayList<String>();
       for (int x=0;x<keywordsList.length;x++) {
           String currentKeyword = keywordsList[x].trim();
           if (currentKeyword.length()>0) {
               items.add(currentKeyword);
           }
       }
       this.notifyDataSetChanged();
   }
  
   @Override
   public void OnDoubleTap(AdapterView parent, View view, int position, long id) {
        System.out.println("Double Clicked on "+position+": "+items.get(position));
   }

   @Override
   public void OnSingleTap(AdapterView parent, View view, int position, long id) {
       // Do nothing on Single Tap (for now)
   }
  
   
   public void returnToSelectedRow() {
       if (rowSelected==-1) {
           rowSelected = 0;
       }
       goRow(rowSelected);       
   }
   
   public void goRow(int row) {
       if (etKeywordFields==null||etKeywordFields.size()==0) {
           return;
       }
       else if (etKeywordFields.size()-1<row) {
           etKeywordFields.get(etKeywordFields.size()-1).requestFocus();
       }
       else {
           etKeywordFields.get(row).requestFocus();
       }
   }

}