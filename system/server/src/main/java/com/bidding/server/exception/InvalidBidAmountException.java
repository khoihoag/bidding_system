package com.bidding.server.exception;

public class InvalidBidAmountException extends AuctionException{
    public InvalidBidAmountException(String message){
        super(message);
    }
}
