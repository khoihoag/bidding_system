package com.bidding.server.exception;

public class ItemAlreadyInAuctionException extends Exception {
    public ItemAlreadyInAuctionException(String message) {
        super(message);
    }
}
