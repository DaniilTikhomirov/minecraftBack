package com.back.minecraftback.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Санитизация HTML для поля {@code detailedDescription} (витрина / админка).
 * Plain text без тегов не меняется.
 */
public final class DetailedDescriptionHtml {

    private static final Pattern LOOKS_LIKE_HTML = Pattern.compile("<[/!]?[a-zA-Z]");
    private static final Pattern DANGEROUS_STYLE_VALUE = Pattern.compile(
            "(?i)(javascript:|expression\\s*\\(|@import|behavior\\s*:|binding\\s*:|url\\s*\\(\\s*['\"]?(?!https?:))"
    );

    private static final Set<String> ALLOWED_STYLE_PROPS = Set.of(
            "color",
            "background-color",
            "text-align",
            "font-weight",
            "font-size"
    );

    private static final Safelist SAFELIST = Safelist.none()
            .addTags(
                    "p", "br", "div", "span", "strong", "b", "em", "i", "u",
                    "h1", "h2", "h3", "h4", "ul", "ol", "li", "a"
            )
            .addAttributes("a", "href", "target", "rel")
            .addAttributes(":all", "style")
            .addProtocols("a", "href", "http", "https");

    private DetailedDescriptionHtml() {
    }

    /**
     * @return исходная строка, plain text как есть, или очищенный HTML
     */
    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!looksLikeHtml(value)) {
            return value;
        }

        String cleaned = Jsoup.clean(
                value,
                "",
                SAFELIST,
                new Document.OutputSettings().prettyPrint(false)
        );
        if (cleaned == null || cleaned.isBlank()) {
            return "";
        }

        Document doc = Jsoup.parseBodyFragment(cleaned);
        Element body = doc.body();
        for (Element el : body.getAllElements()) {
            if (el.hasAttr("style")) {
                String filtered = filterStyle(el.attr("style"));
                if (filtered.isEmpty()) {
                    el.removeAttr("style");
                } else {
                    el.attr("style", filtered);
                }
            }
            if ("a".equals(el.tagName())) {
                el.attr("rel", "nofollow noopener noreferrer");
            }
        }
        return body.html();
    }

    private static boolean looksLikeHtml(String value) {
        return value.indexOf('<') >= 0 && LOOKS_LIKE_HTML.matcher(value).find();
    }

    private static String filterStyle(String style) {
        if (style == null || style.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String part : style.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            int colon = part.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String prop = part.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String val = part.substring(colon + 1).trim();
            if (!isAllowedStyleProperty(prop) || isDangerousStyleValue(val)) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append("; ");
            }
            out.append(prop).append(": ").append(val);
        }
        return out.toString();
    }

    private static boolean isAllowedStyleProperty(String prop) {
        if (ALLOWED_STYLE_PROPS.contains(prop)) {
            return true;
        }
        return prop.startsWith("border")
                || prop.startsWith("padding")
                || prop.startsWith("margin");
    }

    private static boolean isDangerousStyleValue(String value) {
        return DANGEROUS_STYLE_VALUE.matcher(value).find();
    }
}
