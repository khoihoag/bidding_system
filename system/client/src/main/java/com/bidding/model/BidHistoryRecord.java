package com.bidding.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model đại diện cho một dòng trong bảng lịch sử đấu giá.
 * Dùng JavaFX Property để TableView có thể observe và tự cập nhật.
 */
public class BidHistoryRecord {

    private final IntegerProperty index;
    private final StringProperty  auctionId;
    private final StringProperty  bidAmount;
    private final StringProperty  bidTime;
    private final StringProperty  bidType;
    private final StringProperty  status;

    public BidHistoryRecord(int index,
                            String auctionId,
                            String bidAmount,
                            String bidTime,
                            String bidType,
                            String status) {
        this.index     = new SimpleIntegerProperty(index);
        this.auctionId = new SimpleStringProperty(auctionId);
        this.bidAmount = new SimpleStringProperty(bidAmount);
        this.bidTime   = new SimpleStringProperty(bidTime);
        this.bidType   = new SimpleStringProperty(bidType);
        this.status    = new SimpleStringProperty(status);
    }

    // ─── Getters (bắt buộc để PropertyValueFactory hoạt động) ────────────────

    public int getIndex()          { return index.get(); }
    public String getAuctionId()   { return auctionId.get(); }
    public String getBidAmount()   { return bidAmount.get(); }
    public String getBidTime()     { return bidTime.get(); }
    public String getBidType()     { return bidType.get(); }
    public String getStatus()      { return status.get(); }

    // ─── Property accessors ───────────────────────────────────────────────────

    public IntegerProperty indexProperty()     { return index; }
    public StringProperty  auctionIdProperty() { return auctionId; }
    public StringProperty  bidAmountProperty() { return bidAmount; }
    public StringProperty  bidTimeProperty()   { return bidTime; }
    public StringProperty  bidTypeProperty()   { return bidType; }
    public StringProperty  statusProperty()    { return status; }
}
