package com.bidding.server.exception;

public class ItemAlreadyInAuctionException extends AuctionException {
    public ItemAlreadyInAuctionException(String message) {
        super(message);
    }
}
