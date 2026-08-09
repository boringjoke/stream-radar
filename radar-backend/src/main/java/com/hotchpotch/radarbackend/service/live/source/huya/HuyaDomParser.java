package com.hotchpotch.radarbackend.service.live.source.huya;

import java.util.Locale;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * 虎牙直播间 DOM 备用数据源解析器。
 */
@Component
public class HuyaDomParser {

    /**
     * 解析虎牙直播间页面中的稳定 DOM 资料。
     *
     * @param html 页面 HTML
     * @param requestedRoomId 请求房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String html, String requestedRoomId) {
        if (isBlank(html)) {
            return LiveSourceResult.unknown("虎牙 DOM 备用页面响应为空");
        }
        if (isBlank(requestedRoomId)) {
            return LiveSourceResult.unknown("虎牙 DOM 备用房间标识为空");
        }

        Document document = Jsoup.parse(html);
        String pageTitle = firstAttribute(document, "meta[property=og:title]", "content");
        if (isBlank(pageTitle)) {
            pageTitle = firstText(document, "title");
        }
        if (isMissingAnchorPage(document, pageTitle)) {
            return LiveSourceResult.unknown("虎牙 DOM 备用页面提示找不到主播");
        }
        String anchorName = firstAttribute(
                document,
                "[data-anchor-name], [data-nickname], [data-nick], meta[name=author]",
                "content");
        if (isBlank(anchorName)) {
            anchorName = firstText(document,
                    "[data-anchor-name], [data-nickname], [data-nick], "
                            + ".host-name, .host-nick, .host-nickname, "
                            + "[class*=host-name], [class*=host-nick], [class*=nickname]");
        }
        if (isBlank(anchorName)) {
            anchorName = firstDataValue(document,
                    "[data-anchor-name], [data-nickname], [data-nick]",
                    "data-anchor-name", "data-nickname", "data-nick");
        }

        String liveTitle = firstAttribute(document, "meta[name=description]", "content");
        if (isBlank(liveTitle)) {
            liveTitle = firstText(document,
                    "[data-room-name], .room-name, .live-room-name, [class*=room-name]");
        }
        if (isBlank(liveTitle) && !isGenericPageTitle(pageTitle)) {
            liveTitle = pageTitle;
        }

        String coverUrl = firstAttribute(document,
                "meta[property=og:image], meta[name=image]", "content");
        if (isBlank(coverUrl)) {
            coverUrl = firstImageUrl(document,
                    ".room-cover img, .host-pic img, [class*=cover] img, [class*=screenshot] img");
        }

        String avatarUrl = firstImageUrl(document,
                ".host-avatar img, [class*=avatar] img, [class*=host] img");
        String platformUid = firstAttribute(document,
                "[data-anchor-uid], [data-user-id], [data-uid]", "data-anchor-uid");
        if (isBlank(platformUid)) {
            platformUid = firstDataAttribute(document,
                    "[data-anchor-uid], [data-user-id], [data-uid]");
        }

        if (isGenericPageTitle(pageTitle)
                && isBlank(anchorName)
                && isBlank(platformUid)) {
            return LiveSourceResult.unknown("虎牙 DOM 备用页面缺少主播身份字段");
        }
        if (isGenericPageTitle(pageTitle)
                && isBlank(anchorName)
                && isBlank(liveTitle)
                && isBlank(coverUrl)) {
            return LiveSourceResult.unknown("虎牙 DOM 备用页面缺少主播资料");
        }
        if (isBlank(anchorName) && isBlank(liveTitle) && isBlank(coverUrl)) {
            return LiveSourceResult.unknown("虎牙 DOM 备用页面缺少主播资料");
        }

        return LiveSourceResult.available(new LiveSnapshot(
                LivePlatform.HUYA,
                requestedRoomId,
                platformUid,
                anchorName,
                avatarUrl,
                stripQuery(coverUrl),
                liveTitle,
                null,
                detectStatus(document)));
    }

    /**
     * 从 DOM 中读取显式直播状态；无法确认时保留 UNKNOWN。
     *
     * @param document 页面文档
     * @return 统一直播状态
     */
    private LiveStatus detectStatus(Document document) {
        Elements statusElements = document.select("[data-live-status], [data-live]");
        for (Element element : statusElements) {
            LiveStatus status = mapStatus(firstNonBlank(
                    element.attr("data-live-status"),
                    firstNonBlank(element.attr("data-live"), element.text())));
            if (status != null) {
                return status;
            }
        }

        String bodyText = document.body() == null ? "" : document.body().text();
        if (bodyText.contains("未开播") || bodyText.contains("直播已结束")) {
            return LiveStatus.OFFLINE;
        }
        if (!document.select("video[src], video source[src]").isEmpty()) {
            return LiveStatus.LIVE;
        }
        return LiveStatus.UNKNOWN;
    }

