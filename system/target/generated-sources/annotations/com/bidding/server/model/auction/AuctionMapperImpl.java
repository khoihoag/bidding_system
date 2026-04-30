package com.bidding.server.model.auction;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-04-30T16:49:34+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
public class AuctionMapperImpl implements AuctionMapper {

    @Override
    public AuctionEntity toEntity(Auction auctionModel) {
        if ( auctionModel == null ) {
            return null;
        }

        AuctionEntity auctionEntity = new AuctionEntity();

        auctionEntity.setCurrentPrice( unwrapAtomic( auctionModel.getCurrentPrice() ) );
        auctionEntity.setId( auctionModel.getId() );
        auctionEntity.setCreatedAt( auctionModel.getCreatedAt() );
        auctionEntity.setUpdatedAt( auctionModel.getUpdatedAt() );
        auctionEntity.setAntiSnipingSeconds( auctionModel.getAntiSnipingSeconds() );
        auctionEntity.setCurrentWinner( auctionModel.getCurrentWinner() );
        auctionEntity.setEndTime( auctionModel.getEndTime() );
        auctionEntity.setExtensionSeconds( auctionModel.getExtensionSeconds() );
        auctionEntity.setItem( auctionModel.getItem() );
        auctionEntity.setStartTime( auctionModel.getStartTime() );
        auctionEntity.setStatus( auctionModel.getStatus() );

        return auctionEntity;
    }

    @Override
    public Auction toModel(AuctionEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Auction auction = new Auction();

        auction.setCurrentPrice( wrapAtomic( entity.getCurrentPrice() ) );
        auction.setId( entity.getId() );
        auction.setCreatedAt( entity.getCreatedAt() );
        auction.setUpdatedAt( entity.getUpdatedAt() );
        auction.setAntiSnipingSeconds( entity.getAntiSnipingSeconds() );
        auction.setCurrentWinner( entity.getCurrentWinner() );
        auction.setEndTime( entity.getEndTime() );
        auction.setExtensionSeconds( entity.getExtensionSeconds() );
        auction.setItem( entity.getItem() );
        auction.setStartTime( entity.getStartTime() );
        auction.setStatus( entity.getStatus() );

        return auction;
    }

    @Override
    public List<AuctionEntity> toEntityList(List<Auction> models) {
        if ( models == null ) {
            return null;
        }

        List<AuctionEntity> list = new ArrayList<AuctionEntity>( models.size() );
        for ( Auction auction : models ) {
            list.add( toEntity( auction ) );
        }

        return list;
    }

    @Override
    public List<Auction> toModelList(List<AuctionEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<Auction> list = new ArrayList<Auction>( entities.size() );
        for ( AuctionEntity auctionEntity : entities ) {
            list.add( toModel( auctionEntity ) );
        }

        return list;
    }
}
