$file = 'c:\Users\PC\IdeaProjects\bidding_system\Scene\src\main\java\com\bidding\client\scene\SceneBidder1.java'
$content = [System.IO.File]::ReadAllText($file)

$newMethod = @"

    /**
     * Xu ly ITEMS_LIST tu GET_ALL_ITEMS (toan bo items tren san).
     * Items chua co trong allProducts -> them voi status NOT_AUCTION
     * Items da co (dang/da dau gia) -> chi cap nhat fields phu, giu nguyen auction status.
     */
    private void handleItemsList(JsonObject response) {
        JsonArray itemsArray = getArray(response, "items");
        if (itemsArray == null) itemsArray = getArray(response, "data");
        if (itemsArray == null) {
            showInfoToast("Khong co du lieu san pham tu server.");
            return;
        }

        for (JsonElement el : itemsArray) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            String rawId = coalesce(getString(obj, "id"), getString(obj, "itemId"), "");

            ProductData existing = findProductByRawId(rawId);
            if (existing != null) {
                // Product da co (dang/da dau gia) -> chi cap nhat fields phu
                ProductData incoming = parseProductFromItems(obj);
                if (existing.imagePaths.isEmpty() && !incoming.imagePaths.isEmpty())
                    existing.imagePaths = incoming.imagePaths;
                if ("Chua co mo ta tu server.".equals(existing.description) || existing.description.isBlank())
                    existing.description = incoming.description;
                if ((existing.name == null || existing.name.startsWith("San pham #")) && !incoming.name.startsWith("San pham #"))
                    existing.name = incoming.name;
                // Khong overwrite: serverStatus, currentPrice, startTime, endTime, auctionId
            } else {
                // Item chua duoc dau gia -> them moi voi NOT_AUCTION
                ProductData incoming = parseProductFromItems(obj);
                incoming.serverStatus = "NOT_AUCTION";
                allProducts.add(incoming);
            }
        }

        mergeFilterOptionsFromProducts();
        applyAllFilters();
        refreshWatchlistPane();
        refreshVisibleDetail();

        if (allProducts.isEmpty())
            showInfoToast("Server da ket noi nhung chua co du lieu san pham nao.");
    }

"@

# Use regex to replace the handleItemsList method
$pattern = '(?s)(\s*/\*\*\s*\n\s*\* FIX: X[^*]*?\*/\s*\n\s*private void handleItemsList\(JsonObject response\) \{.*?\n\s*\})'
$newContent = [regex]::Replace($content, $pattern, $newMethod)

if ($newContent -eq $content) {
    Write-Host "PATTERN NOT MATCHED - trying alternative"
    # Try simpler pattern
    $pattern2 = '(?s)(    /\*\*\r?\n.*?handleItemsList.*?\n    \})'
    $newContent = [regex]::Replace($content, $pattern2, $newMethod)
}

if ($newContent -eq $content) {
    Write-Host "STILL NOT MATCHED"
} else {
    [System.IO.File]::WriteAllText($file, $newContent)
    Write-Host "SUCCESS - file updated"
}
