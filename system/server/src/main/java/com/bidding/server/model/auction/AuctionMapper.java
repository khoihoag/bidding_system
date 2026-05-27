package com.bidding.server.model.auction;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.mapstruct.MappingTarget;
import java.util.concurrent.atomic.AtomicReference;
import org.mapstruct.Named;

@Mapper(uses = {com.bidding.server.model.transaction.BiddingTransactionMapper.class})
public interface AuctionMapper {

    AuctionMapper INSTANCE = Mappers.getMapper(AuctionMapper.class);

    // --- CẬP NHẬT GHI ĐÈ ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "unwrapAtomic")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    // SỬA CHỮ isReverse THÀNH reverse THEO Ý THẰNG MAPSTRUCT:
    @Mapping(target = "reverse", source = "reverse")
    @Mapping(target = "dropStep", source = "dropStep")
    void updateEntityFromModel(Auction model, @MappingTarget AuctionEntity entity);

    // --- CHIỀU ĐI: RAM -> DATABASE ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "unwrapAtomic")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "reverse", source = "reverse")
    @Mapping(target = "dropStep", source = "dropStep")
    AuctionEntity toEntity(Auction auctionModel);

    // --- CHIỀU VỀ: DATABASE -> RAM ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "wrapAtomic")
    @Mapping(target = "bidHistory", source = "transactions")
    @Mapping(target = "reverse", source = "reverse")
    @Mapping(target = "dropStep", source = "dropStep")
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