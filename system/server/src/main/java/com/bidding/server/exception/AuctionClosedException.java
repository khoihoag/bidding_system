package com.bidding.server.exception;

public class AuctionClosedException extends AuctionException{
    public AuctionClosedException(String message){
        super(message);
    }
}
