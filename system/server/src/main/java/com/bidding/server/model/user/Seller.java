package com.bidding.server.model.user;

import com.bidding.server.exception.ItemAlreadyInAuctionException;
import com.bidding.server.model.auction.Auction;
import com.bidding.server.enums.UserRole;
import com.bidding.server.model.item.Item;
import com.bidding.server.enums.AuctionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Seller extends User {

    private Map<String, Item> listedItems;
    private Map<String, Auction> activeAuctions;
    private double totalRevenue;

    public Seller() {
        super();
        this.listedItems = new HashMap<>();
        this.activeAuctions = new HashMap<>();
        this.totalRevenue = 0.0;
    }

    public Seller( String username, String email, String passwordHash, String fullName) {
        super(username, email, passwordHash, fullName, UserRole.SELLER);
        this.listedItems = new HashMap<>();
        this.activeAuctions = new HashMap<>();
        this.totalRevenue = 0.0;
    }

    public Map<String, Item> getListedItems() {
        return listedItems;
    }

    public void setListedItems(Map<String, Item> listedItems) {
        this.listedItems = listedItems;
    }

    public Map<String, Auction> getActiveAuctions() {
        return activeAuctions;
    }

    public void setActiveAuctions(HashMap<String, Auction> activeAuctions) {
        this.activeAuctions = activeAuctions;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    // Tạo sản phẩm
    public Item createItem(Item item) {
        return item;
    }

    // Cập nhật sản phẩm
    public Item updateItem(String id, Item item) {
        return item;
    }

    // Xóa sản phẩm
    public boolean deleteItem(String id) throws ItemAlreadyInAuctionException {
        // Kiểm tra sản phẩm có tồn tại
        if (!this.listedItems.containsKey(id)) {
            return false;
        }
        if (this.activeAuctions.containsKey(id)) {
            throw new ItemAlreadyInAuctionException(
                    "Không thể xóa sản phẩm! Sản phẩm ID [" + id + "] đang nằm trong một phiên đấu giá."
            );
        }
        this.listedItems.remove(id);
        return true;
    }


    // Tạo phiên đấu giá
    public Auction createAuction(Item item, Object cfg) {
        // Kiểm tra item có tồn tại trong danh sách của seller hay không
        if (item == null || !this.listedItems.containsKey(item.getId())) {
            System.err.println("Sản phẩm không hợp lệ hoặc không thuộc quyền sở hữu của người bán.");
            return null;
        }

        Auction newAuction = new Auction();
        // Cần truyền item và cấu hình (cfg) vào newAuction nếu Auction hỗ trợ
        newAuction.setItem(item);
        newAuction.setStatus(AuctionStatus.OPEN);

        // Lưu vào danh sách các phiên đang chạy (activeAuctions sử dụng Map<String, Auction>)
        this.activeAuctions.put(newAuction.getId(), newAuction);
        return newAuction; 
    }

    // Huỷ phiên khi chưa có người bid
    public boolean cancelAuction(String auctionId) {
        if (!this.activeAuctions.containsKey(auctionId)) {
            System.err.println("Phiên đấu giá không tồn tại trong danh sách đang chạy.");
            return false;
        }

        Auction auction = this.activeAuctions.get(auctionId);
        
        // Kiểm tra xem có bid nào không thông qua phương thức từ class Auction
        if (auction.hasBids()) {
            System.err.println("Không thể hủy! Phiên đấu giá đã có người trả giá.");
            return false;
        }
        
        auction.setStatus(com.bidding.server.enums.AuctionStatus.CANCELED);

        // Hủy thành công: xóa khỏi activeAuctions map
        this.activeAuctions.remove(auctionId);
        return true; 
    }

    // Trả về danh sách các phiên đấu giá
    public List<Auction> getAuctionHistory() {
        return new ArrayList<>();
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }

    @Override
    public void validate() {
        // 1. Validate dữ liệu kế thừa từ Entity (Mã định danh)
        if (getId() == null || getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Lỗi dữ liệu: ID định danh của Seller không được để trống.");
        }

        // 2. Validate dữ liệu kế thừa từ User (Thông tin tài khoản)
        super.validate();

        // 3. Validate dữ liệu đặc thù của Seller
        if (this.totalRevenue < 0) {
            throw new IllegalArgumentException("Lỗi nghiệp vụ: Tổng doanh thu (totalRevenue) không được là số âm.");
        }

        // Đảm bảo tính toàn vẹn của các Collection (tránh NullPointerException khi thao tác)
        if (this.listedItems == null) {
            throw new IllegalStateException("Lỗi hệ thống: Map 'listedItems' chưa được khởi tạo.");
        }
        if (this.activeAuctions == null) {
            throw new IllegalStateException("Lỗi hệ thống: Map 'activeAuctions' chưa được khởi tạo.");
        }
    }
}
