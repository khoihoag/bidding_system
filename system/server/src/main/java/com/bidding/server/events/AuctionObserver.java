package com.bidding.server.events;


public interface AuctionObserver {

    void onBidPlaced(AuctionEvent event);
    void onAuctionClosed(AuctionEvent event);
    void onPriceChanged(AuctionEvent event);
}   