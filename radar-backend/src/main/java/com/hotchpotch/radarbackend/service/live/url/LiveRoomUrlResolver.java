package com.hotchpotch.radarbackend.service.live.url;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.config.RadarUrlProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 直播间 URL 安全解析服务。
 *
 * <p>该服务只负责 URL 身份解析和短链接安全跳转，不调用平台直播数据接口。</p>
 */
@Service
public class LiveRoomUrlResolver {

    /**
     * 平台正式域名白名单。
     */
    private static final Map<String, LivePlatform> PLATFORM_HOSTS = Map.of(
            "live.bilibili.com", LivePlatform.BILIBILI,
            "m.live.bilibili.com", LivePlatform.BILIBILI,
            "www.douyu.com", LivePlatform.DOUYU,
            "douyu.com", LivePlatform.DOUYU,
            "m.douyu.com", LivePlatform.DOUYU,
            "www.huya.com", LivePlatform.HUYA,
            "huya.com", LivePlatform.HUYA,
            "m.huya.com", LivePlatform.HUYA,
            "live.douyin.com", LivePlatform.DOUYIN);

    /**
     * 平台官方短链接域名白名单。
     */
    private static final Set<String> SHORT_LINK_HOSTS = Set.of(
            "b23.tv",
            "bili2233.cn",
            "v.douyin.com",
            "iesdouyin.com",
            "www.iesdouyin.com");

    /**
     * 允许作为数字房间标识的字符格式。
     */
    private static final Pattern NUMERIC_ROOM_ID = Pattern.compile("[0-9]{1,128}");

