package com.bidding.server.events;


public interface AuctionObserver {
    // Định danh duy nhất của Client/Session
    String getObserverId();

    // Cơ chế quan trọng: Kiểm tra xem kết nối WebSocket/Client còn sống không?
    boolean isAlive();

    void onBidPlaced(AuctionEvent event);
    void onAuctionClosed(AuctionEvent event);
    void onPriceChanged(AuctionEvent event);
}   