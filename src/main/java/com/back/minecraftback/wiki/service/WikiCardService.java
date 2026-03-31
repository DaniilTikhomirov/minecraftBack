package com.back.minecraftback.wiki.service;

import com.back.minecraftback.service.FileStorageService;
import com.back.minecraftback.wiki.WikiBase64Images;
import com.back.minecraftback.wiki.WikiImageValidator;
import com.back.minecraftback.wiki.dto.GetWikiCardDto;
import com.back.minecraftback.wiki.dto.WikiCardSaveDto;
import com.back.minecraftback.wiki.entity.WikiCardEntity;
import com.back.minecraftback.wiki.repository.WikiCardRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WikiCardService {

    private final WikiCardRepository wikiCardRepository;
    private final FileStorageService fileStorageService;
    private final WikiImageValidator wikiImageValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveAll(List<WikiCardSaveDto> list) {
        for (WikiCardSaveDto dto : list) {
            if (isNew(dto)) {
                saveNew(dto);
            } else {
                if (dto.id() == null) {
                    throw new IllegalArgumentException("id is required for update");
                }
                saveExisting(dto);
            }
        }
    }

    private void saveNew(WikiCardSaveDto dto) {
        validateBasics(dto);
        WikiCardEntity entity = new WikiCardEntity();
        applyTextFields(dto, entity);
        String articleJson = processArticleJson(dto.article());
        entity.setArticle(articleJson);
        applyCover(dto, entity, true, null);
        wikiCardRepository.save(entity);
    }

    private void saveExisting(WikiCardSaveDto dto) {
        validateBasics(dto);
        WikiCardEntity entity = wikiCardRepository.findById(dto.id())
                .orElseThrow(EntityNotFoundException::new);
        Set<String> urlsBefore = collectAllFileUrls(entity);
        applyTextFields(dto, entity);
        String articleJson = processArticleJson(dto.article());
        entity.setArticle(articleJson);
        applyCover(dto, entity, false, entity.getCoverImageUrl());
        wikiCardRepository.save(entity);
        Set<String> urlsAfter = collectAllFileUrls(entity);
        urlsBefore.removeAll(urlsAfter);
        for (String u : urlsBefore) {
            fileStorageService.deleteStoredFileIfExists(u);
        }
    }

    private static void validateBasics(WikiCardSaveDto dto) {
        if (dto.title() == null || dto.title().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
    }

    private static void applyTextFields(WikiCardSaveDto dto, WikiCardEntity entity) {
        entity.setTitle(dto.title().trim());
        entity.setSubtitle(dto.subtitle() != null ? dto.subtitle().trim() : "");
        entity.setSortOrder(dto.sortOrder() != null ? dto.sortOrder() : 0);
    }

    /**
     * Обложка: новый файл из base64; иначе URL из DTO при создании; при обновлении без base64 — сохраняем переданный coverImageUrl или старое значение.
     */
    private void applyCover(WikiCardSaveDto dto, WikiCardEntity entity, boolean isNew, String previousCoverUrl) {
        boolean hasB64 = dto.coverImageBase64() != null && !dto.coverImageBase64().isBlank();
        if (hasB64) {
            byte[] data = WikiBase64Images.decodeImagePayload(dto.coverImageBase64());
            wikiImageValidator.validate(data);
            if (!isNew && previousCoverUrl != null && !previousCoverUrl.isBlank()) {
                entity.setCoverImageUrl(fileStorageService.save(data, previousCoverUrl));
            } else {
                entity.setCoverImageUrl(fileStorageService.save(data));
            }
            return;
        }
        if (isNew) {
            entity.setCoverImageUrl(dto.coverImageUrl() != null ? dto.coverImageUrl().trim() : "");
            return;
        }
        if (dto.coverImageUrl() != null && !dto.coverImageUrl().isBlank()) {
            entity.setCoverImageUrl(dto.coverImageUrl().trim());
        }
    }

    private String processArticleJson(JsonNode articleInput) {
        ObjectNode root = defaultArticleRoot(articleInput);
        ArrayNode blocks = ensureBlocksArray(root);
        for (int i = 0; i < blocks.size(); i++) {
            JsonNode el = blocks.get(i);
            if (!el.isObject()) {
                continue;
            }
            processBlock((ObjectNode) el);
        }
        if (!root.has("version")) {
            root.put("version", 1);
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid article JSON", e);
        }
    }

    private ObjectNode defaultArticleRoot(JsonNode articleInput) {
        if (articleInput == null || !articleInput.isObject()) {
            ObjectNode o = objectMapper.createObjectNode();
            o.put("version", 1);
            o.set("blocks", objectMapper.createArrayNode());
            return o;
        }
        return (ObjectNode) articleInput.deepCopy();
    }

    private ArrayNode ensureBlocksArray(ObjectNode root) {
        JsonNode b = root.get("blocks");
        if (b == null || !b.isArray()) {
            ArrayNode arr = objectMapper.createArrayNode();
            root.set("blocks", arr);
            return arr;
        }
        return (ArrayNode) b;
    }

    private void processBlock(ObjectNode block) {
        String type = block.has("type") ? block.get("type").asText("") : "";
        switch (type) {
            case "textImage" -> processImageField(block, "imageBase64", "imageUrl", true);
            case "image" -> processImageField(block, "imageBase64", "imageUrl", true);
            case "text" -> block.remove("imageBase64");
            default -> block.remove("imageBase64");
        }
    }

    private void processImageField(ObjectNode block, String b64Key, String urlKey, boolean allowReplace) {
        if (!block.has(b64Key) || block.get(b64Key).isNull()) {
            block.remove(b64Key);
            return;
        }
        String raw = block.get(b64Key).asText("");
        if (raw.isBlank()) {
            block.remove(b64Key);
            return;
        }
        byte[] data = WikiBase64Images.decodeImagePayload(raw);
        wikiImageValidator.validate(data);
        String existingUrl = block.has(urlKey) && !block.get(urlKey).isNull()
                ? block.get(urlKey).asText("")
                : "";
        String saved;
        if (allowReplace && !existingUrl.isBlank()) {
            saved = fileStorageService.save(data, existingUrl);
        } else {
            saved = fileStorageService.save(data);
        }
        block.put(urlKey, saved);
        block.remove(b64Key);
    }

    private Set<String> collectAllFileUrls(WikiCardEntity entity) {
        Set<String> set = new HashSet<>();
        addUrl(set, entity.getCoverImageUrl());
        collectArticleImageUrls(entity.getArticle(), set);
        return set;
    }

    private void collectArticleImageUrls(String articleJson, Set<String> out) {
        if (articleJson == null || articleJson.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(articleJson);
            JsonNode blocks = root.get("blocks");
            if (blocks == null || !blocks.isArray()) {
                return;
            }
            for (JsonNode b : blocks) {
                if (!b.isObject()) {
                    continue;
                }
                String type = b.has("type") ? b.get("type").asText("") : "";
                if ("textImage".equals(type) || "image".equals(type)) {
                    if (b.has("imageUrl") && !b.get("imageUrl").isNull()) {
                        addUrl(out, b.get("imageUrl").asText());
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore parse errors for cleanup
        }
    }

    private static void addUrl(Set<String> set, String url) {
        if (url != null && !url.isBlank()) {
            set.add(url.trim());
        }
    }

    private static JsonNode stripBase64FromArticle(JsonNode root) {
        if (root == null || !root.isObject()) {
            return root;
        }
        ObjectNode copy = (ObjectNode) root.deepCopy();
        JsonNode blocks = copy.get("blocks");
        if (blocks != null && blocks.isArray()) {
            for (JsonNode b : blocks) {
                if (b.isObject()) {
                    ((ObjectNode) b).remove("imageBase64");
                }
            }
        }
        return copy;
    }

    public List<GetWikiCardDto> getAllActive() {
        return wikiCardRepository.findAllByActiveIsTrueOrderBySortOrderAscIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    public List<GetWikiCardDto> getAllInactive() {
        return wikiCardRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                .filter(e -> !Boolean.TRUE.equals(e.getActive()))
                .map(this::toDto)
                .toList();
    }

    public List<GetWikiCardDto> getAllFromDb() {
        return wikiCardRepository.findAll(Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"))).stream()
                .map(this::toDto)
                .toList();
    }

    private GetWikiCardDto toDto(WikiCardEntity e) {
        JsonNode articleNode;
        try {
            articleNode = stripBase64FromArticle(objectMapper.readTree(e.getArticle()));
        } catch (Exception ex) {
            articleNode = objectMapper.createObjectNode().put("version", 1)
                    .set("blocks", objectMapper.createArrayNode());
        }
        return new GetWikiCardDto(
                e.getId(),
                e.getTitle(),
                e.getSubtitle(),
                e.getSortOrder(),
                e.getCoverImageUrl() != null ? e.getCoverImageUrl() : "",
                Boolean.TRUE.equals(e.getActive()),
                articleNode
        );
    }

    @Transactional
    public void swapActive(long id) {
        WikiCardEntity entity = wikiCardRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        entity.setActive(!Boolean.TRUE.equals(entity.getActive()));
        wikiCardRepository.saveAndFlush(entity);
    }

    @Transactional
    public void deleteById(long id) {
        WikiCardEntity entity = wikiCardRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        Set<String> urls = collectAllFileUrls(entity);
        wikiCardRepository.delete(entity);
        for (String u : urls) {
            fileStorageService.deleteStoredFileIfExists(u);
        }
    }

    @Transactional
    public void deleteAll() {
        List<WikiCardEntity> all = wikiCardRepository.findAll();
        for (WikiCardEntity e : all) {
            for (String u : collectAllFileUrls(e)) {
                fileStorageService.deleteStoredFileIfExists(u);
            }
        }
        wikiCardRepository.deleteAll();
    }

    private static boolean isNew(WikiCardSaveDto dto) {
        return dto.id() == null || dto.id() == 0;
    }
}
