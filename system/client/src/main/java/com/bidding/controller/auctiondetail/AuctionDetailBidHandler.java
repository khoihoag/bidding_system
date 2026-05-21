package com.bidding.controller.auctiondetail;

import com.google.gson.JsonObject;

/**
 * Validates and sends manual/auto bid requests.
 */
public class AuctionDetailBidHandler {

    private final AuctionDetailState state;
    private final AuctionDetailNetworkGateway network;
    private final AuctionDetailUiPresenter ui;

    public AuctionDetailBidHandler(AuctionDetailState state,
                                   AuctionDetailNetworkGateway network,
                                   AuctionDetailUiPresenter ui) {
        this.state = state;
        this.network = network;
        this.ui = ui;
    }

    public void placeBid(String rawAmountText) {
        boolean isReverse = false;
        if (state.currentSelectedAuction != null && state.currentSelectedAuction.has("isReverse")) {
            isReverse = state.currentSelectedAuction.get("isReverse").getAsBoolean();
        }

        double amount = 0;

        if (isReverse) {
            amount = 0;
        } else {
            String rawAmount = rawAmountText.trim();
            if (rawAmount.isEmpty()) {
                ui.showBidError("Vui lòng nhập mức giá trước khi đặt.");
                return;
            }
            try {
                amount = AuctionDetailFormats.parseAmount(rawAmount);
            } catch (NumberFormatException e) {
                ui.showBidError("Mức giá không hợp lệ. Vui lòng nhập số.");
                return;
            }
            if (amount <= 0) {
                ui.showBidError("Mức giá phải lớn hơn 0.");
                return;
            }
        }

        ui.setBidLoading(true);
        network.sendBid(state.currentAuctionId, amount);
    }

    public void registerAutoBid(String rawMaxBid, String rawIncrement) {
        if (rawMaxBid.isEmpty() || rawIncrement.isEmpty()) {
            ui.showAutoBidError("Vui lòng điền đầy đủ Giá tối đa và Bước giá.");
            return;
        }

        double maxBid;
        double increment;
        try {
            maxBid = AuctionDetailFormats.parseAmount(rawMaxBid);
            increment = AuctionDetailFormats.parseAmount(rawIncrement);
        } catch (NumberFormatException e) {
            ui.showAutoBidError("Giá trị không hợp lệ. Vui lòng nhập số.");
            return;
        }

        if (maxBid <= 0 || increment <= 0) {
            ui.showAutoBidError("Giá tối đa và bước giá phải lớn hơn 0.");
            return;
        }

        ui.setAutoBidLoading(true);
        network.sendAutoBid(state.currentAuctionId, maxBid, increment);
    }
}
