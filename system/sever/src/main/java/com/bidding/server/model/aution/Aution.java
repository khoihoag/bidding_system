package com.bidding.server.model.aution;

import com.bidding.server.model.entity.Entity;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.Bidder;
import com.bidding.server.model.user.User;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Aution extends Entity {
    private Item item;
    private AutionStatus status;
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;
    private AtomicReference<Double> currentPrice;
    private Bidder currentWinner;
    private CopyOnWriteArrayList<LocalDateTime> bidHistory;
    private CopyOnWriteArrayList<User> observers;
    private int antiSnipingSeconds;
    private int ectensionSeconds;
    private ReentrantLock lock;

    @Override
    public void validate() {

    }

    @Override
    public void printInfo() {

    }
}