    /**
     * 允许作为虎牙等平台房间标识的字符格式。
     */
    private static final Pattern TOKEN_ROOM_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,127}");

    /**
     * URL 解析配置。
     */
    private final RadarUrlProperties properties;

    /**
     * 短链接 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 创建直播间 URL 解析服务。
     *
     * @param properties URL 解析配置
     * @param webClient 短链接 HTTP 客户端
     */
    public LiveRoomUrlResolver(RadarUrlProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    /**
     * 安全解析并规范化直播间 URL。
     *
     * @param rawUrl 用户输入的直播间 URL
     * @return 解析后的直播间身份
     */
    public ResolvedLiveRoom resolve(String rawUrl) {
        String normalizedUrl = normalizeInput(rawUrl);
        URI initialUri = parseUri(normalizedUrl);
        validateUriBasics(initialUri, true);

        URI targetUri = isShortLink(initialUri)
                ? resolveShortLink(initialUri)
                : initialUri;
        return parsePlatformRoom(targetUri);
    }

    /**
     * 规范化并校验用户输入文本。
     *
     * @param rawUrl 原始 URL
     * @return 去除首尾空白后的 URL
     */
    private String normalizeInput(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "请输入直播间链接");
        }
        String normalizedUrl = rawUrl.trim();
        if (normalizedUrl.length() > properties.getMaxLength()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "直播间链接过长");
        }
        return normalizedUrl;
    }

    /**
     * 将文本解析为绝对 URI。
     *
     * @param rawUrl 原始 URL
     * @return URI 对象
     */
    private URI parseUri(String rawUrl) {
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException exception) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "直播间链接格式不正确", exception);
        }
    }

    /**
     * 校验协议、域名、端口和危险 URL 组成部分。
     *
     * @param uri 待校验 URI
     * @param allowShortLink 是否允许短链接域名
     */
    private void validateUriBasics(URI uri, boolean allowShortLink) {
        String scheme = uri.getScheme();
        String host = normalizedHost(uri);
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "仅支持 HTTP 或 HTTPS 直播间链接");
        }
        if (uri.getUserInfo() != null || uri.getPort() != -1) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "直播间链接格式不安全");
        }
        if (!PLATFORM_HOSTS.containsKey(host) && !(allowShortLink && SHORT_LINK_HOSTS.contains(host))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "暂不支持该直播平台或链接域名");
        }
        validateRawPath(uri.getRawPath());
    }

    /**
     * 获取小写域名，并拒绝无法识别的 URI 主机部分。
     *
     * @param uri 待处理 URI
     * @return 小写域名
     */
    private String normalizedHost(URI uri) {
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "直播间链接缺少合法域名");
        }
        return host.toLowerCase(Locale.ROOT);
    }

    /**
     * 校验原始路径，避免编码后的斜杠、反斜杠和控制字符绕过路径规则。
     *
     * @param rawPath 原始路径
     */
    private void validateRawPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "直播间房间标识缺失");
        }
        String lowerPath = rawPath.toLowerCase(Locale.ROOT);
        if (lowerPath.contains("%2f") || lowerPath.contains("%5c") || lowerPath.contains("%00")) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "直播间链接路径不安全");
        }
        for (int index = 0; index < rawPath.length(); index++) {
            if (Character.isISOControl(rawPath.charAt(index))) {
                throw new BusinessException(ErrorCode.PARAMETER_ERROR, "直播间链接路径不安全");
            }
        }
    }

    /**
     * 判断 URI 是否为需要服务端跟随解析的官方短链接。
     *
     * @param uri 待判断 URI
     * @return 是否为短链接
     */
    private boolean isShortLink(URI uri) {
        return SHORT_LINK_HOSTS.contains(normalizedHost(uri));
    }

    /**
     * 手动解析短链接重定向，避免让 HTTP 客户端访问任意跳转目标。
     *
     * @param initialUri 初始短链接
     * @return 最终平台直播间 URI
     */
    private URI resolveShortLink(URI initialUri) {
        URI currentUri = initialUri;
        Set<String> visitedUris = new HashSet<>();
        int maxRedirects = Math.max(0, properties.getMaxRedirects());

        for (int redirectCount = 0; redirectCount <= maxRedirects; redirectCount++) {
            String currentText = currentUri.toString();
            if (!visitedUris.add(currentText)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接重定向存在循环");
            }

            String currentHost = normalizedHost(currentUri);
            if (PLATFORM_HOSTS.containsKey(currentHost)) {
                return currentUri;
            }

            validateUriBasics(currentUri, true);
            validatePublicNetworkTarget(currentHost);
            RedirectResponse response = requestShortLink(currentUri);
            if (!response.isRedirect()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接未跳转到有效直播间");
            }
            if (response.location() == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接缺少安全跳转地址");
            }

            URI nextUri = currentUri.resolve(response.location());
            validateUriBasics(nextUri, true);
            currentUri = nextUri;
        }

        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接重定向次数超过安全限制");
    }

    /**
     * 请求短链接响应头，并释放响应体。
     *
     * @param uri 短链接 URI
     * @return 重定向响应摘要
     */
    private RedirectResponse requestShortLink(URI uri) {
        try {
            RedirectResponse response = webClient.get()
                    .uri(uri)
                    .exchangeToMono(clientResponse -> {
                        HttpStatusCode statusCode = clientResponse.statusCode();
                        URI location = clientResponse.headers().asHttpHeaders().getLocation();
                        return clientResponse.releaseBody()
                                .thenReturn(new RedirectResponse(statusCode.value(), location));
                    })
                    .block(Duration.ofMillis(Math.max(1000, properties.getResponseTimeoutMs() + 1000L)));
            if (response == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接暂时无法解析，请稍后重试");
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接暂时无法解析，请稍后重试", exception);
        }
    }

    /**
     * 校验短链接域名的 DNS 解析结果不是内网或保留地址。
     *
     * @param host 已通过域名白名单的主机名
     */
    private void validatePublicNetworkTarget(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接域名暂时无法解析");
            }
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    throw new BusinessException(ErrorCode.PARAMETER_ERROR, "短链接目标地址不安全");
                }
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短链接域名暂时无法解析", exception);
        }
    }

    /**
     * 判断 IP 地址是否属于内网、回环、链路本地或保留网段。
     *
     * @param address 待判断地址
     * @return 是否应当阻止访问
     */
    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            long ip = ((long) bytes[0] & 0xff) << 24
                    | ((long) bytes[1] & 0xff) << 16
                    | ((long) bytes[2] & 0xff) << 8
                    | ((long) bytes[3] & 0xff);
            return isInIpv4Range(ip, 0x00000000L, 0x00ffffffL)
                    || isInIpv4Range(ip, 0x64400000L, 0x647fffffL)
                    || isInIpv4Range(ip, 0xc0000000L, 0xc00000ffL)
                    || isInIpv4Range(ip, 0xc0000200L, 0xc00002ffL)
                    || isInIpv4Range(ip, 0xc6120000L, 0xc613ffffL)
                    || isInIpv4Range(ip, 0xc6336400L, 0xc63364ffL)
                    || isInIpv4Range(ip, 0xcb007100L, 0xcb0071ffL)
                    || isInIpv4Range(ip, 0xf0000000L, 0xffffffffL);
        }

        return (bytes[0] & 0xfe) == 0xfc
                || (bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xc0) == 0x80
                || isIpv6DocumentationAddress(bytes);
    }

    /**
     * 判断 IPv4 地址是否落在指定闭区间内。
     *
     * @param ip 无符号 IPv4 地址
     * @param start 起始地址
     * @param end 结束地址
     * @return 是否在区间内
     */
    private boolean isInIpv4Range(long ip, long start, long end) {
        return ip >= start && ip <= end;
    }

    /**
     * 判断 IPv6 文档保留地址。
     *
     * @param bytes IPv6 地址字节
     * @return 是否为文档保留地址
     */
    private boolean isIpv6DocumentationAddress(byte[] bytes) {
        return (bytes[0] & 0xff) == 0x20
                && (bytes[1] & 0xff) == 0x01
                && (bytes[2] & 0xff) == 0x0d
                && (bytes[3] & 0xff) == 0xb8;
    }

    /**
     * 从正式平台 URI 中提取并校验房间标识。
     *
     * @param uri 平台直播间 URI
     * @return 解析后的直播间身份
     */
    private ResolvedLiveRoom parsePlatformRoom(URI uri) {
        String host = normalizedHost(uri);
        LivePlatform platform = PLATFORM_HOSTS.get(host);
        if (platform == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未能识别直播平台");
        }

        List<String> pathSegments = pathSegments(uri.getPath());
        String roomId = extractRoomId(platform, pathSegments);
        return new ResolvedLiveRoom(
                platform,
                roomId,
                platform.getCanonicalUrlPrefix() + "/" + roomId);
    }

    /**
     * 将路径拆分为非空段，并拒绝中间空路径段。
     *
     * @param path 已解码路径
     * @return 路径段列表
     */
    private List<String> pathSegments(String path) {
        if (path == null || path.isBlank() || path.contains("\\") || path.contains("//")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "直播间房间标识缺失或路径不合法");
        }
        String normalizedPath = path;
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (normalizedPath.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "直播间房间标识缺失");
        }
        return Arrays.asList(normalizedPath.split("/", -1));
    }

    /**
     * 按平台路径规则提取房间标识。
     *
     * @param platform 直播平台
     * @param pathSegments 路径段列表
     * @return 房间标识
     */
    private String extractRoomId(LivePlatform platform, List<String> pathSegments) {
        String roomId;
        switch (platform) {
            case BILIBILI -> roomId = extractWithOptionalPrefix(pathSegments, Set.of("h5", "mobile"), true);
            case DOUYU -> roomId = extractWithOptionalPrefix(pathSegments, Set.of("room"), true);
            case HUYA -> roomId = extractWithOptionalPrefix(pathSegments, Set.of("room"), false);
            case DOUYIN -> roomId = extractWithOptionalPrefix(pathSegments, Set.of(), true);
            default -> throw new BusinessException(ErrorCode.BUSINESS_ERROR, "暂不支持该直播平台");
        }
        return roomId;
    }

    /**
     * 提取单段房间标识，并支持少量已知移动端路径前缀。
     *
     * @param pathSegments 路径段列表
     * @param optionalPrefixes 允许的路径前缀
     * @param numericOnly 是否仅允许数字房间标识
     * @return 房间标识
     */
    private String extractWithOptionalPrefix(
            List<String> pathSegments,
            Set<String> optionalPrefixes,
            boolean numericOnly) {
        String roomId;
        if (pathSegments.size() == 1) {
            roomId = pathSegments.get(0);
        } else if (pathSegments.size() == 2
                && optionalPrefixes.contains(pathSegments.get(0).toLowerCase(Locale.ROOT))) {
            roomId = pathSegments.get(1);
        } else {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "直播间路径无法识别");
        }

        boolean valid = numericOnly
                ? NUMERIC_ROOM_ID.matcher(roomId).matches()
                : TOKEN_ROOM_ID.matcher(roomId).matches();
        if (!valid) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "直播间房间标识不合法");
        }
        return roomId;
    }

    /**
     * 短链接响应摘要。
     *
     * @param statusCode HTTP 状态码
     * @param location 重定向地址
     */
    private record RedirectResponse(int statusCode, URI location) {

        /**
         * 判断响应是否为重定向。
         *
         * @return 是否为 3xx 响应
         */
        private boolean isRedirect() {
            return statusCode >= 300 && statusCode < 400;
        }
    }
}
