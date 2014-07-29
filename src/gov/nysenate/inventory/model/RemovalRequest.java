package gov.nysenate.inventory.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RemovalRequest
{
    private int transactionNum;
    private List<Item> items;
    private AdjustCode adjustCode;
    private String employee;
    private Date date;
    private String status;

    public RemovalRequest(String employee, Date date) {
        this.employee = employee;
        this.date = date;
        this.items = new ArrayList<Item>();
    }

    public int getTransactionNum() {
        return transactionNum;
    }

    public void setTransactionNum(int transactionNum) {
        this.transactionNum = transactionNum;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void deleteItem(Item item) {
        items.remove(item);
    }

    public AdjustCode getAdjustCode() {
        return adjustCode;
    }

    public void setAdjustCode(AdjustCode adjustCode) {
        this.adjustCode = adjustCode;
    }

    public String getEmployee() {
        return employee;
    }

    public Date getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}