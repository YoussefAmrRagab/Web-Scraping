package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final String BASE_URL = "https://www.carrefouregypt.com/mafegy/ar/c/";
    private static final String CATEGORY = "FEGY1770000";
    private static final String FILTER = "?currentPage=0&filter=&maxPrice=%d&minPrice=%d&nextPageOffset=0&pageSize=60&sortBy=relevance";
    private static final long TIMEOUT_DURATION_MILLIS = 60000; // 60 second

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        int minPrice = 0;
        int maxPrice = 10;

        while (true) {
            try {
                List<Map<String, Object>> productsList = processProducts(minPrice, maxPrice);
                if (productsList.isEmpty()) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime >= TIMEOUT_DURATION_MILLIS) {
                        System.out.println("Timeout reached. Exiting...");
                        break;
                    }
                }

                for (Map<String, Object> product : productsList) {
                    System.out.println(toJson(product));
                }

                minPrice = maxPrice;
                maxPrice = maxPrice + 5;
            } catch (IOException e) {
                e.fillInStackTrace();
            }
        }
    }

    private static String toJson(Map<String, Object> product) {
        return "{\n\"product\": " + "\"" + product.get("product") + "\"" +
                ",\n\"image\": " + "\"" + product.get("image") + "\"" +
                ",\n\"price\": " + "\"" + product.get("price") + "\"" +
                ",\n\"country\": " + "\"" + product.get("country") + "\"" + "\n},\n";
    }

    private static List<Map<String, Object>> processProducts(int minPrice, int maxPrice) throws IOException {
        String url = String.format(BASE_URL + CATEGORY + FILTER, maxPrice, minPrice);
        Document document = Jsoup.connect(url).timeout((int) TIMEOUT_DURATION_MILLIS).get();
        Element scriptTag = document.select("#__NEXT_DATA__").first();
        if (scriptTag == null) {
            throw new RuntimeException("Script tag not found");
        }
        String scriptContent = scriptTag.html();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(scriptContent);

        return extractProductInfo(jsonNode);
    }

    private static List<Map<String, Object>> extractProductInfo(JsonNode jsonNode) {
        List<Map<String, Object>> productsList = new ArrayList<>();
        List<JsonNode> productList = jsonNode.findValues("products");

        if (!productList.isEmpty()) {
            productList.get(1).forEach(product -> {
                Map<String, Object> productInfo = new HashMap<>();
                productInfo.put("product", product.get("image").get("altText").asText());
                productInfo.put("image", product.findValue("newProductCardImage").asText());
                productInfo.put("price", product.findValue("originalPrice").asText());
                List<JsonNode> promoBadges = product.findValues("promoBadges");
                String country = promoBadges.get(0).findValue("text") == null ? "" : promoBadges.get(0).findValue("text").get("boldText").asText().equals("صنع في مصر") ? "صنع في مصر" : "";
                productInfo.put("country", country);
                productsList.add(productInfo);
            });
        }

        return productsList;
    }
}
