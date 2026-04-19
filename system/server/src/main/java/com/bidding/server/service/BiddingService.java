package com.bidding.server.service;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.AuctionEntity;
import com.bidding.server.model.auction.AuctionMapper;
import com.bidding.server.model.auction.AutoBidConfig;
import com.bidding.server.model.transaction.BidTransaction;
import com.bidding.server.model.transaction.BiddingTransactionEntity;
import com.bidding.server.model.user.User;
import com.bidding.server.repository.AuctionRepository;
import com.bidding.server.repository.UserRepository;

import java.util.List;

public class BiddingService {

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    // private final WalletService walletService; // Nếu có xử lý trừ tiền

    public BiddingService(AuctionRepository auctionRepository, UserRepository userRepository) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    public BidTransaction placeBid(User actor, String auctionId, double amount) {
        // 1. Lôi dữ liệu từ DB lên (Entity)
        AuctionEntity entity = auctionRepository.findById(auctionId);
        if (entity == null) {
            throw new RuntimeException("Phiên đấu giá không tồn tại.");
        }

        // 2. PHIÊN DỊCH BẰNG MAPSTRUCT: Database (Entity) -> RAM (Model)
        // MapStruct sẽ tự động bọc currentPrice vào AtomicReference nhờ hàm wrapAtomic
        Auction auction = AuctionMapper.INSTANCE.toModel(entity);

        // 3. Fail-Fast: Chống Shill Bidding
        if (auction.getSeller().getId().equals(actor.getId())) {
            throw new IllegalArgumentException("Chủ sản phẩm không được tự đặt giá.");
        }

        // 4. LUÂN CHUYỂN LOGIC: Entity Model làm toán (An toàn đa luồng)
        BidTransaction tx = auction.placeBid(actor, amount);

        // 5. PHIÊN DỊCH NGƯỢC LẠI: RAM (Model) -> Database (Entity)
        // MapStruct sẽ tự động bóc AtomicReference ra thành Double nhờ hàm unwrapAtomic
        AuctionEntity entityToSave = AuctionMapper.INSTANCE.toEntity(auction);

        // 6. LƯU LẠI DB
        // Vì DAO của bạn đang dùng session.merge(entity), nên việc MapStruct tạo ra
        // một object entityToSave hoàn toàn mới (nhưng giữ nguyên ID) sẽ được Hibernate
        // tự động hiểu là Update (Cập nhật) chứ không phải Insert (Thêm mới). Rất mượt!
        auctionRepository.saveOrUpdate(entityToSave);

        // (Thực tế bạn sẽ cần lưu thêm BidTransaction vào DB ở đây)
        // bidRepository.save(tx);

        return tx;
    }

    public List<BiddingTransactionEntity> getActiveBids(User actor) {
        // Sử dụng Vũ khí mới trong UserRepository của bạn
        return userRepository.getBidHistoryByUserId(actor.getId());
    }

    // --- Các hàm Auto-Bid để giữ nguyên khung ---
    public void setAutoBid(User actor, String auctionId, AutoBidConfig config) {
        // Lưu cấu hình vào DB
    }

    public void cancelAutoBid(User actor, String auctionId) {
        // Xóa cấu hình khỏi DB
    }
}