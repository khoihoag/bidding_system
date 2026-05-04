package com.bidding.client.scene;

import com.bidding.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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

    // ─── Inner classes ────────────────────────────────────────────────────────

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
        public String serverStatus = "";

        public String getStatusType() {
            if (serverStatus != null && !serverStatus.isBlank()) {
                String s = serverStatus.toUpperCase();
                if (s.equals("RUNNING")) return "RUNNING";
                if (s.equals("FINISHED")) return "FINISHED";
                if (s.equals("OPEN") || s.equals("UPCOMING") || s.equals("SCHEDULED") || s.equals("PENDING")) return "UPCOMING";
                if (s.equals("NOT_AUCTION")) return "NOT_AUCTION";
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(startTime)) return "UPCOMING";
            if (now.isAfter(endTime)) return "FINISHED";
            return "RUNNING";
        }
    }

    // ─── FXML Fields ──────────────────────────────────────────────────────────

    @FXML private StackPane rootPane;
    @FXML private BorderPane mainPage;
    @FXML private BorderPane watchlistPage;
    @FXML private VBox sideMenu;
    @FXML private Region dimOverlay;
    @FXML private VBox accountPopup;

    @FXML private TextField searchField;
    @FXML private Label lblClock;
    @FXML private Label lblClockWatch;
    @FXML private Label lblClockDetail; // used in detail
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
    @FXML private FlowPane watchlistPane;

    @FXML private Label lblRunningEmpty;
    @FXML private Label lblUpcomingEmpty;
    @FXML private Label lblFinishedEmpty;
    @FXML private Label lblWatchlistEmpty;

    @FXML private Label lblPageInfo;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    @FXML private VBox toastHost;

    // Controllers for included FXMLs
    @FXML private NavBarController homeNavbarController;
    @FXML private NavBarController watchNavbarController;
    @FXML private AuctionDetailController detailPageController;
    @FXML private Parent detailPage;

    // ─── State ───────────────────────────────────────────────────────────────

    private final List<ProductData> allProducts = new ArrayList<>();
    private List<ProductData> filteredProducts = new ArrayList<>();
    private final Set<Integer> watchedProductIds = new HashSet<>();
    private ProductData selectedProduct;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 8;
    private String currentSearchQuery = "";
    private String currentCategory = "Tat ca";
    private String currentStatusFilter = "Tat ca";
    private String currentPriceRange = "Tat ca";

    private final Map<Integer, AuctionCardController> cardMap = new LinkedHashMap<>();
    private ToastManager toastManager;
    private Timeline clockTimeline;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private int unreadNotificationCount = 0;

    // ─── Initialization ───────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        toastManager = new ToastManager(toastHost);
        initFilterComboBoxes();
        setupNetworkListener();
        startClock();
        clipAvatars();
        initNavBars();

        // Initial request for data
        Platform.runLater(() -> {
            requestDataFromServer();
        });
    }

    private void requestDataFromServer() {
        if (NetworkClient.isConnected()) {
            // GET_ALL_ITEMS: Lấy toàn bộ kho hàng (cho Buyer)
            NetworkClient.send("{\"action\":\"GET_ALL_ITEMS\"}");
            // GET_AUCTIONS: Lấy trạng thái đấu giá (Running/Upcoming/Finished)
            NetworkClient.send("{\"action\":\"GET_AUCTIONS\"}");
        }
    }

    private void setupNetworkListener() {
        // Chỉ set itemsListener cho scene này — KHÔNG xóa các listener khác
        NetworkClient.itemsListener = response -> Platform.runLater(() -> {
            String action = getString(response, "action");
            switch (action) {
                case "ITEMS_LIST" -> handleItemsList(response);
                case "AUCTIONS_LIST" -> handleAuctionsList(response);
                case "UPDATE_PRICE", "NEW_BID" -> handlePriceUpdate(response);
                case "AUCTION_END" -> handleAuctionEnd(response);
                case "NEW_AUCTION_POSTED" -> handleNewAuctionPosted(response);
                case "GLOBAL_NOTIFY" -> handleGlobalNotify(response);
                case "ERROR" -> showInfoToast("Loi server: " + getString(response, "message"));
            }
        });
    }

    // ─── Network Handlers ─────────────────────────────────────────────────────

    private void handleItemsList(JsonObject response) {
        JsonArray itemsArray = getArray(response, "items");
        if (itemsArray == null) itemsArray = getArray(response, "data");
        if (itemsArray == null) return;

        for (JsonElement el : itemsArray) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String rawId = coalesce(getString(obj, "id"), getString(obj, "itemId"), "");

            ProductData existing = findProductByRawId(rawId);
            if (existing != null) {
                ProductData incoming = parseProductFromItems(obj);
                // Update basic fields but keep auction status
                if (existing.imagePaths.isEmpty()) existing.imagePaths = incoming.imagePaths;
                if (existing.description.isBlank() || existing.description.startsWith("Chua co"))
                    existing.description = incoming.description;
                if (existing.name.startsWith("San pham #")) existing.name = incoming.name;
            } else {
                ProductData incoming = parseProductFromItems(obj);
                incoming.serverStatus = "NOT_AUCTION";
                allProducts.add(incoming);
            }
        }
        applyAllFilters();
        mergeFilterOptionsFromProducts();
    }

    private void handleAuctionsList(JsonObject response) {
        JsonArray auctionsArray = getArray(response, "auctions");
        if (auctionsArray == null) auctionsArray = getArray(response, "data");
        if (auctionsArray == null) return;

        for (JsonElement el : auctionsArray) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            ProductData incoming = parseProductFromAuctions(obj);

            ProductData existing = findProductByAuctionId(incoming.auctionId);
            if (existing == null) {
                // Try to find by item ID
                existing = findProductById(incoming.id);
            }

            if (existing != null) {
                // Merge auction data into existing item
                existing.auctionId = incoming.auctionId;
                existing.currentPrice = incoming.currentPrice;
                existing.serverStatus = incoming.serverStatus;
                existing.bidCount = incoming.bidCount;
                existing.participants = incoming.participants;
                existing.topBidder = incoming.topBidder;
                existing.startTime = incoming.startTime;
                existing.endTime = incoming.endTime;
                if (!incoming.bidHistory.isEmpty()) existing.bidHistory = incoming.bidHistory;
            } else {
                allProducts.add(incoming);
            }
        }
        applyAllFilters();
        refreshVisibleDetail();
    }

    private void handlePriceUpdate(JsonObject response) {
        ProductData product = resolveProduct(response);
        if (product == null) return;

        long newPrice = getLong(response, "newPrice", safeParseLong(product.currentPrice));
        product.currentPrice = String.valueOf(newPrice);
        product.topBidder = getString(response, "winnerId", getString(response, "highestBidder", product.topBidder));
        product.bidCount = Math.max(product.bidCount + 1, getInt(response, "bidCount", product.bidCount + 1));
        product.participants = Math.max(product.participants, getInt(response, "participants", product.participants));
        product.bidHistory.add(0, new BidEntry(product.topBidder, newPrice, LocalDateTime.now()));
        
        if (product.serverStatus.isBlank() || "UPCOMING".equals(product.getStatusType())) 
            product.serverStatus = "RUNNING";

        applyAllFilters();
        refreshWatchlistPane();
        refreshVisibleDetail();
    }

    private void handleAuctionEnd(JsonObject response) {
        ProductData product = resolveProduct(response);
        if (product == null) return;

        product.currentPrice = String.valueOf(getLong(response, "finalPrice", safeParseLong(product.currentPrice)));
        product.topBidder = getString(response, "winnerId", getString(response, "winner", product.topBidder));
        product.serverStatus = "FINISHED";
        product.endTime = LocalDateTime.now();

        applyAllFilters();
        refreshWatchlistPane();
        refreshVisibleDetail();
    }

    private void handleNewAuctionPosted(JsonObject response) {
        JsonObject data = getObject(response, "data");
        if (data == null) return;

        ProductData incoming = parseProductFromAuctions(data);
        ProductData existing = findProductByAuctionId(incoming.auctionId);
        if (existing != null) allProducts.remove(existing);
        
        allProducts.add(0, incoming);
        applyAllFilters();
        showInfoToast("Co phien dau gia moi vua duoc dang len san.");
    }

    private void handleGlobalNotify(JsonObject response) {
        String msg = getString(response, "message");
        if (!msg.isBlank()) showInfoToast(msg);
        unreadNotificationCount++;
        if (homeNavbarController != null) homeNavbarController.setNotificationCount(unreadNotificationCount);
    }

    // ─── Parsing Helpers ──────────────────────────────────────────────────────

    private ProductData parseProductFromItems(JsonObject o) {
        ProductData p = new ProductData();
        String rawId = coalesce(getString(o, "id"), getString(o, "itemId"), "0");
        p.id = parseIdSafe(rawId);
        p.name = coalesce(getString(o, "name"), getString(o, "itemName"), "San pham #" + p.id);
        p.category = coalesce(getString(o, "item_type"), getString(o, "category"), "Khac");
        p.description = coalesce(getString(o, "description"), "Chua co mo ta.");
        p.seller = coalesce(getString(o, "seller"), "Chua ro");
        long startP = getLong(o, "startingPrice", getLong(o, "price", 0L));
        p.startPrice = String.valueOf(startP);
        p.currentPrice = p.startPrice;
        p.imagePaths = parseImagePaths(o);
        p.specs = parseSpecs(o);
        return p;
    }

    private ProductData parseProductFromAuctions(JsonObject o) {
        ProductData p = new ProductData();
        p.auctionId = getString(o, "id", getString(o, "auctionId", ""));
        
        JsonObject itemObj = getObject(o, "item");
        if (itemObj != null) {
            p.id = parseIdSafe(coalesce(getString(itemObj, "id"), getString(itemObj, "itemId"), "0"));
            p.name = coalesce(getString(itemObj, "name"), "San pham #" + p.id);
            p.category = coalesce(getString(itemObj, "item_type"), "Khac");
            p.description = coalesce(getString(itemObj, "description"), "Chua co mo ta.");
            p.imagePaths = parseImagePaths(itemObj);
            p.startPrice = String.valueOf(getLong(itemObj, "startingPrice", 0L));
        } else {
            p.id = parseIdSafe(getString(o, "itemId", "0"));
            p.name = getString(o, "itemName", "San pham #" + p.id);
        }

        p.currentPrice = String.valueOf(getLong(o, "currentPrice", safeParseLong(p.startPrice)));
        p.serverStatus = getString(o, "status", "");
        p.bidCount = getInt(o, "bidCount", 0);
        p.participants = getInt(o, "participants", 0);
        p.topBidder = getString(o, "winnerId", getString(o, "highestBidder", "---"));
        p.startTime = parseDateTime(o, "startTime", LocalDateTime.now());
        p.endTime = parseDateTime(o, "endTime", LocalDateTime.now().plusHours(1));
        p.bidHistory = parseBidHistory(o);
        
        return p;
    }

    private List<String> parseImagePaths(JsonObject obj) {
        List<String> paths = new ArrayList<>();
        JsonArray arr = getArray(obj, "images");
        if (arr != null) {
            for (JsonElement el : arr) if (el.isJsonPrimitive()) paths.add(el.getAsString());
        }
        String single = getString(obj, "image", getString(obj, "imageUrl", ""));
        if (!single.isBlank()) paths.add(single);
        return paths.stream().distinct().collect(Collectors.toList());
    }

    private Map<String, String> parseSpecs(JsonObject obj) {
        Map<String, String> specs = new LinkedHashMap<>();
        JsonObject s = getObject(obj, "specs");
        if (s != null) {
            for (Map.Entry<String, JsonElement> entry : s.entrySet()) 
                specs.put(entry.getKey(), entry.getValue().getAsString());
        }
        return specs;
    }

    private List<BidEntry> parseBidHistory(JsonObject obj) {
        List<BidEntry> history = new ArrayList<>();
        JsonArray arr = getArray(obj, "bidHistory");
        if (arr != null) {
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject b = el.getAsJsonObject();
                history.add(new BidEntry(getString(b, "bidder"), getLong(b, "price", 0L), parseDateTime(b, "time", LocalDateTime.now())));
            }
        }
        return history;
    }

    // ─── Filter / Render ──────────────────────────────────────────────────────

    private void applyAllFilters() {
        String query = currentSearchQuery.toLowerCase();
        filteredProducts = allProducts.stream().filter(p -> {
            if (!query.isBlank() && !p.name.toLowerCase().contains(query) && !p.seller.toLowerCase().contains(query)) return false;
            if (!"Tat ca".equals(currentCategory) && !currentCategory.equalsIgnoreCase(p.category)) return false;
            if (!"Tat ca".equals(currentStatusFilter) && !currentStatusFilter.equalsIgnoreCase(p.getStatusType())) return false;
            if (!matchesPrice(p, currentPriceRange)) return false;
            return true;
        }).collect(Collectors.toList());
        renderHomePage();
    }

    private boolean matchesPrice(ProductData p, String range) {
        if ("Tat ca".equals(range)) return true;
        long price = safeParseLong(p.currentPrice);
        return switch (range) {
            case "< 1.000.000" -> price < 1_000_000L;
            case "1-5 trieu" -> price >= 1_000_000L && price <= 5_000_000L;
            case "5-20 trieu" -> price > 5_000_000L && price <= 20_000_000L;
            case "20-50 trieu" -> price > 20_000_000L && price <= 50_000_000L;
            case "> 50 trieu" -> price > 50_000_000L;
            default -> true;
        };
    }

    private void renderHomePage() {
        if (runningProductsPane == null) return;
        runningProductsPane.getChildren().clear();
        upcomingProductsPane.getChildren().clear();
        finishedProductsPane.getChildren().clear();

        List<ProductData> pageItems = filteredProducts.stream()
                .skip((long) currentPage * PAGE_SIZE).limit(PAGE_SIZE).collect(Collectors.toList());

        for (ProductData p : pageItems) {
            VBox card = createSmallCard(p);
            String status = p.getStatusType();
            if ("RUNNING".equals(status)) runningProductsPane.getChildren().add(card);
            else if ("UPCOMING".equals(status)) upcomingProductsPane.getChildren().add(card);
            else finishedProductsPane.getChildren().add(card);
        }

        lblRunningEmpty.setVisible(runningProductsPane.getChildren().isEmpty());
        lblUpcomingEmpty.setVisible(upcomingProductsPane.getChildren().isEmpty());
        lblFinishedEmpty.setVisible(finishedProductsPane.getChildren().isEmpty());

        int total = (int) Math.ceil((double) filteredProducts.size() / PAGE_SIZE);
        lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + Math.max(1, total));
        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= total - 1);
    }

    private VBox createSmallCard(ProductData data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidding/client/scene/auction_card.fxml"));
            VBox card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.configure(data, watchedProductIds.contains(data.id), () -> onProductClicked(data), id -> {
                if (watchedProductIds.contains(id)) watchedProductIds.remove(id); else watchedProductIds.add(id);
                controller.updateWatchButton(watchedProductIds.contains(id));
                refreshWatchlistPane();
            });
            cardMap.put(data.id, controller);
            return card;
        } catch (IOException e) { return new VBox(new Label("Loi card")); }
    }

    private void onProductClicked(ProductData p) {
        selectedProduct = p;
        if (detailPageController != null) detailPageController.setProductData(p);
        mainPage.setVisible(false);
        watchlistPage.setVisible(false);
        detailPage.setVisible(true);
    }

    private void refreshVisibleDetail() {
        if (selectedProduct != null && detailPage.isVisible()) {
            detailPageController.setProductData(selectedProduct);
        }
    }

    private void refreshWatchlistPane() {
        if (watchlistPane == null) return;
        watchlistPane.getChildren().clear();
        List<ProductData> watched = allProducts.stream().filter(p -> watchedProductIds.contains(p.id)).collect(Collectors.toList());
        lblWatchlistEmpty.setVisible(watched.isEmpty());
        for (ProductData p : watched) watchlistPane.getChildren().add(createSmallCard(p));
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    @FXML void handleSearch(ActionEvent event) { currentSearchQuery = searchField.getText(); currentPage = 0; applyAllFilters(); }
    @FXML void handleFilter(ActionEvent event) {
        currentCategory = filterCategory.getValue();
        currentStatusFilter = filterStatus.getValue();
        currentPriceRange = filterPrice.getValue();
        currentPage = 0;
        applyAllFilters();
    }
    @FXML void handleClearFilter(ActionEvent event) {
        searchField.clear(); currentSearchQuery = "";
        filterCategory.getSelectionModel().selectFirst();
        filterStatus.getSelectionModel().selectFirst();
        filterPrice.getSelectionModel().selectFirst();
        currentPage = 0; applyAllFilters();
    }
    @FXML void handlePrevPage(ActionEvent event) { if (currentPage > 0) { currentPage--; renderHomePage(); } }
    @FXML void handleNextPage(ActionEvent event) { if ((currentPage + 1) * PAGE_SIZE < filteredProducts.size()) { currentPage++; renderHomePage(); } }

    @FXML void toggleMenu(ActionEvent event) { sideMenu.setTranslateX(sideMenu.getTranslateX() == 0 ? -300 : 0); dimOverlay.setVisible(!dimOverlay.isVisible()); }
    @FXML void toggleAccountInfo(MouseEvent event) { accountPopup.setVisible(!accountPopup.isVisible()); dimOverlay.setVisible(accountPopup.isVisible()); }
    @FXML void closeAll(MouseEvent event) { sideMenu.setTranslateX(-300); accountPopup.setVisible(false); dimOverlay.setVisible(false); }

    @FXML void menuGoHome(ActionEvent event) { mainPage.setVisible(true); watchlistPage.setVisible(false); detailPage.setVisible(false); closeAll(null); }
    @FXML void menuGoWatchlist(ActionEvent event) { mainPage.setVisible(false); watchlistPage.setVisible(true); detailPage.setVisible(false); refreshWatchlistPane(); closeAll(null); }
    @FXML void menuGoNotifications(ActionEvent event) { openScene("BidderNotifications.fxml", "Thong bao"); }
    @FXML void menuGoMyBids(ActionEvent event) { openScene("SceneMyBids.fxml", "Dau gia cua toi"); }
    @FXML void menuGoHistory1(ActionEvent event) { openScene("SceneLSGD.fxml", "Lich su giao dich"); }
    @FXML void handleOpenAutoBid(ActionEvent event) { openScene("SceneAutoBid.fxml", "Tu dong tra gia"); }
    @FXML void switchToSeller(ActionEvent event) { openScene("SceneSeller1.fxml", "Nguoi ban"); }
    @FXML void handleLogout(ActionEvent event) { openScene("Scene1.fxml", "Dang nhap"); }

    @FXML void handleChangeAvatar(ActionEvent event) {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (f != null) {
            Image img = new Image(f.toURI().toString());
            imgAvatar.setImage(img); imgAvatarPopup.setImage(img);
            lblAvatarFallback.setVisible(false); lblAvatarPopupFallback.setVisible(false);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            String time = LocalTime.now().format(TIME_FMT);
            lblClock.setText(time);
            if (lblClockWatch != null) lblClockWatch.setText(time);
        }));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void initFilterComboBoxes() {
        filterCategory.getItems().setAll("Tat ca", "Khac"); filterCategory.getSelectionModel().selectFirst();
        filterStatus.getItems().setAll("Tat ca", "RUNNING", "UPCOMING", "FINISHED"); filterStatus.getSelectionModel().selectFirst();
        filterPrice.getItems().setAll("Tat ca", "< 1.000.000", "1-5 trieu", "5-20 trieu", "20-50 trieu", "> 50 trieu"); filterPrice.getSelectionModel().selectFirst();
        sortCombo.getItems().setAll("Moi nhat", "Gia tang dan", "Gia giam dan"); sortCombo.getSelectionModel().selectFirst();
    }

    private void mergeFilterOptionsFromProducts() {
        Set<String> cats = allProducts.stream().map(p -> p.category).collect(Collectors.toSet());
        List<String> items = new ArrayList<>(); items.add("Tat ca"); items.addAll(cats);
        filterCategory.getItems().setAll(items);
    }

    private void clipAvatars() {
        Circle clip = new Circle(26, 26, 26); imgAvatar.setClip(clip);
        Circle clip2 = new Circle(30, 30, 30); imgAvatarPopup.setClip(clip2);
    }

    private void initNavBars() {
        if (homeNavbarController != null) {
            homeNavbarController.configure("home", "Bidder", "5.000.000 d", unreadNotificationCount, true);
            homeNavbarController.setCallbacks(
                this::toggleMenu,
                this::menuGoHome,
                this::menuGoWatchlist,
                this::toggleAccountInfo,
                this::menuGoNotifications,
                q -> { currentSearchQuery = q; currentPage = 0; applyAllFilters(); },
                this::handleLogout,
                this::toggleAccountInfo
            );
        }
        if (watchNavbarController != null) {
            watchNavbarController.configure("watchlist", "Bidder", "5.000.000 d", unreadNotificationCount, false);
            watchNavbarController.setCallbacks(
                this::toggleMenu,
                this::menuGoHome,
                this::menuGoWatchlist,
                this::toggleAccountInfo,
                this::menuGoNotifications,
                q -> {},
                this::handleLogout,
                this::toggleAccountInfo
            );
        }
    }

    private void toggleMenu() { toggleMenu(null); }
    private void toggleAccountInfo() { toggleAccountInfo(null); }
    private void menuGoHome() { menuGoHome(null); }
    private void menuGoWatchlist() { menuGoWatchlist(null); }
    private void menuGoNotifications() { menuGoNotifications(null); }
    private void handleLogout() { handleLogout(null); }

    private void openScene(String fxml, String title) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(new FXMLLoader(getClass().getResource("/com/bidding/client/scene/" + fxml)).load()));
            stage.setTitle(title);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showInfoToast(String msg) { if (toastManager != null) toastManager.show(msg, ToastManager.ToastType.INFO); }

    private ProductData findProductById(int id) { return allProducts.stream().filter(p -> p.id == id).findFirst().orElse(null); }
    private ProductData findProductByAuctionId(String aid) { return allProducts.stream().filter(p -> aid.equals(p.auctionId)).findFirst().orElse(null); }
    private ProductData findProductByRawId(String rid) { 
        int nid = parseIdSafe(rid);
        return allProducts.stream().filter(p -> rid.equals(String.valueOf(p.id)) || p.id == nid || rid.equals(p.auctionId)).findFirst().orElse(null);
    }
    private ProductData resolveProduct(JsonObject o) {
        String aid = getString(o, "auctionId");
        ProductData p = findProductByAuctionId(aid);
        if (p != null) return p;
        int iid = getInt(o, "itemId", -1);
        return findProductById(iid);
    }

    private String getString(JsonObject o, String k) { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : ""; }
    private String getString(JsonObject o, String k, String f) { String s = getString(o, k); return s.isBlank() ? f : s; }
    private int getInt(JsonObject o, String k, int f) { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsInt() : f; }
    private long getLong(JsonObject o, String k, long f) {
        if (!o.has(k) || o.get(k).isJsonNull()) return f;
        try { return o.get(k).getAsLong(); } catch (Exception e) { return f; }
    }
    private JsonArray getArray(JsonObject o, String k) { return (o.has(k) && o.get(k).isJsonArray()) ? o.getAsJsonArray(k) : null; }
    private JsonObject getObject(JsonObject o, String k) { return (o.has(k) && o.get(k).isJsonObject()) ? o.getAsJsonObject(k) : null; }
    private String coalesce(String... v) { for (String s : v) if (s != null && !s.isBlank()) return s; return ""; }
    private long safeParseLong(String s) { try { return Long.parseLong(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0L; } }
    private int parseIdSafe(String s) { try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Exception e) { return 0; } }
    
    private LocalDateTime parseDateTime(JsonObject o, String k, LocalDateTime f) {
        if (!o.has(k) || o.get(k).isJsonNull()) return f;
        try { return LocalDateTime.parse(o.get(k).getAsString(), DateTimeFormatter.ISO_DATE_TIME); } catch (Exception e) { return f; }
    }
}
