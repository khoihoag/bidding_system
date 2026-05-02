package com.bidding.server.model.transaction;

import com.bidding.server.model.AuctionMapper;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-03T00:24:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.10 (Microsoft)"
)
public class BiddingTransactionMapperImpl implements BiddingTransactionMapper {

    private final AuctionMapper auctionMapper = AuctionMapper.INSTANCE;

    @Override
    public BiddingTransactionEntity toEntity(BiddingTransaction model) {
        if ( model == null ) {
            return null;
        }

        BiddingTransactionEntity biddingTransactionEntity = new BiddingTransactionEntity();

        biddingTransactionEntity.setId( model.getId() );
        biddingTransactionEntity.setBidder( model.getBidder() );
        biddingTransactionEntity.setAuction( auctionMapper.toEntity( model.getAuction() ) );
        biddingTransactionEntity.setBidAmount( model.getBidAmount() );
        biddingTransactionEntity.setBidTime( model.getBidTime() );
        biddingTransactionEntity.setStatus( model.getStatus() );
        biddingTransactionEntity.setAutoBid( model.isAutoBid() );

        return biddingTransactionEntity;
    }

    @Override
    public BiddingTransaction toModel(BiddingTransactionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        BiddingTransaction biddingTransaction = new BiddingTransaction();

        biddingTransaction.setId( entity.getId() );
        biddingTransaction.setBidder( entity.getBidder() );
        biddingTransaction.setAuction( auctionMapper.toModel( entity.getAuction() ) );
        biddingTransaction.setBidAmount( entity.getBidAmount() );
        biddingTransaction.setBidTime( entity.getBidTime() );
        biddingTransaction.setStatus( entity.getStatus() );
        biddingTransaction.setAutoBid( entity.isAutoBid() );

        return biddingTransaction;
    }
}
