package com.bidding.server.model.auction;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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
    @Mapping(target = "followerIds", source = "followerIds", qualifiedByName = "toFollowerSet")
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
    @Mapping(target = "followerIds", source = "followerIds", qualifiedByName = "toFollowerSet")
    AuctionEntity toEntity(Auction auctionModel);

    // --- CHIỀU VỀ: DATABASE -> RAM ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "wrapAtomic")
    @Mapping(target = "bidHistory", source = "transactions")
    @Mapping(target = "reverse", source = "reverse")
    @Mapping(target = "dropStep", source = "dropStep")
    @Mapping(target = "followerIds", source = "followerIds", qualifiedByName = "toFollowerList")
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

    @Named("toFollowerList")
    default CopyOnWriteArrayList<String> toFollowerList(Set<String> followerIds) {
        return new CopyOnWriteArrayList<>(followerIds != null ? followerIds : Set.of());
    }

    @Named("toFollowerSet")
    default Set<String> toFollowerSet(CopyOnWriteArrayList<String> followerIds) {
        return new HashSet<>(followerIds != null ? followerIds : List.of());
    }

    // --- MAPPING DANH SÁCH ---
    List<AuctionEntity> toEntityList(List<Auction> models);
    List<Auction> toModelList(List<AuctionEntity> entities);
}
