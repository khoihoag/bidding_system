package com.bidding.server.model.auction;

import com.bidding.server.events.AuctionEvent;
import com.bidding.server.events.AuctionObserver;
import com.bidding.server.model.entity.Entity;
import com.bidding.server.model.enums.AuctionStatus;
import com.bidding.server.model.item.Item;
import com.bidding.server.model.user.Bidder;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Item item;
    private AuctionStatus status;
    private volatile LocalDateTime startTime;
    private volatile LocalDateTime endTime;
    private AtomicReference<Double> currentPrice;
    private Bidder currentWinner;
    private CopyOnWriteArrayList<LocalDateTime> bidHistory;
    private CopyOnWriteArrayList<AuctionObserver> observers;
    private int antiSnipingSeconds;
    private int ectensionSeconds;
    private ReentrantLock lock;

    //đặt giá
    public synchronized void placeBid(Bidder bidder, double amount){

    }
    // chuyển trạng thái phiên giao dịch
    public void start(){

    }
    public void end(){

    }

    // Gia hạn phiên
    public void extendTimt(int second){

    }

    // Quản lý các observer
    public void addObserver(AuctionObserver observer) {
        if (observer != null && !this.observers.contains(observer)) {
            this.observers.add(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        if (observer != null) {
            this.observers.remove(observer);
        }
    }

    // Thông báo AutionEvent đến tất cả observer
    private void notifyObserver(AuctionEvent event){

    }

    // Kiểm rta giá đặt hợp lệ
    private void isValidBid(double amount){

    }

    private void checkAntiSnipe(){

    }

    @Override
    public void validate() {

    }

    @Override
    public void printInfo() {

    }

    // --- Getter, Setter & Utility Methods ---
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public Bidder getCurrentWinner() {
        return currentWinner;
    }

    public AtomicReference<Double> getCurrentPrice() {
        return currentPrice;
    }

    // Kiểm tra xem phiên đã có người đặt giá hay chưa
    public boolean hasBids() {
        return this.bidHistory != null && !this.bidHistory.isEmpty();
    }
}