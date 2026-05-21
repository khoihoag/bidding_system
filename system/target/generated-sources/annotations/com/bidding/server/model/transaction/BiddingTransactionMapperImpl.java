package com.bidding.server.model.transaction;

import com.bidding.server.model.auction.AuctionMapper;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-21T16:50:33+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
public class BiddingTransactionMapperImpl implements BiddingTransactionMapper {

    private final AuctionMapper auctionMapper = AuctionMapper.INSTANCE;

    @Override
    public BiddingTransactionEntity toEntity(BiddingTransaction model) {
        if ( model == null ) {
            return null;
        }

        BiddingTransactionEntity biddingTransactionEntity = new BiddingTransactionEntity();

        biddingTransactionEntity.setAuction( auctionMapper.toEntity( model.getAuction() ) );
        biddingTransactionEntity.setAutoBid( model.isAutoBid() );
        biddingTransactionEntity.setBidAmount( model.getBidAmount() );
        biddingTransactionEntity.setBidTime( model.getBidTime() );
        biddingTransactionEntity.setBidder( model.getBidder() );
        biddingTransactionEntity.setExtended( model.isExtended() );
        biddingTransactionEntity.setId( model.getId() );
        biddingTransactionEntity.setStatus( model.getStatus() );

        return biddingTransactionEntity;
    }

    @Override
    public BiddingTransaction toModel(BiddingTransactionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        BiddingTransaction biddingTransaction = new BiddingTransaction();

        biddingTransaction.setAutoBid( entity.isAutoBid() );
        biddingTransaction.setBidAmount( entity.getBidAmount() );
        biddingTransaction.setBidTime( entity.getBidTime() );
        biddingTransaction.setBidder( entity.getBidder() );
        biddingTransaction.setExtended( entity.isExtended() );
        biddingTransaction.setId( entity.getId() );
        biddingTransaction.setStatus( entity.getStatus() );

        return biddingTransaction;
    }
}
