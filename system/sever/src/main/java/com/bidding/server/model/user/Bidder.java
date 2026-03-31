package com.bidding.server.model.user;

public class Bidder extends User {


    public Bidder() {
        super();
    }

    public Bidder(String id, String username, String email, String passwordHash, String fullName) {
        super(id, username, email, passwordHash, fullName, UserRole.BIDDER);
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }


    @Override
    public void printInfo() {
        super.printInfo();
    }

    @Override
    public void validate() {

    }
}
