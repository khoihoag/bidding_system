package com.bidding.server.model;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.concurrent.atomic.AtomicReference;
import org.mapstruct.Named;
@Mapper
public interface AuctionMapper {
    AuctionMapper INSTANCE = Mappers.getMapper(AuctionMapper.class);

    // --- CHIỀU ĐI: RAM -> DATABASE ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "unwrapAtomic")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    AuctionEntity toEntity(Auction auctionModel);

    // --- CHIỀU VỀ: DATABASE -> RAM (Để load lại dữ liệu) ---
    @Mapping(target = "currentPrice", source = "currentPrice", qualifiedByName = "wrapAtomic")
    // Lưu ý: Lúc này target là Model, source là Entity
    Auction toModel(AuctionEntity entity);

    // --- TUYỆT CHIÊU BÓC/GÓI ---
    @Named("unwrapAtomic")
    default Double unwrapAtomic(AtomicReference<Double> atomicPrice) {
        return (atomicPrice != null) ? atomicPrice.get() : 0.0;
    }

    @Named("wrapAtomic")
    default AtomicReference<Double> wrapAtomic(Double price) {
        // Biến con số Double vô hồn từ DB thành cái hộp AtomicReference đa luồng
        return new AtomicReference<>(price != null ? price : 0.0);
    }

    // --- MAPPING DANH SÁCH ---
    // Chỉ cần 2 dòng này, MapStruct tự hiểu phải gọi toEntity/toModel cho từng phần tử
    List<AuctionEntity> toEntityList(List<Auction> models);
    List<Auction> toModelList(List<AuctionEntity> entities);
}