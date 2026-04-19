package com.bidding.server.events;

public interface AuctionObserver {
    void onBidPlaced(AuctionEvent event);
    void onAuctionClosed(AuctionEvent event);
    void onPriceChanged(AuctionEvent event);

    // THÊM DÒNG NÀY: Để hóng hớt khi có phiên mới lên sàn
    void onAuctionStarted(AuctionEvent event);
}