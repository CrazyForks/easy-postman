package com.laker.postman.service.render;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.model.*;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 统一的 HTTP 请求/响应 HTML 渲染工具类
 */
@UtilityClass
public class HttpHtmlRenderer {

    // 内容大小限制 - 防止内存溢出
    private static final int MAX_DISPLAY_SIZE = 2 * 1024;

    // HTML 模板
    private static final String HTML_TEMPLATE = "<html><body style='font-family:monospace;font-size:%s;'>%s</body></html>";

    // 颜色常量
    private static final String COLOR_PRIMARY = "#1976d2";
    private static final String COLOR_SUCCESS  = "#388e3c";
    private static final String COLOR_ERROR    = "#d32f2f";
    private static final String COLOR_WARNING  = "#ffa000";
    private static final String COLOR_GRAY     = "#888";

    // ==================== 字号计算 ====================

    /**
     * HTML 字号。
     * JTextPane 的 HTMLEditorKit 基于 72dpi，而 Swing 字号基于屏幕 DPI（macOS 通常 96~144dpi），
     * 直接用 Swing 字号渲染 HTML 会偏大，乘以 0.75 换算为视觉等效的 HTML px。
     */
    private static int htmlFontSize() {
        return Math.max(8, (int) Math.round(SettingManager.getUiFontSize() * 0.75));
    }

    /** 正文字号字符串，如 "10px" */
    private static String fs() {
        return htmlFontSize() + "px";
    }

    /** 辅助文字字号（比正文小 1px，最小 8px） */
    private static String fsSmall() {
        return Math.max(8, htmlFontSize() - 1) + "px";
    }

    // ==================== 主题工具 ====================

    private static boolean isDarkTheme() { return FlatLaf.isLafDark(); }

    private static String bgColor()     { return isDarkTheme() ? "rgb(60,63,65)"   : "rgb(242,242,242)"; }
    private static String textColor()   { return isDarkTheme() ? "#e0e0e0"          : "#222"; }
    private static String borderColor() { return isDarkTheme() ? "#4a4a4a"          : "#e0e0e0"; }

    private static String statusColor(int code) {
        if (code >= 500) return COLOR_ERROR;
        if (code >= 400) return COLOR_WARNING;
        return "#43a047";
    }

    // ==================== HTML 片段工具 ====================

    private static String htmlDoc(String bodyContent) {
        return String.format(HTML_TEMPLATE, fs(), bodyContent);
    }

    /** key: value 行 */
    private static String kvRow(String keyColor, String key, String valueColor, String value) {
        return "<div style='margin-bottom:4px;padding:3px 8px;background:" + bgColor()
                + ";border-radius:3px;word-break:break-all;overflow-wrap:break-word;'>"
                + "<strong style='color:" + keyColor + ";'>" + key + ":</strong> "
                + "<span style='color:" + valueColor + ";'>" + value + "</span>"
                + "</div>";
    }

    /** 节标题 */
    private static String sectionTitle(String color, String title) {
        return "<div style='margin:8px 0 4px 0;font-weight:bold;color:" + color + ";'>" + title + "</div>";
    }

    /** 无数据提示 */
    private static String noData(String message) {
        return "<div style='color:" + COLOR_GRAY + ";padding:12px;'>" + message + "</div>";
    }

    /** 警告框 */
    private static String alertBox(String color, String title, String message) {
        return "<div style='border-left:3px solid " + color + ";padding:8px 12px;margin-bottom:10px;"
                + "background:" + bgColor() + ";font-size:" + fs() + ";'>"
                + "<div style='color:" + color + ";font-weight:bold;margin-bottom:4px;'>" + escapeHtml(title) + "</div>"
                + "<div style='color:" + textColor() + ";white-space:pre-wrap;word-break:break-all;'>"
                + escapeHtml(message) + "</div>"
                + "</div>";
    }

    // ==================== 公开 API ====================

