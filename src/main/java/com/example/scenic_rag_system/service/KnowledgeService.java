package com.example.scenic_rag_system.service;

import com.example.scenic_rag_system.entity.*;
import com.example.scenic_rag_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识库CRUD服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeService {

    private final ScenicAreaRepository scenicAreaRepo;
    private final AttractionRepository attractionRepo;
    private final FoodShopRepository foodShopRepo;
    private final HotelRepository hotelRepo;
    private final LocalProductRepository localProductRepo;
    private final TicketRepository ticketRepo;
    private final FixedQARepository fixedQARepo;
    private final CustomerServiceRepository customerServiceRepo;

    private final EmbeddingService embeddingService;
    private final ChromaVectorService chromaVectorService;

    /**
     * 获取指定知识库的所有数据
     */
    public List<?> getAll(String type) {
        return switch (type) {
            case "scenic" -> scenicAreaRepo.findAll();
            case "attraction" -> attractionRepo.findAll();
            case "food" -> foodShopRepo.findAll();
            case "hotel" -> hotelRepo.findAll();
            case "product" -> localProductRepo.findAll();
            case "ticket" -> ticketRepo.findAll();
            case "fixed_qa" -> fixedQARepo.findAll();
            case "customer_service" -> customerServiceRepo.findAll();
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    /**
     * 添加单条知识库数据
     */
    @Transactional
    public Object add(String type, Map<String, Object> data) {
        Object entity = switch (type) {
            case "scenic" -> {
                ScenicArea e = new ScenicArea();
                mapToEntity(e, data, "image", "name", "type", "level", "longitude", "latitude", "address", "description");
                yield scenicAreaRepo.save(e);
            }
            case "attraction" -> {
                Attraction e = new Attraction();
                mapToEntity(e, data, "image", "name", "tags", "relatedScenic", "briefIntro", "address", "longitude", "latitude", "description");
                Attraction saved = attractionRepo.save(e);
                try { syncAttractionToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(attraction): {}", ex.getMessage());
                }
                yield saved;
            }
            case "food" -> {
                FoodShop e = new FoodShop();
                e.setType("美食");
                mapToEntity(e, data, "image", "name", "relatedScenic", "longitude", "latitude", "address", "tags");
                FoodShop saved = foodShopRepo.save(e);
                try { syncFoodToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(food): {}", ex.getMessage());
                }
                yield saved;
            }
            case "hotel" -> {
                Hotel e = new Hotel();
                mapToEntity(e, data, "image", "name", "level", "price", "address", "phone", "facilities");
                Hotel saved = hotelRepo.save(e);
                try { syncHotelToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(hotel): {}", ex.getMessage());
                }
                yield saved;
            }
            case "product" -> {
                LocalProduct e = new LocalProduct();
                mapToEntity(e, data, "image", "name", "category", "price", "description", "origin");
                LocalProduct saved = localProductRepo.save(e);
                try { syncProductToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(product): {}", ex.getMessage());
                }
                yield saved;
            }
            case "ticket" -> {
                Ticket e = new Ticket();
                mapToEntity(e, data, "image", "name", "price", "location", "description", "tags");
                Ticket saved = ticketRepo.save(e);
                try { syncTicketToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(ticket): {}", ex.getMessage());
                }
                yield saved;
            }
            case "fixed_qa" -> {
                FixedQA e = new FixedQA();
                mapToEntity(e, data, "question", "answer");
                FixedQA saved = fixedQARepo.save(e);
                try { syncFixedQAToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(fixed_qa): {}", ex.getMessage());
                }
                yield saved;
            }
            case "customer_service" -> {
                CustomerService e = new CustomerService();
                mapToEntity(e, data, "keywords", "response");
                CustomerService saved = customerServiceRepo.save(e);
                try { syncCustomerServiceToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(customer_service): {}", ex.getMessage());
                }
                yield saved;
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
        return entity;
    }

    /**
     * 更新知识库数据
     */
    @Transactional
    public Object update(String type, Long id, Map<String, Object> data) {
        return switch (type) {
            case "scenic" -> {
                ScenicArea e = scenicAreaRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "image", "name", "type", "level", "longitude", "latitude", "address", "description");
                yield scenicAreaRepo.save(e);
            }
            case "attraction" -> {
                Attraction e = attractionRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "image", "name", "tags", "relatedScenic", "briefIntro", "address", "longitude", "latitude", "description");
                Attraction saved = attractionRepo.save(e);
                syncAttractionToVector(saved);
                yield saved;
            }
            case "food" -> {
                FoodShop e = foodShopRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "image", "name", "relatedScenic", "longitude", "latitude", "address", "tags");
                FoodShop saved = foodShopRepo.save(e);
                syncFoodToVector(saved);
                yield saved;
            }
            case "hotel" -> {
                Hotel e = hotelRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "image", "name", "level", "price", "address", "phone", "facilities");
                Hotel saved = hotelRepo.save(e);
                try { syncHotelToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(hotel): {}", ex.getMessage());
                }
                yield saved;
            }
            case "product" -> {
                LocalProduct e = localProductRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "image", "name", "category", "price", "description", "origin");
                LocalProduct saved = localProductRepo.save(e);
                try { syncProductToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(product): {}", ex.getMessage());
                }
                yield saved;
            }
            case "ticket" -> {
                Ticket e = ticketRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "image", "name", "price", "location", "description", "tags");
                Ticket saved = ticketRepo.save(e);
                try { syncTicketToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(ticket): {}", ex.getMessage());
                }
                yield saved;
            }
            case "fixed_qa" -> {
                FixedQA e = fixedQARepo.findById(id).orElseThrow();
                mapToEntity(e, data, "question", "answer");
                FixedQA saved = fixedQARepo.save(e);
                syncFixedQAToVector(saved);
                yield saved;
            }
            case "customer_service" -> {
                CustomerService e = customerServiceRepo.findById(id).orElseThrow();
                mapToEntity(e, data, "keywords", "response");
                CustomerService saved = customerServiceRepo.save(e);
                try { syncCustomerServiceToVector(saved); } catch (Exception ex) {
                    log.warn("向量同步失败(customer_service): {}", ex.getMessage());
                }
                yield saved;
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    /**
     * 删除知识库数据
     */
    @Transactional
    public void delete(String type, Long id) {
        switch (type) {
            case "scenic" -> scenicAreaRepo.deleteById(id);
            case "attraction" -> {
                attractionRepo.deleteById(id);
                chromaVectorService.deleteDocuments("attraction", List.of("attr_" + id));
            }
            case "food" -> {
                foodShopRepo.deleteById(id);
                chromaVectorService.deleteDocuments("food", List.of("food_" + id));
            }
            case "hotel" -> {
                hotelRepo.deleteById(id);
                chromaVectorService.deleteDocuments("hotel", List.of("hotel_" + id));
            }
            case "product" -> {
                localProductRepo.deleteById(id);
                chromaVectorService.deleteDocuments("product", List.of("prod_" + id));
            }
            case "ticket" -> {
                ticketRepo.deleteById(id);
                chromaVectorService.deleteDocuments("ticket", List.of("ticket_" + id));
            }
            case "fixed_qa" -> {
                fixedQARepo.deleteById(id);
                chromaVectorService.deleteDocuments("fixed_qa", List.of("qa_" + id));
            }
            case "customer_service" -> {
                customerServiceRepo.deleteById(id);
                chromaVectorService.deleteDocuments("customer_service", List.of("cs_" + id));
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    // ========== 向量同步 ==========

    /**
     * 同步景点数据到向量库
     */
    public void syncAttractionToVector(Attraction a) {
        String ragText = String.join(" ",
                nullToEmpty(a.getName()),
                nullToEmpty(a.getTags()),
                nullToEmpty(a.getRelatedScenic()),
                nullToEmpty(a.getBriefIntro()));
        if (ragText.isBlank()) return;

        var embedding = embeddingService.embedRagText(
                a.getName(), a.getTags(), a.getRelatedScenic(), a.getBriefIntro());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", a.getId().toString());
        metadata.put("type", "attraction");
        metadata.put("name", a.getName());
        metadata.put("image", nullToEmpty(a.getImage()));

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("attr_" + a.getId())
                .embedding(embedding)
                .text(ragText)
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("attraction", List.of(doc));
    }

    /**
     * 同步美食数据到向量库
     */
    public void syncFoodToVector(FoodShop f) {
        String ragText = String.join(" ",
                nullToEmpty(f.getName()),
                "美食",
                nullToEmpty(f.getRelatedScenic()),
                nullToEmpty(f.getTags()));
        if (ragText.isBlank()) return;

        var embedding = embeddingService.embedRagText(
                f.getName(), "美食", f.getRelatedScenic(), f.getTags());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", f.getId().toString());
        metadata.put("type", "food");
        metadata.put("name", f.getName());
        metadata.put("image", nullToEmpty(f.getImage()));

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("food_" + f.getId())
                .embedding(embedding)
                .text(ragText)
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("food", List.of(doc));
    }

    /**
     * 同步酒店数据到向量库
     */
    public void syncHotelToVector(Hotel h) {
        String ragText = String.join(" ",
                nullToEmpty(h.getName()),
                nullToEmpty(h.getLevel()),
                nullToEmpty(h.getPrice()),
                nullToEmpty(h.getAddress()),
                nullToEmpty(h.getFacilities()));
        if (ragText.isBlank()) return;

        var embedding = embeddingService.embedRagText(
                h.getName(), h.getLevel(), h.getAddress(), h.getFacilities());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", h.getId().toString());
        metadata.put("type", "hotel");
        metadata.put("name", h.getName());

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("hotel_" + h.getId())
                .embedding(embedding)
                .text(ragText)
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("hotel", List.of(doc));
    }

    /**
     * 同步特产数据到向量库
     */
    public void syncProductToVector(LocalProduct p) {
        String ragText = String.join(" ",
                nullToEmpty(p.getName()),
                nullToEmpty(p.getCategory()),
                nullToEmpty(p.getDescription()),
                nullToEmpty(p.getOrigin()));
        if (ragText.isBlank()) return;

        var embedding = embeddingService.embedRagText(
                p.getName(), p.getCategory(), p.getDescription(), p.getOrigin());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", p.getId().toString());
        metadata.put("type", "product");
        metadata.put("name", p.getName());

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("prod_" + p.getId())
                .embedding(embedding)
                .text(ragText)
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("product", List.of(doc));
    }

    /**
     * 同步门票数据到向量库
     */
    public void syncTicketToVector(Ticket t) {
        String ragText = String.join(" ",
                nullToEmpty(t.getName()),
                nullToEmpty(t.getLocation()),
                nullToEmpty(t.getDescription()),
                nullToEmpty(t.getTags()));
        if (ragText.isBlank()) return;

        var embedding = embeddingService.embedRagText(
                t.getName(), t.getLocation(), t.getDescription(), t.getTags());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", t.getId().toString());
        metadata.put("type", "ticket");
        metadata.put("name", t.getName());

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("ticket_" + t.getId())
                .embedding(embedding)
                .text(ragText)
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("ticket", List.of(doc));
    }

    /**
     * 同步固定问答到向量库
     */
    public void syncFixedQAToVector(FixedQA qa) {
        var embedding = embeddingService.embed(qa.getQuestion());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", qa.getId().toString());
        metadata.put("type", "fixed_qa");
        metadata.put("answer", qa.getAnswer());

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("qa_" + qa.getId())
                .embedding(embedding)
                .text(qa.getQuestion())
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("fixed_qa", List.of(doc));
    }

    /**
     * 初始化所有知识库数据到向量库（首次启动时调用）
     */
    @Transactional
    public void syncAllToVector() {
        // 同步景点
        List<Attraction> attractions = attractionRepo.findAll();
        for (Attraction a : attractions) {
            syncAttractionToVector(a);
        }

        // 同步美食
        List<FoodShop> foods = foodShopRepo.findAll();
        for (FoodShop f : foods) {
            syncFoodToVector(f);
        }

        // 同步酒店
        List<Hotel> hotels = hotelRepo.findAll();
        for (Hotel h : hotels) {
            syncHotelToVector(h);
        }

        // 同步特产
        List<LocalProduct> products = localProductRepo.findAll();
        for (LocalProduct p : products) {
            syncProductToVector(p);
        }

        // 同步门票
        List<Ticket> tickets = ticketRepo.findAll();
        for (Ticket t : tickets) {
            syncTicketToVector(t);
        }

        // 同步客服问答
        List<CustomerService> css = customerServiceRepo.findAll();
        for (CustomerService cs : css) {
            syncCustomerServiceToVector(cs);
        }

        // 同步固定问答
        List<FixedQA> qas = fixedQARepo.findAll();
        for (FixedQA qa : qas) {
            syncFixedQAToVector(qa);
        }

        log.info("Synced {} attractions, {} foods, {} hotels, {} products, {} tickets, {} fixed QAs, {} customer services to vector store",
                attractions.size(), foods.size(), hotels.size(), products.size(), tickets.size(), qas.size(), css.size());
    }

    /**
     * 同步客服问答数据到向量库
     */
    public void syncCustomerServiceToVector(CustomerService cs) {
        String ragText = String.join(" ",
                nullToEmpty(cs.getKeywords()),
                nullToEmpty(cs.getResponse()));
        if (ragText.isBlank()) return;

        var embedding = embeddingService.embedRagText(
                cs.getKeywords(), cs.getResponse());
        if (embedding.isEmpty()) return;

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", cs.getId().toString());
        metadata.put("type", "customer_service");

        var doc = com.example.scenic_rag_system.dto.VectorDocument.builder()
                .id("cs_" + cs.getId())
                .embedding(embedding)
                .text(ragText)
                .metadata(metadata)
                .build();

        chromaVectorService.addDocuments("customer_service", List.of(doc));
    }

    // ========== 工具方法 ==========

    private void mapToEntity(Object entity, Map<String, Object> data, String... fields) {
        for (String field : fields) {
            Object value = data.get(field);
            if (value != null) {
                try {
                    var fieldObj = entity.getClass().getDeclaredField(field);
                    fieldObj.setAccessible(true);
                    if (value instanceof String s) {
                        fieldObj.set(entity, s);
                    } else {
                        fieldObj.set(entity, value.toString());
                    }
                } catch (Exception e) {
                    log.warn("Failed to set field {}: {}", field, e.getMessage());
                }
            }
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
