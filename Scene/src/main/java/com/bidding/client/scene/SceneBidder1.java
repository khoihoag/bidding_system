package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SceneBidder1 {

    public static class BidEntry {
        public final String bidder;
        public final long price;
        public final LocalDateTime time;

        public BidEntry(String bidder, long price, LocalDateTime time) {
            this.bidder = bidder;
            this.price = price;
            this.time = time;
        }
    }

    public static class ProductData {
        public int id;
        public String auctionId = "";
        public String name = "San pham";
        public String category = "Khac";
        public String startPrice = "0";
        public String currentPrice = "0";
        public String description = "Chua co mo ta.";
        public String seller = "Chua ro";
        public double sellerRating = 0.0;
        public LocalDateTime startTime = LocalDateTime.now();
        public LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        public List<String> imagePaths = new ArrayList<>();
        public Map<String, String> specs = new LinkedHashMap<>();
        public int bidCount;
        public int participants;
        public String topBidder = "---";
        public List<BidEntry> bidHistory = new ArrayList<>();

        public String getStatusType() {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(startTime)) {
                return "UPCOMING";
            }
            if (now.isAfter(endTime)) {
                return "FINISHED";
            }
            return "RUNNING";
        }
    }

    @FXML private StackPane rootPane;
    @FXML private BorderPane mainPage;
    @FXML private BorderPane detailPage;
    @FXML private BorderPane watchlistPage;
    @FXML private AuctionDetailController detailPageController;

    @FXML private NavBarController homeNavbarController;
    @FXML private NavBarController watchNavbarController;

    @FXML private VBox sideMenu;
    @FXML private VBox accountPopup;
    @FXML private VBox toastHost;
    @FXML private Region dimOverlay;

    @FXML private TextField searchField;
    @FXML private Label lblClock;
    @FXML private Label lblClockDetail;
    @FXML private Label lblClockWatch;
    @FXML private Label lblBalance;
    @FXML private Label lblBalancePopup;
    @FXML private Label lblUserName;

    @FXML private ImageView imgAvatar;
    @FXML private ImageView imgAvatarPopup;
    @FXML private Label lblAvatarFallback;
    @FXML private Label lblAvatarPopupFallback;

    @FXML private ComboBox<String> filterCategory;
    @FXML private ComboBox<String> filterStatus;
    @FXML private ComboBox<String> filterPrice;
    @FXML private ComboBox<String> sortCombo;

    @FXML private FlowPane runningProductsPane;
    @FXML private FlowPane upcomingProductsPane;
    @FXML private FlowPane finishedProductsPane;
    @FXML private Label lblRunningEmpty;
    @FXML private Label lblUpcomingEmpty;
    @FXML private Label lblFinishedEmpty;

    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label lblPageInfo;

    @FXML private FlowPane watchlistPane;
    @FXML private Label lblWatchlistEmpty;

    private static final int PAGE_SIZE = 12;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final Set<Integer> watchedProductIds = new HashSet<>();
    private final List<ProductData> allProducts = new ArrayList<>();
    private final Map<Integer, AuctionCardController> cardMap = new LinkedHashMap<>();
    private List<ProductData> filteredProducts = new ArrayList<>();

    private Timeline clockTimeline;
    private boolean isMenuOpen;
    private int currentPage;
    private int unreadNotificationCount = 3;
    private ProductData selectedProduct;
    private ToastManager toastManager;

    @FXML
    public void initialize() {
        toastManager = toastHost == null ? null : new ToastManager(toastHost);
        startClock();
        clipAvatars();
        initFilterComboBoxes();
        initNavBars();
        bindDetailCallbacks();
        if (dimOverlay != null) {
            dimOverlay.setOnMouseClicked(this::closeAll);
        }

        filteredProducts = new ArrayList<>(allProducts);
        setupNetworkListener();
        NetworkClient.send("{\"action\":\"GET_ITEMS\"}");
    }

    private void bindDetailCallbacks() {
        if (detailPageController == null) {
            return;
        }

        detailPageController.setCallbacks(
                () -> {
                    detailPage.setVisible(false);
                    watchlistPage.setVisible(false);
                    mainPage.setVisible(true);
                    refreshVisibleDetail();
                },
                productId -> {
                    toggleWatch(productId);
                    refreshWatchlistPane();
                },
                productId -> watchedProductIds.contains(productId)
        );
    }

    private void setupNetworkListener() {
        NetworkClient.itemsListener = response -> {
            String action = getString(response, "action", "");
            switch (action) {
                case "ITEMS_LIST" -> handleItemsList(response);
                case "UPDATE_PRICE", "NEW_BID" -> handlePriceUpdate(response);
                case "AUCTION_END" -> handleAuctionEnd(response);
                case "NEW_AUCTION_POSTED" -> handleNewAuctionPosted(response);
                case "GLOBAL_NOTIFY" -> showInfoToast(getString(response, "message", "Co thong bao moi tu server."));
                case "ERROR" -> showInfoToast(getString(response, "message", "Server tra ve loi."));
                default -> {
                }
            }
        };
    }

    private void handleItemsList(JsonObject response) {
        JsonArray itemsArray = getArray(response, "items");
        if (itemsArray == null) {
            itemsArray = getArray(response, "data");
        }
        if (itemsArray == null) {
            showInfoToast("Khong co du lieu danh sach dau gia tu server.");
            return;
        }

        allProducts.clear();
        for (JsonElement element : itemsArray) {
            if (element != null && element.isJsonObject()) {
                allProducts.add(parseProduct(element.getAsJsonObject()));
            }
        }

        mergeFilterOptionsFromProducts();
        filteredProducts = new ArrayList<>(allProducts);
        currentPage = 0;
        renderHomePage();
        refreshWatchlistPane();
        refreshVisibleDetail();

        if (allProducts.isEmpty()) {
            showInfoToast("Server da ket noi nhung chua co phien dau gia nao.");
        }
    }

    private void handlePriceUpdate(JsonObject response) {
        ProductData product = resolveProduct(response);
        if (product == null) {
            return;
        }

        long newPrice = getLong(response, "newPrice", safeParseLong(product.currentPrice));
        product.currentPrice = String.valueOf(newPrice);
        product.topBidder = getString(response, "winnerId", getString(response, "highestBidder", product.topBidder));
        product.bidCount = Math.max(product.bidCount + 1, getInt(response, "bidCount", product.bidCount + 1));
        product.participants = Math.max(product.participants, getInt(response, "participants", product.participants));
        product.bidHistory.add(0, new BidEntry(product.topBidder, newPrice, LocalDateTime.now()));

        renderHomePage();
        refreshWatchlistPane();
        refreshVisibleDetail();
    }

    private void handleAuctionEnd(JsonObject response) {
        ProductData product = resolveProduct(response);
        if (product == null) {
            return;
        }

        long finalPrice = getLong(response, "finalPrice", safeParseLong(product.currentPrice));
        product.currentPrice = String.valueOf(finalPrice);
        product.topBidder = getString(response, "winnerId", getString(response, "winner", getString(response, "highestBidder", product.topBidder)));
        product.endTime = LocalDateTime.now();

        renderHomePage();
        refreshWatchlistPane();
        refreshVisibleDetail();
    }

    private void handleNewAuctionPosted(JsonObject response) {
        JsonObject data = getObject(response, "data");
        if (data == null) {
            return;
        }

        ProductData incoming = parseProduct(data);
        ProductData existing = findProductByAuctionId(incoming.auctionId);
        if (existing != null) {
            allProducts.remove(existing);
        } else {
            ProductData existingByIntId = findProductById(incoming.id);
            if (existingByIntId != null) {
                allProducts.remove(existingByIntId);
            }
        }

        allProducts.add(0, incoming);
        mergeFilterOptionsFromProducts();
        filteredProducts = new ArrayList<>(allProducts);
        currentPage = 0;
        renderHomePage();
        refreshWatchlistPane();
        showInfoToast("Co phien dau gia moi vua duoc dang len san.");
    }

    private ProductData parseProduct(JsonObject object) {
        ProductData product = new ProductData();
        product.auctionId = getString(object, "auctionId", getString(object, "id", ""));
        product.id = getInt(object, "itemId", extractNumericId(product.auctionId));

        JsonObject itemObject = getObject(object, "item");
        String nestedItemName = itemObject == null ? "" : getString(itemObject, "name", "");
        product.name = getString(object, "itemName", getString(object, "name", getString(object, "title", nestedItemName.isBlank() ? "San pham #" + product.id : nestedItemName)));
        product.category = getString(object, "category", "Khac");

        long startPrice = getLong(object, "startPrice",
                getLong(object, "startingPrice", getLong(object, "basePrice", getLong(object, "price", 0L))));
        long currentPrice = getLong(object, "currentPrice", startPrice);
        product.startPrice = String.valueOf(startPrice);
        product.currentPrice = String.valueOf(currentPrice);

        product.description = getString(object, "description", getString(object, "desc", "Chua co mo ta tu server."));
        product.seller = getString(object, "seller", getString(object, "sellerName", "Chua ro"));
        product.sellerRating = getDouble(object, "sellerRating", getDouble(object, "rating", 0.0));
        product.bidCount = getInt(object, "bidCount", getInt(object, "totalBids", 0));
        product.participants = getInt(object, "participants", getInt(object, "watchers", 0));
        product.topBidder = getString(object, "winnerId", getString(object, "highestBidder", getString(object, "topBidder", "---")));

        product.startTime = parseDateTime(object, "startTime", parseDateTime(object, "startDate", LocalDateTime.now()));
        product.endTime = parseDateTime(object, "endTime",
                parseDateTime(object, "time", parseDateTime(object, "endDate", product.startTime.plusHours(1))));
        if (!product.endTime.isAfter(product.startTime)) {
            product.endTime = product.startTime.plusHours(1);
        }

        product.imagePaths = parseImagePaths(object);
        product.specs = parseSpecs(object);
        product.bidHistory = parseBidHistory(object);
        if (product.bidHistory.isEmpty() && currentPrice > 0 && !"---".equals(product.topBidder)) {
            product.bidHistory.add(new BidEntry(product.topBidder, currentPrice, LocalDateTime.now()));
        }

        if (product.auctionId == null || product.auctionId.isBlank()) {
            product.auctionId = "auc-" + product.id;
        }

        return product;
    }

    private List<String> parseImagePaths(JsonObject object) {
        List<String> paths = new ArrayList<>();
        JsonArray images = getArray(object, "images");
        if (images == null) {
            images = getArray(object, "imagePaths");
        }

        if (images != null) {
            for (JsonElement image : images) {
                if (image == null || image.isJsonNull()) {
                    continue;
                }
                if (image.isJsonPrimitive()) {
                    paths.add(image.getAsString());
                } else if (image.isJsonObject()) {
                    JsonObject imageObj = image.getAsJsonObject();
                    String value = getString(imageObj, "path", getString(imageObj, "url", getString(imageObj, "imageUrl", "")));
                    if (!value.isBlank()) {
                        paths.add(value);
                    }
                }
            }
        }

        String singleImage = getString(object, "image", getString(object, "imageUrl", ""));
        if (!singleImage.isBlank()) {
            paths.add(singleImage);
        }

        return paths.stream().distinct().collect(Collectors.toList());
    }

    private Map<String, String> parseSpecs(JsonObject object) {
        Map<String, String> specs = new LinkedHashMap<>();
        JsonObject specsObject = getObject(object, "specs");
        if (specsObject == null) {
            specsObject = getObject(object, "specifications");
        }

        if (specsObject != null) {
            for (Map.Entry<String, JsonElement> entry : specsObject.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isJsonNull()) {
                    specs.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }

        if (specs.isEmpty()) {
            specs.put("Item ID", String.valueOf(getInt(object, "itemId", getInt(object, "id", 0))));
            specs.put("Danh muc", getString(object, "category", "Khac"));
        }

        return specs;
    }

    private List<BidEntry> parseBidHistory(JsonObject object) {
        List<BidEntry> history = new ArrayList<>();
        JsonArray historyArray = getArray(object, "bidHistory");
        if (historyArray == null) {
            historyArray = getArray(object, "history");
        }
        if (historyArray == null) {
            return history;
        }

        for (JsonElement element : historyArray) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject bidObject = element.getAsJsonObject();
            String bidder = getString(bidObject, "bidder", getString(bidObject, "user", "---"));
            long price = getLong(bidObject, "price", getLong(bidObject, "amount", 0L));
            LocalDateTime time = parseDateTime(bidObject, "time", parseDateTime(bidObject, "createdAt", LocalDateTime.now()));
            history.add(new BidEntry(bidder, price, time));
        }
        return history;
    }

    private void startClock() {
        clockTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, event -> {
                    String time = LocalTime.now().format(TIME_FMT);
                    if (lblClock != null) {
                        lblClock.setText(time);
                    }
                    if (lblClockDetail != null) {
                        lblClockDetail.setText(time);
                    }
                    if (lblClockWatch != null) {
                        lblClockWatch.setText(time);
                    }
                    if (homeNavbarController != null) {
                        homeNavbarController.setClockText(time);
                    }
                    if (watchNavbarController != null) {
                        watchNavbarController.setClockText(time);
                    }
                }),
                new KeyFrame(Duration.seconds(1))
        );
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void clipAvatars() {
        if (imgAvatar != null) {
            imgAvatar.setClip(new Circle(26, 26, 26));
        }
        if (imgAvatarPopup != null) {
            imgAvatarPopup.setClip(new Circle(30, 30, 30));
        }
    }

    private void initNavBars() {
        configureNavBar(homeNavbarController, "home", true);
        configureNavBar(watchNavbarController, "watchlist", false);
        syncAvatarToNavBars(null);
    }

    private void configureNavBar(NavBarController controller, String activeSection, boolean searchVisible) {
        if (controller == null) {
            return;
        }

        String userName = lblUserName == null ? "Bidder" : lblUserName.getText();
        String balance = lblBalancePopup == null ? "0 d" : lblBalancePopup.getText().replace("So du: ", "");

        controller.configure(activeSection, userName, balance, unreadNotificationCount, searchVisible);
        controller.setCallbacks(
                () -> toggleMenu(null),
                () -> menuGoHome(null),
                () -> menuGoWatchlist(null),
                this::showProfileToast,
                () -> menuGoNotifications(null),
                this::applySearchQuery,
                () -> handleLogout(null),
                () -> toggleAccountInfo(null)
        );
    }

    private void showProfileToast() {
        showInfoToast("Thong tin ho so hien dang dung du lieu giao dien.");
    }

    private void syncAvatarToNavBars(Image avatar) {
        if (homeNavbarController != null) {
            homeNavbarController.setAvatarImage(avatar);
        }
        if (watchNavbarController != null) {
            watchNavbarController.setAvatarImage(avatar);
        }
    }

    private void initFilterComboBoxes() {
        if (filterCategory != null) {
            filterCategory.getItems().setAll("Tat ca", "Khac");
            filterCategory.getSelectionModel().selectFirst();
        }
        if (filterStatus != null) {
            filterStatus.getItems().setAll("Tat ca", "RUNNING", "UPCOMING", "FINISHED");
            filterStatus.getSelectionModel().selectFirst();
        }
        if (filterPrice != null) {
            filterPrice.getItems().setAll("Tat ca", "< 1.000.000", "1-5 trieu", "5-20 trieu", "20-50 trieu", "> 50 trieu");
            filterPrice.getSelectionModel().selectFirst();
        }
        if (sortCombo != null) {
            sortCombo.getItems().setAll("Moi nhat", "Gia tang dan", "Gia giam dan", "Ket thuc som nhat");
            sortCombo.getSelectionModel().selectFirst();
        }
    }

    private void mergeFilterOptionsFromProducts() {
        if (filterCategory == null) {
            return;
        }

        List<String> categories = allProducts.stream()
                .map(product -> product.category == null || product.category.isBlank() ? "Khac" : product.category)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<String> items = new ArrayList<>();
        items.add("Tat ca");
        items.addAll(categories);
        filterCategory.getItems().setAll(items);
        filterCategory.getSelectionModel().selectFirst();
    }

    private void renderHomePage() {
        if (runningProductsPane == null || upcomingProductsPane == null || finishedProductsPane == null) {
            return;
        }

        cardMap.clear();
        runningProductsPane.getChildren().clear();
        upcomingProductsPane.getChildren().clear();
        finishedProductsPane.getChildren().clear();

        List<ProductData> workingSet = new ArrayList<>(filteredProducts);
        sortProducts(workingSet);

        int totalPages = Math.max(1, (int) Math.ceil((double) workingSet.size() / PAGE_SIZE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int fromIndex = Math.min(currentPage * PAGE_SIZE, workingSet.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, workingSet.size());
        List<ProductData> pageItems = workingSet.subList(fromIndex, toIndex);

        List<ProductData> running = pageItems.stream().filter(product -> "RUNNING".equals(product.getStatusType())).collect(Collectors.toList());
        List<ProductData> upcoming = pageItems.stream().filter(product -> "UPCOMING".equals(product.getStatusType())).collect(Collectors.toList());
        List<ProductData> finished = pageItems.stream().filter(product -> "FINISHED".equals(product.getStatusType())).collect(Collectors.toList());

        for (ProductData product : running) {
            runningProductsPane.getChildren().add(createSmallCard(product));
        }
        for (ProductData product : upcoming) {
            upcomingProductsPane.getChildren().add(createSmallCard(product));
        }
        for (ProductData product : finished) {
            finishedProductsPane.getChildren().add(createSmallCard(product));
        }

        lblRunningEmpty.setVisible(running.isEmpty());
        lblUpcomingEmpty.setVisible(upcoming.isEmpty());
        lblFinishedEmpty.setVisible(finished.isEmpty());

        lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
    }

    private void sortProducts(List<ProductData> products) {
        String sortValue = sortCombo == null ? "Moi nhat" : sortCombo.getValue();
        if ("Gia tang dan".equals(sortValue)) {
            products.sort(Comparator.comparingLong(product -> safeParseLong(product.currentPrice)));
        } else if ("Gia giam dan".equals(sortValue)) {
            products.sort((left, right) -> Long.compare(safeParseLong(right.currentPrice), safeParseLong(left.currentPrice)));
        } else if ("Ket thuc som nhat".equals(sortValue)) {
            products.sort(Comparator.comparing(product -> product.endTime));
        } else {
            products.sort(Comparator.comparing((ProductData product) -> product.startTime).reversed());
        }
    }

    private VBox createSmallCard(ProductData data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidding/client/scene/auction_card.fxml"));
            VBox card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.configure(
                    data,
                    watchedProductIds.contains(data.id),
                    () -> onProductClicked(data),
                    productId -> {
                        toggleWatch(productId);
                        controller.updateWatchButton(watchedProductIds.contains(productId));
                        refreshWatchlistPane();
                    }
            );
            cardMap.put(data.id, controller);
            card.setUserData(data);
            return card;
        } catch (IOException exception) {
            VBox fallback = new VBox();
            fallback.setPrefWidth(228);
            fallback.setPadding(new Insets(12));
            fallback.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #dc2626; -fx-border-radius: 12; -fx-background-radius: 12;");
            fallback.getChildren().add(new Label("Khong load duoc auction_card.fxml"));
            return fallback;
        }
    }

    private void onProductClicked(ProductData product) {
        if (product == null) {
            return;
        }

        selectedProduct = product;
        if (isMenuOpen) {
            closeMenuLogic();
        }
        if (accountPopup != null) {
            accountPopup.setVisible(false);
        }
        if (dimOverlay != null) {
            dimOverlay.setVisible(false);
        }

        if (detailPageController != null) {
            detailPageController.setProductData(product);
        }
        if (mainPage != null) {
            mainPage.setVisible(false);
        }
        if (watchlistPage != null) {
            watchlistPage.setVisible(false);
        }
        if (detailPage != null) {
            detailPage.setVisible(true);
        }
    }

    private void refreshVisibleDetail() {
        if (selectedProduct != null && detailPage != null && detailPage.isVisible() && detailPageController != null) {
            ProductData latest = findProductById(selectedProduct.id);
            if (latest != null) {
                selectedProduct = latest;
                detailPageController.setProductData(latest);
            }
        }
    }

    private Label buildStatusBadge(String statusType) {
        Label label = new Label();
        switch (statusType) {
            case "RUNNING" -> {
                label.setText("RUNNING");
                label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #92400e; -fx-background-color: #fef3c7; -fx-padding: 3 8; -fx-background-radius: 8;");
            }
            case "UPCOMING" -> {
                label.setText("OPEN");
                label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #166534; -fx-background-color: #dcfce7; -fx-padding: 3 8; -fx-background-radius: 8;");
            }
            default -> {
                label.setText("FINISHED");
                label.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #991b1b; -fx-background-color: #fee2e2; -fx-padding: 3 8; -fx-background-radius: 8;");
            }
        }
        return label;
    }

    private void addImgPlaceholder(StackPane box) {
        Rectangle rectangle = new Rectangle(228, 148, Color.web("#f3f4f6"));
        Label label = new Label("No image");
        label.setStyle("-fx-font-size: 16px; -fx-text-fill: #9ca3af; -fx-font-weight: bold;");
        box.getChildren().setAll(rectangle, label);
    }

    private void updateCountdownLabel(Label label, ProductData data) {
        LocalDateTime now = LocalDateTime.now();
        if ("UPCOMING".equals(data.getStatusType())) {
            long seconds = Math.max(0L, ChronoUnit.SECONDS.between(now, data.startTime));
            label.setText("Bat dau sau " + formatSecs(seconds));
            label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        } else if (now.isAfter(data.endTime)) {
            label.setText("Da ket thuc");
            label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
        } else {
            long seconds = Math.max(0L, ChronoUnit.SECONDS.between(now, data.endTime));
            label.setText("Con " + formatSecs(seconds));
            label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (seconds < 300 ? "#dc2626" : "#16a34a") + ";");
        }
    }

    private String formatSecs(long totalSeconds) {
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;
        if (days > 0) {
            return String.format(Locale.US, "%d ngay %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String query = homeNavbarController != null ? homeNavbarController.getSearchText() : (searchField == null ? "" : searchField.getText());
        applySearchQuery(query);
    }

    private void applySearchQuery(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredProducts = allProducts.stream()
                .filter(product -> normalized.isBlank()
                        || product.name.toLowerCase(Locale.ROOT).contains(normalized)
                        || product.seller.toLowerCase(Locale.ROOT).contains(normalized)
                        || product.category.toLowerCase(Locale.ROOT).contains(normalized))
                .collect(Collectors.toList());
        currentPage = 0;
        renderHomePage();
    }

    @FXML
    void handleFilter(ActionEvent event) {
        String category = filterCategory == null ? "Tat ca" : filterCategory.getValue();
        String status = filterStatus == null ? "Tat ca" : filterStatus.getValue();
        String price = filterPrice == null ? "Tat ca" : filterPrice.getValue();

        filteredProducts = allProducts.stream()
                .filter(product -> matchesCategory(product, category))
                .filter(product -> "Tat ca".equals(status) || product.getStatusType().equals(status))
                .filter(product -> matchesPrice(product, price))
                .collect(Collectors.toList());

        currentPage = 0;
        renderHomePage();
    }

    private boolean matchesCategory(ProductData product, String category) {
        return category == null || "Tat ca".equals(category) || category.equals(product.category);
    }

    private boolean matchesPrice(ProductData product, String priceFilter) {
        if (priceFilter == null || "Tat ca".equals(priceFilter)) {
            return true;
        }

        long price = safeParseLong(product.currentPrice);
        return switch (priceFilter) {
            case "< 1.000.000" -> price < 1_000_000L;
            case "1-5 trieu" -> price >= 1_000_000L && price <= 5_000_000L;
            case "5-20 trieu" -> price > 5_000_000L && price <= 20_000_000L;
            case "20-50 trieu" -> price > 20_000_000L && price <= 50_000_000L;
            case "> 50 trieu" -> price > 50_000_000L;
            default -> true;
        };
    }

    @FXML
    void handleClearFilter(ActionEvent event) {
        if (filterCategory != null) {
            filterCategory.getSelectionModel().selectFirst();
        }
        if (filterStatus != null) {
            filterStatus.getSelectionModel().selectFirst();
        }
        if (filterPrice != null) {
            filterPrice.getSelectionModel().selectFirst();
        }
        if (sortCombo != null) {
            sortCombo.getSelectionModel().selectFirst();
        }
        if (homeNavbarController != null) {
            homeNavbarController.clearSearch();
        }
        if (searchField != null) {
            searchField.clear();
        }

        filteredProducts = new ArrayList<>(allProducts);
        currentPage = 0;
        renderHomePage();
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 0) {
            currentPage--;
            renderHomePage();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE));
        if (currentPage < totalPages - 1) {
            currentPage++;
            renderHomePage();
        }
    }

    private void toggleWatch(int productId) {
        if (watchedProductIds.contains(productId)) {
            watchedProductIds.remove(productId);
        } else {
            watchedProductIds.add(productId);
        }
        refreshVisibleDetail();
    }

    private void refreshWatchlistPane() {
        if (watchlistPane == null) {
            return;
        }

        watchlistPane.getChildren().clear();
        List<ProductData> watchedProducts = allProducts.stream()
                .filter(product -> watchedProductIds.contains(product.id))
                .collect(Collectors.toList());

        if (lblWatchlistEmpty != null) {
            lblWatchlistEmpty.setVisible(watchedProducts.isEmpty());
        }

        for (ProductData product : watchedProducts) {
            watchlistPane.getChildren().add(createSmallCard(product));
        }
    }

    @FXML
    void toggleMenu(ActionEvent event) {
        if (accountPopup != null) {
            accountPopup.setVisible(false);
        }
        if (!isMenuOpen) {
            if (dimOverlay != null) {
                dimOverlay.setVisible(true);
            }
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), sideMenu);
            transition.setToX(0);
            transition.play();
            isMenuOpen = true;
        } else {
            closeMenuLogic();
            checkAndHideOverlay();
        }
    }

    private void closeMenuLogic() {
        if (sideMenu == null) {
            return;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis(250), sideMenu);
        transition.setToX(-300);
        transition.play();
        isMenuOpen = false;
    }

    @FXML
    void toggleAccountInfo(MouseEvent event) {
        if (isMenuOpen) {
            closeMenuLogic();
        }
        if (accountPopup == null) {
            return;
        }
        boolean showing = accountPopup.isVisible();
        accountPopup.setVisible(!showing);
        if (dimOverlay != null) {
            dimOverlay.setVisible(!showing);
        }
    }

    @FXML
    void closeAll(MouseEvent event) {
        if (isMenuOpen) {
            closeMenuLogic();
        }
        if (accountPopup != null) {
            accountPopup.setVisible(false);
        }
        if (rootPane != null) {
            rootPane.requestFocus();
        }
        checkAndHideOverlay();
    }

    private void checkAndHideOverlay() {
        boolean searchFocused = searchField != null && searchField.isFocused();
        if (!isMenuOpen && (accountPopup == null || !accountPopup.isVisible()) && !searchFocused && dimOverlay != null) {
            dimOverlay.setVisible(false);
        }
    }

    @FXML
    void menuGoHome(ActionEvent event) {
        closeMenuLogic();
        checkAndHideOverlay();
        if (detailPage != null) {
            detailPage.setVisible(false);
        }
        if (watchlistPage != null) {
            watchlistPage.setVisible(false);
        }
        if (mainPage != null) {
            mainPage.setVisible(true);
        }
    }

    @FXML
    void menuGoWatchlist(ActionEvent event) {
        closeMenuLogic();
        checkAndHideOverlay();
        if (mainPage != null) {
            mainPage.setVisible(false);
        }
        if (detailPage != null) {
            detailPage.setVisible(false);
        }
        refreshWatchlistPane();
        if (watchlistPage != null) {
            watchlistPage.setVisible(true);
        }
    }

    @FXML
    void menuGoNotifications(ActionEvent event) {
        unreadNotificationCount = 0;
        if (homeNavbarController != null) {
            homeNavbarController.setNotificationCount(unreadNotificationCount);
        }
        if (watchNavbarController != null) {
            watchNavbarController.setNotificationCount(unreadNotificationCount);
        }
        openScene("BidderNotifications.fxml", "BidViet - Thong bao");
    }

    @FXML
    void menuGoMyBids(ActionEvent event) {
        openScene("SceneMyBids.fxml", "BidViet - Dau gia cua toi");
    }

    @FXML
    void menuGoHistory1(ActionEvent event) {
        openScene("SceneLSGD.fxml", "BidViet - Lich su giao dich");
    }

    @FXML
    void handleOpenAutoBid(ActionEvent event) {
        openScene("SceneAutoBid.fxml", "BidViet - Tra gia tu dong");
    }

    @FXML
    void switchToSeller(ActionEvent event) {
        openScene("SceneSeller1.fxml", "BidViet - Bang dieu khien nguoi ban");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        openScene("Scene1.fxml", "Dang nhap he thong");
    }

    @FXML
    void handleChangeAvatar(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chon anh dai dien");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tap tin anh", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(rootPane == null ? null : rootPane.getScene().getWindow());
        if (selectedFile == null) {
            return;
        }

        try {
            Image avatar = new Image(selectedFile.toURI().toString());
            if (imgAvatar != null) {
                imgAvatar.setImage(avatar);
            }
            if (imgAvatarPopup != null) {
                imgAvatarPopup.setImage(avatar);
            }
            if (lblAvatarFallback != null) {
                lblAvatarFallback.setVisible(false);
            }
            if (lblAvatarPopupFallback != null) {
                lblAvatarPopupFallback.setVisible(false);
            }
            syncAvatarToNavBars(avatar);
        } catch (Exception exception) {
            showNavigationError("Khong the tai anh dai dien da chon.");
        }
    }

    private void openScene(String fxmlName, String title) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidding/client/scene/" + fxmlName));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception exception) {
            showNavigationError("Khong the mo " + fxmlName + "\n\nLoi: " + exception.getMessage());
        }
    }

    private void showNavigationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loi dieu huong");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoToast(String message) {
        if (toastManager != null) {
            toastManager.show(message, ToastManager.ToastType.INFO);
        }
    }

    private ProductData findProductById(int productId) {
        return allProducts.stream().filter(product -> product.id == productId).findFirst().orElse(null);
    }

    private ProductData findProductByAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return null;
        }
        return allProducts.stream()
                .filter(product -> auctionId.equals(product.auctionId))
                .findFirst()
                .orElse(null);
    }

    private ProductData resolveProduct(JsonObject response) {
        String auctionId = getString(response, "auctionId", "");
        ProductData product = findProductByAuctionId(auctionId);
        if (product != null) {
            return product;
        }

        int itemId = getInt(response, "itemId", -1);
        if (itemId >= 0) {
            return findProductById(itemId);
        }

        return null;
    }

    private String formatPrice(String price) {
        try {
            long value = Long.parseLong(price);
            return String.format(Locale.US, "%,d", value).replace(',', '.');
        } catch (Exception exception) {
            return price == null ? "0" : price;
        }
    }

    private long safeParseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replace(".", "").replace(",", "").trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private int extractNumericId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0;
        }

        String digits = rawValue.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String getString(JsonObject object, String field, String fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(field).getAsString();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private int getInt(JsonObject object, String field, int fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(field).getAsInt();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private long getLong(JsonObject object, String field, long fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(field).getAsLong();
        } catch (Exception exception) {
            try {
                return Long.parseLong(object.get(field).getAsString().replace(".", "").replace(",", ""));
            } catch (Exception nestedException) {
                return fallback;
            }
        }
    }

    private double getDouble(JsonObject object, String field, double fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(field).getAsDouble();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private JsonArray getArray(JsonObject object, String field) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull() || !object.get(field).isJsonArray()) {
            return null;
        }
        return object.getAsJsonArray(field);
    }

    private JsonObject getObject(JsonObject object, String field) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull() || !object.get(field).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(field);
    }

    private LocalDateTime parseDateTime(JsonObject object, String field, LocalDateTime fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            return fallback;
        }
        JsonElement element = object.get(field);

        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                long raw = element.getAsLong();
                if (String.valueOf(Math.abs(raw)).length() > 10) {
                    return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(raw), ZoneId.systemDefault());
                }
                return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(raw), ZoneId.systemDefault());
            }

            String text = element.getAsString().trim();
            if (text.isBlank()) {
                return fallback;
            }

            List<DateTimeFormatter> formatters = Arrays.asList(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            );

            for (DateTimeFormatter formatter : formatters) {
                try {
                    if (formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME) {
                        return OffsetDateTime.parse(text, formatter).toLocalDateTime();
                    }
                    return LocalDateTime.parse(text, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        return fallback;
    }
}