    /**
     * 将 DOM 状态文本映射为统一状态。
     *
     * @param value 状态文本
     * @return 统一状态，无法识别时返回 null
     */
    private LiveStatus mapStatus(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("1")
                || normalized.equals("true")
                || normalized.contains("live")
                || normalized.contains("直播中")) {
            return LiveStatus.LIVE;
        }
        if (normalized.equals("0")
                || normalized.equals("false")
                || normalized.contains("offline")
                || normalized.contains("未开播")
                || normalized.contains("直播已结束")) {
            return LiveStatus.OFFLINE;
        }
        return null;
    }

    /**
     * 读取第一个匹配元素的属性。
     *
     * @param document 页面文档
     * @param selector CSS 选择器
     * @param attribute 属性名
     * @return 属性值
     */
    private String firstAttribute(Document document, String selector, String attribute) {
        Element element = document.select(selector).first();
        if (element == null) {
            return null;
        }
        String value = element.attr(attribute);
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 读取多个选择器中第一个非空文本。
     *
     * @param document 页面文档
     * @param selector CSS 选择器
     * @return 文本值
     */
    private String firstText(Document document, String selector) {
        Element element = document.select(selector).first();
        if (element == null) {
            return null;
        }
        String value = element.text();
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 读取第一个图片地址。
     *
     * @param document 页面文档
     * @param selector CSS 选择器
     * @return 图片地址
     */
    private String firstImageUrl(Document document, String selector) {
        Element element = document.select(selector).first();
        if (element == null) {
            return null;
        }
        String value = firstNonBlank(
                element.absUrl("src"),
                firstNonBlank(element.attr("data-src"), element.attr("src")));
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 读取第一个匹配元素的数据属性。
     *
     * @param document 页面文档
     * @param selector CSS 选择器
     * @return 数据属性值
     */
    private String firstDataAttribute(Document document, String selector) {
        Element element = document.select(selector).first();
        if (element == null) {
            return null;
        }
        return firstNonBlank(
                element.attr("data-anchor-uid"),
                firstNonBlank(element.attr("data-user-id"), element.attr("data-uid")));
    }

    /**
     * 读取第一个匹配元素的数据属性值。
     *
     * @param document 页面文档
     * @param selector CSS 选择器
     * @param attributes 候选数据属性
     * @return 数据属性值
     */
    private String firstDataValue(Document document, String selector, String... attributes) {
        Element element = document.select(selector).first();
        if (element == null) {
            return null;
        }
        for (String attribute : attributes) {
            String value = element.attr(attribute);
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 判断页面标题是否为通用错误标题。
     *
     * @param title 页面标题
     * @return 是否为通用标题
     */
    private boolean isGenericPageTitle(String title) {
        if (isBlank(title)) {
            return true;
        }
        String normalized = title.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("虎牙直播")
                || normalized.equals("官方直播间--虎牙直播")
                || normalized.startsWith("虎牙直播-")
                || normalized.contains("404")
                || normalized.contains("error")
                || normalized.contains("无法加载")
                || normalized.contains("不存在")
                || normalized.contains("找不到主播");
    }

    /**
     * 判断页面是否明确提示主播不存在。
     *
     * @param document 页面文档
     * @param pageTitle 页面标题
     * @return 是否为找不到主播页面
     */
    private boolean isMissingAnchorPage(Document document, String pageTitle) {
        if (containsMissingAnchorText(pageTitle)) {
            return true;
        }
        String bodyText = document.body() == null ? "" : document.body().text();
        return containsMissingAnchorText(bodyText);
    }

    /**
     * 判断文本是否包含虎牙房间不存在页面的提示文案。
     *
     * @param value 待检查文本
     * @return 是否为找不到主播提示
     */
    private boolean containsMissingAnchorText(String value) {
        return !isBlank(value)
                && (value.contains("找不到主播") || value.contains("找不到这个主播"));
    }

    /**
     * 选择第一个非空值。
     *
     * @param first 优先值
     * @param fallback 备用值
     * @return 选择结果
     */
    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    /**
     * 移除封面地址后的查询参数。
     *
     * @param value 原始地址
     * @return 清理后的地址
     */
    private String stripQuery(String value) {
        if (isBlank(value)) {
            return value;
        }
        int queryIndex = value.indexOf('?');
        return queryIndex < 0 ? value : value.substring(0, queryIndex);
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