    public static String renderTimingInfo(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) return noData("No Timing Info");
        return buildTimingHtml(response);
    }

    public static String renderEventInfo(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) return noData("No Event Info");
        return buildEventInfoHtml(response.httpEventInfo);
    }

    /** 渲染请求信息 */
    public static String renderRequest(PreparedRequest req) {
        if (req == null) return htmlDoc(noData("无请求信息"));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:").append(fs()).append(";word-break:break-all;'>");

        sb.append(kvRow(COLOR_PRIMARY, "URL",    textColor(), escapeHtml(safeStr(req.url))));
        sb.append(kvRow(COLOR_PRIMARY, "Method", textColor(), escapeHtml(safeStr(req.method))));

        if (req.okHttpHeaders != null && req.okHttpHeaders.size() > 0) {
            sb.append(sectionTitle(COLOR_PRIMARY, "Headers"));
            for (int i = 0; i < req.okHttpHeaders.size(); i++) {
                sb.append(kvRow(COLOR_PRIMARY,
                        escapeHtml(req.okHttpHeaders.name(i)),
                        textColor(),
                        escapeHtml(req.okHttpHeaders.value(i))));
            }
        }

        if (req.formDataList != null && !req.formDataList.isEmpty()) {
            boolean hasText = req.formDataList.stream().anyMatch(d -> d.isEnabled() && d.isText());
            boolean hasFile = req.formDataList.stream().anyMatch(d -> d.isEnabled() && d.isFile());
            if (hasText) {
                sb.append(sectionTitle(COLOR_PRIMARY, "Form Data"));
                req.formDataList.stream().filter(d -> d.isEnabled() && d.isText()).forEach(d ->
                        sb.append(kvRow(COLOR_PRIMARY, escapeHtml(d.getKey()), textColor(), escapeHtml(d.getValue()))));
            }
            if (hasFile) {
                sb.append(sectionTitle(COLOR_PRIMARY, "Form Files"));
                req.formDataList.stream().filter(d -> d.isEnabled() && d.isFile()).forEach(d ->
                        sb.append(kvRow(COLOR_PRIMARY, escapeHtml(d.getKey()), textColor(), escapeHtml(d.getValue()))));
            }
        }

        if (req.urlencodedList != null && !req.urlencodedList.isEmpty()) {
            sb.append(sectionTitle(COLOR_PRIMARY, "x-www-form-urlencoded"));
            req.urlencodedList.stream().filter(HttpFormUrlencoded::isEnabled).forEach(e ->
                    sb.append(kvRow(COLOR_PRIMARY, escapeHtml(e.getKey()), textColor(), escapeHtml(e.getValue()))));
        }

        if (isNotEmpty(req.okHttpRequestBody)) {
            sb.append(sectionTitle(COLOR_PRIMARY, "Body"));
            sb.append("<pre style='background:").append(bgColor())
                    .append(";padding:8px;border-radius:4px;font-size:").append(fs())
                    .append(";color:").append(textColor())
                    .append(";white-space:pre-wrap;word-break:break-all;margin:0;box-sizing:border-box;'>")
                    .append(escapeHtml(truncate(req.okHttpRequestBody))).append("</pre>");
        }

        sb.append("</div>");
        return htmlDoc(sb.toString());
    }

    /** 渲染响应信息 */
    public static String renderResponse(HttpResponse resp) {
        if (resp == null) return htmlDoc(noData("无响应信息"));

        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:").append(fs()).append(";word-break:break-all;'>");

        sb.append(kvRow(COLOR_SUCCESS, "Status",   statusColor(resp.code), "<b>" + escapeHtml(String.valueOf(resp.code)) + "</b>"));
        sb.append(kvRow(COLOR_PRIMARY, "Protocol", textColor(), escapeHtml(safeStr(resp.protocol))));
        sb.append(kvRow(COLOR_PRIMARY, "Thread",   textColor(), escapeHtml(safeStr(resp.threadName))));
        if (resp.httpEventInfo != null) {
            sb.append(kvRow(COLOR_PRIMARY, "Connection", textColor(),
                    escapeHtml(safeStr(resp.httpEventInfo.getLocalAddress()))
                            + " → " + escapeHtml(safeStr(resp.httpEventInfo.getRemoteAddress()))));
        }

        if (resp.headers != null && !resp.headers.isEmpty()) {
            sb.append(sectionTitle(COLOR_SUCCESS, "Headers"));
            resp.headers.forEach((key, values) ->
                    sb.append(kvRow(COLOR_PRIMARY, escapeHtml(key), textColor(),
                            escapeHtml(values != null ? String.join(", ", values) : ""))));
        }

        sb.append(sectionTitle(COLOR_SUCCESS, "Body"));
        sb.append("<pre style='background:").append(bgColor())
                .append(";padding:8px;border-radius:4px;font-size:").append(fs())
                .append(";color:").append(textColor())
                .append(";white-space:pre-wrap;word-break:break-all;margin:0;box-sizing:border-box;'>")
                .append(escapeHtml(truncate(resp.body))).append("</pre>");

        sb.append("</div>");
        return htmlDoc(sb.toString());
    }

    public static String renderResponseWithError(ResultNodeInfo info) {
        if (info == null) return renderResponse(null);
        return buildResponseWithError(info.errorMsg,
                info.resp != null ? info.resp.httpEventInfo : null, info.resp);
    }

    public static String renderResponseWithError(RequestResult request) {
        if (request == null) return renderResponse(null);
        return buildResponseWithError(request.getErrorMessage(),
                request.getResponse() != null ? request.getResponse().httpEventInfo : null,
                request.getResponse());
    }

    /** 渲染测试结果 */
    public static String renderTestResults(List<TestResult> testResults) {
        if (testResults == null || testResults.isEmpty()) {
            return htmlDoc(noData("No test results"));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='border-collapse:collapse;width:100%;font-size:").append(fs()).append(";'>")
                .append("<tr style='color:").append(textColor()).append(";font-weight:bold;border-bottom:2px solid ").append(borderColor()).append(";'>")
                .append("<th style='padding:6px 12px;text-align:left;'>Name</th>")
                .append("<th style='padding:6px 12px;text-align:center;'>Result</th>")
                .append("<th style='padding:6px 12px;text-align:left;'>Message</th>")
                .append("</tr>");
        for (TestResult r : testResults) {
            if (r != null) sb.append(testResultRow(r));
        }
        sb.append("</table>");
        return htmlDoc(sb.toString());
    }

    // ==================== 私有实现 ====================

    private static String buildResponseWithError(String errorMsg, HttpEventInfo eventInfo, HttpResponse response) {
        StringBuilder sb = new StringBuilder();

        if (isNotEmpty(errorMsg)) {
            sb.append(alertBox(COLOR_ERROR, "⚠ Error", errorMsg));
        }
        if (eventInfo != null && isNotEmpty(eventInfo.getErrorMessage())) {
            sb.append(alertBox(COLOR_WARNING, "⚠ Network Error", eventInfo.getErrorMessage()));
        }

        if (response != null) {
            // 提取 renderResponse 的 body 内容，嵌入当前文档
            String inner = renderResponse(response);
            int start = inner.indexOf('>', inner.indexOf("<body")) + 1;
            int end   = inner.lastIndexOf("</body>");
            if (start > 0 && end > start) sb.append(inner, start, end);
            else sb.append(inner);
        } else {
            sb.append(noData("No Response"));
        }

        return htmlDoc(sb.toString());
    }

    private static String testResultRow(TestResult r) {
        String icon = r.passed
                ? "<span style='color:#4CAF50;'>&#10003;</span>"
                : "<span style='color:#F44336;'>&#10007;</span>";
        String msg = isNotEmpty(r.message)
                ? "<span style='color:#F44336;white-space:pre-wrap;word-break:break-all;'>" + escapeHtml(r.message) + "</span>"
                : "";
        return "<tr style='border-bottom:1px solid " + borderColor() + ";color:" + textColor() + ";'>"
                + "<td style='padding:6px 12px;'>" + escapeHtml(r.name) + "</td>"
                + "<td style='padding:6px 12px;text-align:center;'>" + icon + "</td>"
                + "<td style='padding:6px 12px;'>" + msg + "</td>"
                + "</tr>";
    }

    private static String buildTimingHtml(HttpResponse response) {
        HttpEventInfo info = response.httpEventInfo;
        TimingCalculator calc = new TimingCalculator(info);
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:").append(fs()).append(";'>")
                .append("<b style='color:").append(COLOR_PRIMARY).append(";'>[Timeline]</b>")
                .append("<table style='border-collapse:collapse;margin:6px 0;width:100%;table-layout:fixed;'>");

        timingRow(sb, "Total",                   calc.getTotal(),          COLOR_ERROR,   true,  "Total time (CallStart→CallEnd)");
        timingRow(sb, "Queueing",                calc.getQueueing(),       null,          false, "Waiting in queue (QueueStart→CallStart)");
        timingRow(sb, "Stalled",                 calc.getStalled(),        null,          false, "Blocked, includes DNS (CallStart→ConnectStart)");
        timingRow(sb, "DNS Lookup",              calc.getDns(),            null,          false, "DnsStart→DnsEnd");
        timingRow(sb, "Initial Connection (TCP)",calc.getConnect(),        null,          false, "ConnectStart→ConnectEnd, includes SSL/TLS");
        timingRow(sb, "SSL/TLS",                 calc.getTls(),            null,          false, "SecureConnectStart→SecureConnectEnd");
        timingRow(sb, "Request Sent",            calc.getRequestSent(),    null,          false, "Sending headers + body");
        timingRow(sb, "Waiting (TTFB)",          calc.getServerCost(),     COLOR_SUCCESS, true,  "Server processing (RequestEnd→ResponseHeadersStart)");
        timingRow(sb, "Content Download",        calc.getResponseBody(),   null,          false, "ResponseBodyStart→ResponseBodyEnd");
        timingRowStr(sb, "Connection Reused",    calc.getConnectionReused() ? "Yes" : "No", null, false, "Was the connection reused?");
        timingRowStr(sb, "OkHttp Idle Conns",    String.valueOf(response.idleConnectionCount), null, false, "Idle OkHttp connections (snapshot)");
        timingRowStr(sb, "OkHttp Total Conns",   String.valueOf(response.connectionCount),     null, false, "Total OkHttp connections (snapshot)");

        sb.append("</table></div>");
        return sb.toString();
    }

    private static void timingRow(StringBuilder sb, String name, long val, String color, boolean bold, String desc) {
        timingRowStr(sb, name, val >= 0 ? val + " ms" : "-", color, bold, desc);
    }

    private static void timingRowStr(StringBuilder sb, String name, String val, String color, boolean bold, String desc) {
        String nameStyle = (bold ? "font-weight:bold;" : "") + (color != null ? "color:" + color + ";" : "");
        String valStyle  = color != null ? "color:" + color + ";" + (bold ? "font-weight:bold;" : "") : "";
        sb.append("<tr>")
                .append("<td style='padding:2px 8px 2px 0;").append(nameStyle).append("width:28%;'>")
                .append(bold ? "<b>" + name + "</b>" : name).append("</td>")
                .append("<td style='").append(valStyle).append("width:15%;'>").append(val).append("</td>")
                .append("<td style='color:").append(COLOR_GRAY).append(";font-size:").append(fsSmall()).append(";width:57%;'>").append(desc).append("</td>")
                .append("</tr>");
    }

    private static String buildEventInfoHtml(HttpEventInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-size:").append(fs()).append(";color:").append(textColor()).append(";'>")
                .append("<b style='color:").append(COLOR_PRIMARY).append(";'>[Event Info]</b>")
                .append("<table style='border-collapse:collapse;width:100%;margin:6px 0;table-layout:fixed;'>");

        eventRow(sb, "QueueStart", formatMillis(info.getQueueStart()));
        eventRow(sb, "Local",      escapeHtml(info.getLocalAddress()));
        eventRow(sb, "Remote",     escapeHtml(info.getRemoteAddress()));
        eventRow(sb, "Protocol",   info.getProtocol() != null ? info.getProtocol().toString() : "-");
        eventRow(sb, "TLS",        safeStr(info.getTlsVersion()));
        eventRow(sb, "Thread",     safeStr(info.getThreadName()));
        eventRow(sb, "Error",      info.getErrorMessage() != null ? escapeHtml(info.getErrorMessage()) : "-");

        sb.append("<tr><td colspan='2'><hr style='border:0;border-top:1px dashed ").append(borderColor()).append(";margin:4px 0'/></td></tr>");

        timingEventRow(sb, "QueueStart",          info.getQueueStart(),          COLOR_PRIMARY);
        timingEventRow(sb, "CallStart",            info.getCallStart(),            COLOR_PRIMARY);
        timingEventRow(sb, "DnsStart",             info.getDnsStart(),             COLOR_PRIMARY);
        timingEventRow(sb, "DnsEnd",               info.getDnsEnd(),               COLOR_PRIMARY);
        timingEventRow(sb, "ConnectStart",         info.getConnectStart(),         COLOR_PRIMARY);
        timingEventRow(sb, "SecureConnectStart",   info.getSecureConnectStart(),   COLOR_PRIMARY);
        timingEventRow(sb, "SecureConnectEnd",     info.getSecureConnectEnd(),     COLOR_PRIMARY);
        timingEventRow(sb, "ConnectEnd",           info.getConnectEnd(),           COLOR_PRIMARY);
        timingEventRow(sb, "ConnectionAcquired",   info.getConnectionAcquired(),   COLOR_PRIMARY);
        timingEventRow(sb, "RequestHeadersStart",  info.getRequestHeadersStart(),  null);
        timingEventRow(sb, "RequestHeadersEnd",    info.getRequestHeadersEnd(),    null);
        timingEventRow(sb, "RequestBodyStart",     info.getRequestBodyStart(),     null);
        timingEventRow(sb, "RequestBodyEnd",       info.getRequestBodyEnd(),       null);
        timingEventRow(sb, "ResponseHeadersStart", info.getResponseHeadersStart(), null);
        timingEventRow(sb, "ResponseHeadersEnd",   info.getResponseHeadersEnd(),   null);
        timingEventRow(sb, "ResponseBodyStart",    info.getResponseBodyStart(),    null);
        timingEventRow(sb, "ResponseBodyEnd",      info.getResponseBodyEnd(),      null);
        timingEventRow(sb, "ConnectionReleased",   info.getConnectionReleased(),   null);
        timingEventRow(sb, "CallEnd",              info.getCallEnd(),              COLOR_PRIMARY);
        timingEventRow(sb, "CallFailed",           info.getCallFailed(),           COLOR_ERROR);
        timingEventRow(sb, "Canceled",             info.getCanceled(),             COLOR_ERROR);

        sb.append("</table></div>");
        return sb.toString();
    }

    private static void eventRow(StringBuilder sb, String label, String value) {
        sb.append("<tr>")
                .append("<td style='width:30%;color:").append(COLOR_GRAY).append(";padding:2px 8px 2px 0;'>").append(label).append("</td>")
                .append("<td style='width:70%;word-break:break-all;'>").append(value).append("</td>")
                .append("</tr>");
    }

    private static void timingEventRow(StringBuilder sb, String label, long millis, String color) {
        String style = color != null ? "color:" + color + ";" : "";
        sb.append("<tr>")
                .append("<td style='width:30%;").append(style).append("padding:2px 8px 2px 0;'>").append(label).append("</td>")
                .append("<td style='width:70%;'>").append(formatMillis(millis)).append("</td>")
                .append("</tr>");
    }

    // ==================== 工具方法 ====================

    private static String safeStr(String s) { return s != null ? s : "-"; }

    private static boolean isNotEmpty(String s) { return s != null && !s.isEmpty(); }

    private static String formatMillis(long millis) {
        return millis <= 0 ? "-" : new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(millis));
    }

    private static String truncate(String content) {
        if (content == null) return "";
        if (content.length() <= MAX_DISPLAY_SIZE) return content;
        return content.substring(0, MAX_DISPLAY_SIZE)
                + "\n\n[Truncated: " + content.length() + " chars total, showing first " + (MAX_DISPLAY_SIZE / 1024) + "KB]";
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
