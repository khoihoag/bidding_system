package com.bidding.server.model.auction;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.mapstruct.MappingTarget;
import java.util.concurrent.atomic.AtomicReference;
import org.mapstruct.Named;

// Đừng quên dòng uses này để nó biết cách convert List Transaction nhé sếp!
@Mapper(uses = {com.bidding.server.model.transaction.BiddingTransactionMapper.class})
public interface AuctionMapper {

    AuctionMapper INSTANCE = Mappers.getMapper(AuctionMapper.class);

    // --- CẬP NHẬT GHI ĐÈ (Để không dính lỗi Hibernate) ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "unwrapAtomic")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    void updateEntityFromModel(Auction model, @MappingTarget AuctionEntity entity);

    // --- CHIỀU ĐI: RAM -> DATABASE ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "unwrapAtomic")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    AuctionEntity toEntity(Auction auctionModel);

    // --- CHIỀU VỀ: DATABASE -> RAM (Đã gộp 2 hàm của sếp làm 1) ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "wrapAtomic")
    @Mapping(target = "bidHistory", source = "transactions") // Bốc lịch sử từ DB lên
    Auction toModel(AuctionEntity entity);

    // --- TUYỆT CHIÊU BÓC/GÓI ---
    @Named("unwrapAtomic")
    default Double unwrapAtomic(AtomicReference<Double> atomicPrice) {
        return (atomicPrice != null) ? atomicPrice.get() : 0.0;
    }

    @Named("wrapAtomic")
    default AtomicReference<Double> wrapAtomic(Double price) {
        return new AtomicReference<>(price != null ? price : 0.0);
    }

    // --- MAPPING DANH SÁCH ---
    List<AuctionEntity> toEntityList(List<Auction> models);
    List<Auction> toModelList(List<AuctionEntity> entities);
}