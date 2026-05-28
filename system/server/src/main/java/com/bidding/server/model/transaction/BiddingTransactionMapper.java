package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.Auction;
import com.bidding.server.model.auction.AuctionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BiddingTransactionMapper {
    BiddingTransactionMapper INSTANCE = Mappers.getMapper(BiddingTransactionMapper.class);

    @Mapping(target = "auction", source = "auction", qualifiedByName = "auctionReference")
    BiddingTransactionEntity toEntity(BiddingTransaction model);

    @Mapping(target = "auction", ignore = true)
    BiddingTransaction toModel(BiddingTransactionEntity entity);

    @Named("auctionReference")
    default AuctionEntity toAuctionEntity(Auction auction) {
        if (auction == null) {
            return null;
        }
        AuctionEntity entity = new AuctionEntity();
        entity.setId(auction.getId());
        return entity;
    }
}
